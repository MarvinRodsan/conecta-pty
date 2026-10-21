"""CLI de datos.

    python -m conecta_data descargar   # baja las líneas del Metro desde OSM
    python -m conecta_data generar     # crea salida/metro-pty-gtfs.zip + procedencia.json
    python -m conecta_data validar     # revisa el zip antes de publicarlo
    python -m conecta_data todo        # las tres cosas en orden
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

from . import osm
from .gtfs import construir_feed, escribir_procedencia, escribir_zip
from .validar import validar_zip

RAIZ = Path(__file__).resolve().parent.parent
CONFIG = RAIZ / "config" / "supuestos_metro.json"
SALIDA = RAIZ / "salida"
CRUDO = SALIDA / "osm_metro.json"
ZIP = SALIDA / "metro-pty-gtfs.zip"
# OTP lee todo *.zip con "gtfs" en el nombre dentro de su carpeta de datos
OTP_DIR = RAIZ.parent / "infrastructure" / "otp" / "datos"


def _supuestos() -> dict:
    return json.loads(CONFIG.read_text(encoding="utf-8"))


def cmd_descargar(_: argparse.Namespace) -> int:
    SALIDA.mkdir(exist_ok=True)
    print("Consultando Overpass (OpenStreetMap)...")
    osm.descargar(_supuestos()["zona_busqueda_bbox"], str(CRUDO))
    print(f"Guardado en {CRUDO}")
    return 0


def cmd_generar(a: argparse.Namespace) -> int:
    origen = Path(a.entrada) if a.entrada else CRUDO
    if not origen.exists():
        print(f"No existe {origen}. Corre primero: python -m conecta_data descargar")
        return 1
    patrones, avisos = osm.leer_patrones(json.loads(origen.read_text(encoding="utf-8")))
    for av in avisos:
        print("AVISO:", av)
    if not patrones:
        print("ERROR: no se encontraron líneas de metro en los datos.")
        return 1
    for p in patrones:
        print(f"  Línea {p.ref:>3} | {len(p.paradas):>2} paradas | {p.paradas[0].nombre} -> {p.paradas[-1].nombre}")
    archivos, proc = construir_feed(patrones, _supuestos())
    SALIDA.mkdir(exist_ok=True)
    escribir_zip(archivos, str(ZIP))
    escribir_procedencia(proc, str(SALIDA / "procedencia.json"))
    print(f"GTFS: {ZIP}")
    if OTP_DIR.exists():
        shutil.copy(ZIP, OTP_DIR / ZIP.name)
        print(f"Copiado a {OTP_DIR} para OpenTripPlanner")
    return 0


def cmd_validar(a: argparse.Namespace) -> int:
    ruta = a.zip or str(ZIP)
    rep = validar_zip(ruta)
    for e in rep.errores:
        print("ERROR:", e)
    for av in rep.avisos:
        print("AVISO:", av)
    print("Feed válido." if rep.ok else f"Feed con {len(rep.errores)} errores. No publicar.")
    return 0 if rep.ok else 2


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(prog="conecta_data")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("descargar").set_defaults(f=cmd_descargar)
    g = sub.add_parser("generar"); g.add_argument("--entrada"); g.set_defaults(f=cmd_generar)
    v = sub.add_parser("validar"); v.add_argument("--zip"); v.set_defaults(f=cmd_validar)
    t = sub.add_parser("todo"); t.set_defaults(f=None)
    a = ap.parse_args(argv)
    if a.cmd == "todo":
        a.entrada, a.zip = None, None
        for paso in (cmd_descargar, cmd_generar, cmd_validar):
            if (rc := paso(a)) != 0:
                return rc
        return 0
    return a.f(a)


if __name__ == "__main__":
    sys.exit(main())
