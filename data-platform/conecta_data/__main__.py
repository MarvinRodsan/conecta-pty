"""CLI de datos.

    python -m conecta_data descargar   # baja las líneas del Metro desde OSM
    python -m conecta_data generar     # crea salida/metro-pty-gtfs.zip + procedencia.json
    python -m conecta_data validar     # revisa el zip antes de publicarlo
    python -m conecta_data todo        # las tres cosas en orden
    python -m conecta_data actualizar  # descarga, valida, compara y publica con respaldo (semanal)
    python -m conecta_data historial   # versiones guardadas
    python -m conecta_data revertir    # vuelve a la versión anterior
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
import tempfile
from pathlib import Path

from . import osm, publicar
from .gtfs import construir_feed, escribir_procedencia, escribir_zip
from .validar import validar_zip

RAIZ = Path(__file__).resolve().parent.parent
CONFIG = RAIZ / "config" / "supuestos_metro.json"
FERIADOS = RAIZ / "config" / "feriados_panama.json"
ACCESIBILIDAD = RAIZ / "config" / "accesibilidad_estaciones.json"
SALIDA = RAIZ / "salida"
CRUDO = SALIDA / "osm_metro.json"
ZIP = SALIDA / "metro-pty-gtfs.zip"
# OTP lee todo *.zip con "gtfs" en el nombre dentro de su carpeta de datos
OTP_DIR = RAIZ.parent / "infrastructure" / "otp" / "datos"

# Códigos de salida (útiles para tareas programadas y CI)
OK, ERROR_DESCARGA, FEED_INVALIDO, CAMBIO_SOSPECHOSO = 0, 1, 2, 3


def _json(ruta: Path) -> dict | None:
    return json.loads(ruta.read_text(encoding="utf-8")) if ruta.exists() else None


def _supuestos() -> dict:
    return json.loads(CONFIG.read_text(encoding="utf-8"))


def _descargar(destino: Path) -> int:
    print("Consultando Overpass (OpenStreetMap)...")
    try:
        servidor = osm.descargar(_supuestos()["zona_busqueda_bbox"], str(destino))
    except ConnectionError as e:
        print(f"ERROR: {e}")
        return ERROR_DESCARGA
    print(f"Guardado en {destino} (fuente: {servidor})")
    return OK


def _generar(origen: Path, carpeta: Path) -> int:
    """Lee el JSON de Overpass y escribe metro-pty-gtfs.zip + procedencia.json en `carpeta`."""
    if not origen.exists():
        print(f"No existe {origen}. Corre primero: python -m conecta_data descargar")
        return ERROR_DESCARGA
    patrones, avisos = osm.leer_patrones(json.loads(origen.read_text(encoding="utf-8")))
    for av in avisos:
        print("AVISO:", av)
    if not patrones:
        print("ERROR: no se encontraron líneas de metro en los datos.")
        return FEED_INVALIDO
    for p in patrones:
        print(f"  {p.nombre_ruta:<18} | {len(p.paradas):>2} paradas | {p.paradas[0].nombre} -> {p.paradas[-1].nombre}")
    print("Estaciones de transbordo:")
    for est, rutas in osm.estaciones_de_transbordo(patrones).items():
        print(f"  {est}: {', '.join(sorted(rutas))}")
    archivos, proc = construir_feed(patrones, _supuestos(),
                                    feriados=_json(FERIADOS), accesibilidad=_json(ACCESIBILIDAD))
    if "calendar_dates.txt" in archivos:
        print(f"Feriados modelados: {len(proc['feriados']['fechas'])} (estado: {proc['feriados']['estado']})")
    carpeta.mkdir(parents=True, exist_ok=True)
    escribir_zip(archivos, str(carpeta / ZIP.name))
    escribir_procedencia(proc, str(carpeta / "procedencia.json"))
    print(f"GTFS: {carpeta / ZIP.name}")
    return OK


def _reporte_validacion(ruta: Path) -> bool:
    rep = validar_zip(str(ruta))
    for e in rep.errores:
        print("ERROR:", e)
    for av in rep.avisos:
        print("AVISO:", av)
    print("Feed válido." if rep.ok else f"Feed con {len(rep.errores)} errores. No publicar.")
    return rep.ok


def cmd_descargar(_: argparse.Namespace) -> int:
    SALIDA.mkdir(exist_ok=True)
    return _descargar(CRUDO)


def cmd_generar(a: argparse.Namespace) -> int:
    rc = _generar(Path(a.entrada) if a.entrada else CRUDO, SALIDA)
    if rc == OK and OTP_DIR.exists():
        shutil.copy(ZIP, OTP_DIR / ZIP.name)
        print(f"Copiado a {OTP_DIR} para OpenTripPlanner")
    return rc


def cmd_validar(a: argparse.Namespace) -> int:
    return OK if _reporte_validacion(Path(a.zip) if a.zip else ZIP) else FEED_INVALIDO


def cmd_actualizar(a: argparse.Namespace) -> int:
    """Actualización desatendida: nunca publica un feed roto ni uno que perdió media red."""
    SALIDA.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="conecta-") as tmp:
        nuevo = Path(tmp)
        crudo = nuevo / "osm_metro.json"
        if a.entrada:
            shutil.copy(a.entrada, crudo)
        elif (rc := _descargar(crudo)) != OK:
            return rc
        if (rc := _generar(crudo, nuevo)) != OK:
            return rc
        if not _reporte_validacion(nuevo / ZIP.name):
            return FEED_INVALIDO

        cambios = publicar.comparar(ZIP, nuevo / ZIP.name)
        print(f"Estaciones: {cambios.estaciones_antes} -> {cambios.estaciones_despues}")
        for e in cambios.estaciones_nuevas:
            print(f"  + {e}")
        for e in cambios.estaciones_quitadas:
            print(f"  - {e}")
        motivo = cambios.bloqueante()
        if motivo and not a.forzar:
            print(f"NO SE PUBLICÓ: {motivo}. Revisa OpenStreetMap; si el cambio es real, repite con --forzar.")
            return CAMBIO_SOSPECHOSO

        respaldo = publicar.respaldar(SALIDA)
        publicar.instalar(nuevo, SALIDA, OTP_DIR)
    if respaldo:
        print(f"Versión anterior guardada en {respaldo}")
    print("Publicado. Para que OpenTripPlanner lo use, reconstruye el grafo y reinicia:")
    print("  cd infrastructure; docker compose run --rm otp-construir; docker compose restart otp")
    print("El backend importa las estaciones nuevas al reiniciarse (detecta el cambio por SHA-256).")
    return OK


def cmd_historial(_: argparse.Namespace) -> int:
    vs = publicar.versiones(SALIDA)
    if not vs:
        print("Sin versiones guardadas todavía.")
    for v in vs:
        proc = _json(v / "procedencia.json") or {}
        print(f"  {v.name}   (generado {proc.get('generado', '?')})")
    return OK


def cmd_revertir(a: argparse.Namespace) -> int:
    try:
        v = publicar.revertir(SALIDA, OTP_DIR, a.version)
    except FileNotFoundError as e:
        print(f"ERROR: {e}")
        return ERROR_DESCARGA
    print(f"Revertido a {v.name}. Reconstruye el grafo de OTP y reinicia el backend para aplicarlo.")
    return OK


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(prog="conecta_data")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("descargar").set_defaults(f=cmd_descargar)
    g = sub.add_parser("generar"); g.add_argument("--entrada"); g.set_defaults(f=cmd_generar)
    v = sub.add_parser("validar"); v.add_argument("--zip"); v.set_defaults(f=cmd_validar)
    t = sub.add_parser("todo"); t.set_defaults(f=None)
    u = sub.add_parser("actualizar", help="descargar + generar + validar + comparar + publicar con respaldo")
    u.add_argument("--entrada", help="usar un JSON de Overpass ya descargado")
    u.add_argument("--forzar", action="store_true", help="publicar aunque se pierdan muchas estaciones")
    u.set_defaults(f=cmd_actualizar)
    sub.add_parser("historial").set_defaults(f=cmd_historial)
    r = sub.add_parser("revertir"); r.add_argument("--version"); r.set_defaults(f=cmd_revertir)
    a = ap.parse_args(argv)
    if a.cmd == "todo":
        a.entrada, a.zip = None, None
        for paso in (cmd_descargar, cmd_generar, cmd_validar):
            if (rc := paso(a)) != OK:
                return rc
        return OK
    return a.f(a)


if __name__ == "__main__":
    sys.exit(main())
