"""Publicación segura del feed: respaldo, comparación con la versión actual y reversión.

La actualización semanal no debe poder dejar la app sin estaciones porque alguien
borró una relación en OpenStreetMap. Por eso, antes de publicar:
  1. el feed nuevo tiene que pasar la validación,
  2. no puede perder más del 10 % de las estaciones ni una línea completa
     (salvo --forzar, después de revisarlo a mano),
  3. la versión anterior se guarda en salida/historial/ para poder revertir.
"""
from __future__ import annotations

import csv
import io
import shutil
import zipfile
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

ARCHIVOS_PUBLICADOS = ["metro-pty-gtfs.zip", "procedencia.json", "osm_metro.json"]
MAX_VERSIONES = 10


@dataclass
class Comparacion:
    estaciones_antes: int
    estaciones_despues: int
    rutas_perdidas: list[str] = field(default_factory=list)
    estaciones_nuevas: list[str] = field(default_factory=list)
    estaciones_quitadas: list[str] = field(default_factory=list)

    def bloqueante(self, umbral: float = 0.10) -> str | None:
        """Motivo para no publicar sin revisión manual, o None si es seguro."""
        if self.rutas_perdidas:
            return f"desaparecen líneas completas: {', '.join(self.rutas_perdidas)}"
        if self.estaciones_antes and \
                (self.estaciones_antes - self.estaciones_despues) / self.estaciones_antes > umbral:
            return (f"se pierden {self.estaciones_antes - self.estaciones_despues} de "
                    f"{self.estaciones_antes} estaciones (más del {umbral:.0%})")
        return None


def _filas(ruta_zip: Path, archivo: str) -> list[dict]:
    with zipfile.ZipFile(ruta_zip) as z:
        if archivo not in z.namelist():
            return []
        return list(csv.DictReader(io.TextIOWrapper(z.open(archivo), encoding="utf-8")))


def _estaciones(ruta_zip: Path) -> dict[str, str]:
    return {s["stop_id"]: s["stop_name"] for s in _filas(ruta_zip, "stops.txt") if s.get("location_type") == "1"}


def comparar(actual: Path, nuevo: Path) -> Comparacion:
    if not actual.exists():
        nuevas = _estaciones(nuevo)
        return Comparacion(0, len(nuevas), estaciones_nuevas=sorted(nuevas.values()))
    antes, despues = _estaciones(actual), _estaciones(nuevo)
    rutas_antes = {r["route_id"] for r in _filas(actual, "routes.txt")}
    rutas_despues = {r["route_id"] for r in _filas(nuevo, "routes.txt")}
    return Comparacion(
        estaciones_antes=len(antes),
        estaciones_despues=len(despues),
        rutas_perdidas=sorted(rutas_antes - rutas_despues),
        estaciones_nuevas=sorted(despues[k] for k in despues.keys() - antes.keys()),
        estaciones_quitadas=sorted(antes[k] for k in antes.keys() - despues.keys()),
    )


def respaldar(salida: Path, motivo: str = "", podar: bool = True) -> Path | None:
    """Copia la versión publicada a salida/historial/AAAAMMDD-HHMMSS[-motivo]/."""
    existentes = [salida / a for a in ARCHIVOS_PUBLICADOS if (salida / a).exists()]
    if not existentes:
        return None
    base = datetime.now().strftime("%Y%m%d-%H%M%S") + (f"-{motivo}" if motivo else "")
    destino, n = salida / "historial" / base, 1
    while destino.exists():  # dos respaldos en el mismo segundo
        n += 1
        destino = salida / "historial" / f"{base}-{n}"
    destino.mkdir(parents=True)
    for f in existentes:
        shutil.copy2(f, destino / f.name)
    if podar:
        _podar(salida / "historial")
    return destino


def _podar(historial: Path) -> None:
    versiones = sorted(p for p in historial.iterdir() if p.is_dir())
    for vieja in versiones[:-MAX_VERSIONES]:
        shutil.rmtree(vieja)


def versiones(salida: Path) -> list[Path]:
    h = salida / "historial"
    return sorted((p for p in h.iterdir() if p.is_dir()), reverse=True) if h.exists() else []


def instalar(origen: Path, salida: Path, otp_dir: Path | None) -> None:
    """Pone los archivos de `origen` como versión publicada y copia el zip para OTP."""
    for nombre in ARCHIVOS_PUBLICADOS:
        if (origen / nombre).exists():
            shutil.copy2(origen / nombre, salida / nombre)
    if otp_dir is not None and otp_dir.exists():
        shutil.copy2(salida / "metro-pty-gtfs.zip", otp_dir / "metro-pty-gtfs.zip")


def revertir(salida: Path, otp_dir: Path | None, version: str | None = None) -> Path:
    """Vuelve a una versión del historial (la más reciente si no se indica).

    La versión actual se respalda antes, así que revertir también se puede deshacer.
    """
    disponibles = versiones(salida)
    if not disponibles:
        raise FileNotFoundError("No hay versiones en salida/historial para revertir.")
    elegida = next((v for v in disponibles if v.name == version), None) if version else disponibles[0]
    if elegida is None:
        raise FileNotFoundError(f"No existe la versión {version}. Usa: python -m conecta_data historial")
    respaldar(salida, "antes-de-revertir", podar=False)  # podar después: podría borrar la elegida
    instalar(elegida, salida, otp_dir)
    _podar(salida / "historial")
    return elegida
