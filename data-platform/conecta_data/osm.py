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

# Servidores públicos de Overpass. Si uno falla (caído, certificado vencido...), se prueba el siguiente.
SERVIDORES_OVERPASS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
]
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
    ref: str            # número público de la línea: "1", "2"
    nombre: str         # nombre de la relación en OSM
    color: str | None
    paradas: list[Parada] = field(default_factory=list)
    ruta_id: str = ""   # GTFS route_id: "L1", "L2", "L2R" (ramal)
    nombre_ruta: str = ""


_PREFIJO_LINEA = re.compile(r"(?i)^\s*(l[ií]nea|l)\s*")
_LINEA_EN_NOMBRE = re.compile(r"(?i)\bl(?:[ií]nea)?\s*(\d+)\b")


def normalizar_ref(ref: str | None, nombre: str = "") -> str:
    """'L1' / 'Línea 1' / '1' -> '1'. Si no hay ref, intenta leerla del nombre."""
    r = _PREFIJO_LINEA.sub("", (ref or "").strip())
    if r:
        return r
    m = _LINEA_EN_NOMBRE.search(nombre)
    return m.group(1) if m else ""


def asignar_rutas(patrones: list["Patron"], avisos: list[str]) -> None:
    """Pone ruta_id y nombre a cada patrón.

    Un patrón sin número de línea que comparte estación con una línea conocida se trata
    como ramal de esa línea (caso Corredor Sur -> Aeropuerto de la Línea 2).
    """
    estaciones_por_ref: dict[str, set[str]] = {}
    for p in patrones:
        if p.ref:
            estaciones_por_ref.setdefault(p.ref, set()).update(clave_estacion(x.nombre) for x in p.paradas)
    colores = {p.ref: p.color for p in patrones if p.ref and p.color}
    for p in patrones:
        if p.ref:
            p.ruta_id, p.nombre_ruta = f"L{p.ref}", f"Línea {p.ref}"
            continue
        mias = {clave_estacion(x.nombre) for x in p.paradas}
        madre = next((r for r, est in sorted(estaciones_por_ref.items()) if mias & est), None)
        if madre:
            p.ref, p.ruta_id = madre, f"L{madre}R"
            p.nombre_ruta = f"Línea {madre} – Ramal"
            p.color = p.color or colores.get(madre)
            avisos.append(f"Relación {p.osm_id} ({p.nombre}) sin número de línea: tratada como ramal de la Línea {madre}.")
        else:
            p.ref, p.ruta_id, p.nombre_ruta = "?", f"R{p.osm_id}", p.nombre
            avisos.append(f"Relación {p.osm_id} ({p.nombre}) sin número de línea y sin conexión conocida. Revísala en OSM.")


def consulta_overpass(bbox: list[float]) -> str:
    s, w, n, e = bbox
    return (
        "[out:json][timeout:90];"
        f'relation["route"~"^(subway|light_rail)$"]({s},{w},{n},{e})->.r;'
        ".r out body;"
        "node(r.r)->.n;"
        ".n out body;"
    )


def _contexto_ssl():
    """Usa los certificados del sistema operativo si `truststore` está instalado
    (útil en Windows y en redes con proxy corporativo). Nunca desactiva la verificación."""
    try:
        import ssl
        import truststore  # pip install truststore
        return truststore.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    except ImportError:
        return None


def descargar(bbox: list[float], destino: str, timeout: int = 120,
              servidores: list[str] | None = None) -> str:
    """Descarga las líneas y devuelve el servidor que respondió."""
    datos = urllib.parse.urlencode({"data": consulta_overpass(bbox)}).encode()
    ctx = _contexto_ssl()
    fallos: list[str] = []
    for url in servidores or SERVIDORES_OVERPASS:
        print(f"  Probando {url} ...")
        req = urllib.request.Request(url, data=datos, headers={"User-Agent": "ConectaPTY-data/0.1"})
        try:
            with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
                contenido = resp.read()
            json.loads(contenido)  # falla si el servidor devolvió un error en HTML
        except Exception as e:  # noqa: BLE001 — se reporta y se prueba el siguiente
            motivo = getattr(e, "reason", e)
            print(f"    falló: {motivo}")
            fallos.append(f"{url}: {motivo}")
            continue
        with open(destino, "wb") as f:
            f.write(contenido)
        return url
    raise ConnectionError(
        "Ningún servidor de Overpass respondió.\n  " + "\n  ".join(fallos) +
        "\nSi todos dicen 'certificate has expired': revisa la fecha y hora de tu PC, "
        "o prueba `pip install truststore` y vuelve a correr el comando."
    )


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
        nombre_rel = tags.get("name", f"Relación {rel['id']}")
        patron = Patron(
            osm_id=rel["id"],
            ref=normalizar_ref(tags.get("ref"), nombre_rel),
            nombre=nombre_rel,
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
    asignar_rutas(patrones, avisos)
    patrones.sort(key=lambda p: (p.ruta_id, p.osm_id))
    return patrones, avisos


def estaciones_de_transbordo(patrones: list[Patron]) -> dict[str, set[str]]:
    """{nombre de estación: {rutas que la usan}} solo para las que tienen 2 rutas o más."""
    rutas: dict[str, set[str]] = {}
    nombres: dict[str, str] = {}
    for p in patrones:
        for x in p.paradas:
            k = clave_estacion(x.nombre)
            rutas.setdefault(k, set()).add(p.nombre_ruta)
            nombres.setdefault(k, x.nombre)
    return {nombres[k]: r for k, r in sorted(rutas.items()) if len(r) > 1}


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
