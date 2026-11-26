"""Genera un feed GTFS a partir de los patrones de OSM y los supuestos del piloto.

Principio del proyecto: nada se presenta como confirmado si no lo es. Por eso el
generador escribe, además del GTFS, `procedencia.json` con el estado de cada
tipo de dato (verificado / estimado / desconocido) y su fuente.
"""
from __future__ import annotations

import csv
import io
import json
import math
import zipfile
from collections import defaultdict
from datetime import date, timedelta

from .feriados import calendar_dates, feriados_entre
from .osm import Parada, Patron, clave_estacion

# GTFS wheelchair_boarding: 0 = desconocido, 1 = accesible, 2 = no accesible
_SILLA_RUEDAS = {"si": "1", "no": "2"}


def accesibilidad_verificada(config: dict | None) -> dict[str, dict]:
    """Solo cuenta lo verificado en campo: con valor si/no, fecha y fuente."""
    if not config:
        return {}
    return {
        clave: dato
        for clave, dato in config.get("estaciones", {}).items()
        if dato.get("silla_ruedas") in _SILLA_RUEDAS and dato.get("verificado_el") and dato.get("fuente")
    }


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def hms_a_s(hms: str) -> int:
    h, m, s = (int(x) for x in hms.split(":"))
    return h * 3600 + m * 60 + s


def s_a_hms(seg: int) -> str:
    return f"{seg // 3600:02d}:{seg % 3600 // 60:02d}:{seg % 60:02d}"


def construir_feed(
    patrones: list[Patron], supuestos: dict, hoy: date | None = None,
    feriados: dict | None = None, accesibilidad: dict | None = None,
) -> tuple[dict[str, list[dict]], dict]:
    """Devuelve ({archivo: filas}, procedencia).

    feriados:      contenido de config/feriados_panama.json (None = no se modelan)
    accesibilidad: contenido de config/accesibilidad_estaciones.json (None = todo desconocido)
    """
    hoy = hoy or date.today()
    verificadas = accesibilidad_verificada(accesibilidad)
    fin = hoy + timedelta(days=int(supuestos.get("vigencia_dias", 365)))
    ag = supuestos["agencia"]
    archivos: dict[str, list[dict]] = {}

    archivos["agency.txt"] = [dict(ag)]
    archivos["feed_info.txt"] = [{
        "feed_publisher_name": "Conecta PTY (datos no oficiales)",
        "feed_publisher_url": "https://github.com/conecta-pty",
        "feed_lang": "es",
        "feed_start_date": hoy.strftime("%Y%m%d"),
        "feed_end_date": fin.strftime("%Y%m%d"),
        "feed_version": hoy.isoformat(),
    }]

    # --- Paradas: un andén por nodo OSM, agrupados en estaciones padre por nombre
    andenes: dict[int, Parada] = {}
    for p in patrones:
        for par in p.paradas:
            andenes.setdefault(par.osm_id, par)
    por_estacion: dict[str, list[Parada]] = defaultdict(list)
    for par in andenes.values():
        por_estacion[clave_estacion(par.nombre)].append(par)

    stops: list[dict] = []
    for clave, hijos in sorted(por_estacion.items()):
        nombre = min((h.nombre for h in hijos), key=len)  # el más limpio suele ser el más corto
        # 0 = desconocido salvo verificación en campo: no prometemos accesibilidad
        silla = _SILLA_RUEDAS.get(verificadas.get(clave, {}).get("silla_ruedas"), "0")
        stops.append({
            "stop_id": f"EST_{clave}",
            "stop_name": nombre,
            "stop_lat": f"{sum(h.lat for h in hijos) / len(hijos):.6f}",
            "stop_lon": f"{sum(h.lon for h in hijos) / len(hijos):.6f}",
            "location_type": "1",
            "parent_station": "",
            "wheelchair_boarding": silla,
        })
        for h in sorted(hijos, key=lambda x: x.osm_id):
            stops.append({
                "stop_id": f"N{h.osm_id}",
                "stop_name": nombre,
                "stop_lat": f"{h.lat:.6f}",
                "stop_lon": f"{h.lon:.6f}",
                "location_type": "0",
                "parent_station": f"EST_{clave}",
                "wheelchair_boarding": silla,
            })
    archivos["stops.txt"] = stops

    # --- Transbordos dentro de una misma estación (p. ej. San Miguelito L1 <-> L2)
    t_min = str(int(supuestos.get("tiempo_minimo_transbordo_s", 180)))
    transfers = []
    for hijos in por_estacion.values():
        for a in hijos:
            for b in hijos:
                if a.osm_id != b.osm_id:
                    transfers.append({
                        "from_stop_id": f"N{a.osm_id}", "to_stop_id": f"N{b.osm_id}",
                        "transfer_type": "2", "min_transfer_time": t_min,
                    })
    archivos["transfers.txt"] = transfers

    # --- Rutas (una por ref) y servicios
    rutas: dict[str, dict] = {}
    for p in patrones:
        if p.ruta_id not in rutas:
            rutas[p.ruta_id] = {
                "route_id": p.ruta_id,
                "agency_id": ag["agency_id"],
                "route_short_name": p.ref,
                "route_long_name": p.nombre_ruta,
                "route_type": "1",  # metro
                "route_color": p.color or "",
                "route_text_color": "FFFFFF" if p.color else "",
            }
    archivos["routes.txt"] = list(rutas.values())

    servicios = supuestos["servicios"]
    dias = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
    archivos["calendar.txt"] = [
        {"service_id": sid, **{d: "1" if d in s["dias"] else "0" for d in dias},
         "start_date": hoy.strftime("%Y%m%d"), "end_date": fin.strftime("%Y%m%d")}
        for sid, s in servicios.items()
    ]
    lista_feriados = feriados_entre(feriados, hoy, fin) if feriados else []
    if lista_feriados:
        archivos["calendar_dates.txt"] = calendar_dates(
            lista_feriados, servicios, feriados.get("servicio_en_feriado", "DOM"))

    # --- Viajes: uno por cada salida estimada (cada N minutos según el servicio).
    # No usamos frequencies.txt: OpenTripPlanner 2.7 falla ("distanceMeters") con viajes por
    # frecuencia (corregido en OTP 2.8, PR #6580) y además cobra siempre la espera completa
    # del intervalo (issue #8080). Viajes explícitos funcionan bien en cualquier versión.
    vel_ms = float(supuestos["velocidad_comercial_kmh"]) / 3.6
    perm = int(supuestos["permanencia_en_estacion_s"])
    trips, stop_times = [], []
    for p in patrones:
        # Desfase (llegada, salida) de cada parada respecto a la salida de la primera
        desfases: list[tuple[int, int]] = []
        t = 0
        for i, par in enumerate(p.paradas):
            if i > 0:
                prev = p.paradas[i - 1]
                t += max(60, round(haversine_m(prev.lat, prev.lon, par.lat, par.lon) / vel_ms))
            salida = t + (perm if 0 < i < len(p.paradas) - 1 else 0)
            desfases.append((t, salida))
            t = salida
        for sid, s in servicios.items():
            intervalo = int(supuestos["intervalo_min_por_servicio"][sid]) * 60
            for inicio in range(hms_a_s(s["inicio"]), hms_a_s(s["fin"]) + 1, intervalo):
                trip_id = f"{sid}_{p.osm_id}_{s_a_hms(inicio)[:5].replace(':', '')}"
                trips.append({
                    "route_id": p.ruta_id, "service_id": sid, "trip_id": trip_id,
                    "trip_headsign": p.paradas[-1].nombre, "direction_id": "",
                })
                for i, (par, (ll, sa)) in enumerate(zip(p.paradas, desfases)):
                    stop_times.append({
                        "trip_id": trip_id, "arrival_time": s_a_hms(inicio + ll),
                        "departure_time": s_a_hms(inicio + sa), "stop_id": f"N{par.osm_id}",
                        "stop_sequence": str(i + 1), "timepoint": "0",  # 0 = aproximado
                    })
    archivos["trips.txt"] = trips
    archivos["stop_times.txt"] = stop_times

    procedencia = {
        "generado": hoy.isoformat(),
        "estaciones": {"estado": "verificado_en_osm", "fuente": "OpenStreetMap (ODbL) — © colaboradores de OpenStreetMap",
                       "nota": "Ubicación y orden tomados de OSM; falta verificación en campo."},
        "horario_operacion": {"estado": "por_confirmar", "fuente": supuestos.get("fuente_horarios", "")},
        "frecuencias": {"estado": supuestos.get("estado_intervalo", "estimado"),
                        "valores_min": supuestos["intervalo_min_por_servicio"]},
        "tiempos_entre_estaciones": {"estado": supuestos.get("estado_tiempos_viaje", "estimado"),
                                     "metodo": f"distancia / {supuestos['velocidad_comercial_kmh']} km/h + {perm}s de parada"},
        "tarifas": {"estado": "desconocido", "nota": "No se publican tarifas hasta tener fuente oficial."},
        "accesibilidad": (
            {"estado": "verificado_parcial", "estaciones_verificadas": sorted(verificadas),
             "nota": "Las demás estaciones siguen como desconocido."}
            if verificadas else {"estado": "desconocido"}),
        "feriados": (
            {"estado": feriados.get("estado", "por_confirmar"), "fuente": feriados.get("fuente", ""),
             "servicio": feriados.get("servicio_en_feriado", "DOM"),
             "fechas": [{"fecha": f.fecha.isoformat(), "nombre": f.nombre} for f in lista_feriados]}
            if lista_feriados else {"estado": "no_modelado", "nota": "Sin config/feriados_panama.json."}),
        "patrones": [{"osm_relation": p.osm_id, "ref": p.ref, "ruta": p.ruta_id, "nombre": p.nombre,
                      "paradas": len(p.paradas)} for p in patrones],
    }
    return archivos, procedencia


def escribir_zip(archivos: dict[str, list[dict]], ruta_zip: str) -> None:
    with zipfile.ZipFile(ruta_zip, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for nombre, filas in archivos.items():
            if not filas:
                continue
            buf = io.StringIO()
            w = csv.DictWriter(buf, fieldnames=list(filas[0].keys()), lineterminator="\n")
            w.writeheader()
            w.writerows(filas)
            z.writestr(nombre, buf.getvalue())


def escribir_procedencia(procedencia: dict, ruta: str) -> None:
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(procedencia, f, ensure_ascii=False, indent=2)
