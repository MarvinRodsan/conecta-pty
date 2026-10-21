"""Descarga y lectura de las líneas del Metro desde OpenStreetMap (Overpass API).

Se usan las relaciones `route=subway` (esquema PTv2): cada relación es un sentido
de una línea y sus miembros con rol `stop*` vienen en orden de recorrido.
Los datos de OSM están bajo licencia ODbL: hay que citar
"© colaboradores de OpenStreetMap" donde se muestren.
"""
from __future__ import annotations

import json
import re
import unicodedata
import urllib.parse
import urllib.request
from dataclasses import dataclass, field

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
ROLES_PARADA = {"stop", "stop_entry_only", "stop_exit_only"}


@dataclass(frozen=True)
class Parada:
    osm_id: int
    nombre: str
    lat: float
    lon: float


@dataclass
class Patron:
    """Un sentido de una línea: secuencia ordenada de paradas."""
    osm_id: int
    ref: str
    nombre: str
    color: str | None
    paradas: list[Parada] = field(default_factory=list)


def consulta_overpass(bbox: list[float]) -> str:
    s, w, n, e = bbox
    return (
        "[out:json][timeout:90];"
        f'relation["route"~"^(subway|light_rail)$"]({s},{w},{n},{e})->.r;'
        ".r out body;"
        "node(r.r)->.n;"
        ".n out body;"
    )


def descargar(bbox: list[float], destino: str, timeout: int = 120) -> None:
    datos = urllib.parse.urlencode({"data": consulta_overpass(bbox)}).encode()
    req = urllib.request.Request(
        OVERPASS_URL, data=datos, headers={"User-Agent": "ConectaPTY-data/0.1"}
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        contenido = resp.read()
    json.loads(contenido)  # falla aquí si Overpass devolvió un error en HTML
    with open(destino, "wb") as f:
        f.write(contenido)


def leer_patrones(overpass_json: dict) -> tuple[list[Patron], list[str]]:
    """Convierte la respuesta de Overpass en patrones. Devuelve (patrones, avisos)."""
    avisos: list[str] = []
    nodos = {
        el["id"]: el
        for el in overpass_json.get("elements", [])
        if el.get("type") == "node" and "lat" in el
    }
    patrones: list[Patron] = []
    for rel in overpass_json.get("elements", []):
        if rel.get("type") != "relation":
            continue
        tags = rel.get("tags", {})
        ref = (tags.get("ref") or "").strip() or "?"
        patron = Patron(
            osm_id=rel["id"],
            ref=ref,
            nombre=tags.get("name", f"Línea {ref}"),
            color=_color(tags.get("colour")),
        )
        miembros = [m for m in rel.get("members", []) if m.get("type") == "node"]
        con_rol = [m for m in miembros if m.get("role") in ROLES_PARADA]
        if not con_rol:
            # Algunas relaciones antiguas solo marcan andenes como nodos.
            con_rol = [m for m in miembros if (m.get("role") or "").startswith("platform")]
            if con_rol:
                avisos.append(f"Relación {rel['id']} sin roles 'stop'; se usaron andenes.")
        for m in con_rol:
            nodo = nodos.get(m["ref"])
            if nodo is None:
                avisos.append(f"Relación {rel['id']}: nodo {m['ref']} sin coordenadas, omitido.")
                continue
            nombre = (nodo.get("tags", {}).get("name") or "").strip()
            if not nombre:
                avisos.append(f"Nodo {nodo['id']} sin nombre, omitido.")
                continue
            parada = Parada(nodo["id"], nombre, float(nodo["lat"]), float(nodo["lon"]))
            # Evita duplicados consecutivos (stop_position + plataforma con el mismo nombre)
            if patron.paradas and clave_estacion(patron.paradas[-1].nombre) == clave_estacion(nombre):
                continue
            patron.paradas.append(parada)
        if len(patron.paradas) < 2:
            avisos.append(f"Relación {rel['id']} ({patron.nombre}) tiene menos de 2 paradas; descartada.")
            continue
        patrones.append(patron)
    patrones.sort(key=lambda p: (p.ref, p.osm_id))
    return patrones, avisos


_RUIDO = re.compile(r"\b(estacion|metro|linea\s*\d+[a-z]?|l\d+[a-z]?)\b")


def clave_estacion(nombre: str) -> str:
    """Normaliza un nombre para agrupar andenes de la misma estación.

    'Estación San Miguelito L2' y 'San Miguelito' -> 'san-miguelito'.
    """
    s = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode().lower()
    s = _RUIDO.sub(" ", s)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s or "sin-nombre"


def _color(valor: str | None) -> str | None:
    if not valor:
        return None
    v = valor.strip().lstrip("#")
    return v.upper() if re.fullmatch(r"[0-9a-fA-F]{6}", v) else None
