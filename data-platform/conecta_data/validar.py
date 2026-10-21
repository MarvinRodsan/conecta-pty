"""Validaciones de calidad antes de publicar un feed (sección 7.2 del documento)."""
from __future__ import annotations

import csv
import io
import zipfile
from dataclasses import dataclass, field

from .gtfs import haversine_m, hms_a_s

OBLIGATORIOS = ["agency.txt", "stops.txt", "routes.txt", "trips.txt", "stop_times.txt", "calendar.txt"]
# Panamá continental, con margen
LAT_MIN, LAT_MAX, LON_MIN, LON_MAX = 7.0, 10.0, -83.1, -77.1


@dataclass
class Reporte:
    errores: list[str] = field(default_factory=list)
    avisos: list[str] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.errores


def _leer(z: zipfile.ZipFile, nombre: str) -> list[dict]:
    if nombre not in z.namelist():
        return []
    return list(csv.DictReader(io.TextIOWrapper(z.open(nombre), encoding="utf-8")))


def validar_zip(ruta_zip: str) -> Reporte:
    r = Reporte()
    with zipfile.ZipFile(ruta_zip) as z:
        for req in OBLIGATORIOS:
            if req not in z.namelist():
                r.errores.append(f"Falta el archivo obligatorio {req}")
        if r.errores:
            return r
        stops = _leer(z, "stops.txt")
        routes = _leer(z, "routes.txt")
        trips = _leer(z, "trips.txt")
        stop_times = _leer(z, "stop_times.txt")
        calendar = _leer(z, "calendar.txt")
        frequencies = _leer(z, "frequencies.txt")

    ids_stop = {s["stop_id"] for s in stops}
    if len(ids_stop) != len(stops):
        r.errores.append("Hay stop_id duplicados")
    for s in stops:
        lat, lon = float(s["stop_lat"]), float(s["stop_lon"])
        if not (LAT_MIN <= lat <= LAT_MAX and LON_MIN <= lon <= LON_MAX):
            r.errores.append(f"{s['stop_id']} ({s['stop_name']}) fuera de Panamá: {lat},{lon}")
        if s.get("parent_station") and s["parent_station"] not in ids_stop:
            r.errores.append(f"{s['stop_id']} apunta a estación padre inexistente {s['parent_station']}")

    # Andenes de estaciones distintas a menos de 30 m: probable duplicado mal nombrado
    andenes = [s for s in stops if s.get("location_type", "0") == "0"]
    for i, a in enumerate(andenes):
        for b in andenes[i + 1:]:
            if a["parent_station"] != b["parent_station"]:
                d = haversine_m(float(a["stop_lat"]), float(a["stop_lon"]),
                                float(b["stop_lat"]), float(b["stop_lon"]))
                if d < 30:
                    r.avisos.append(f"'{a['stop_name']}' y '{b['stop_name']}' están a {d:.0f} m pero son estaciones distintas")

    ids_route = {x["route_id"] for x in routes}
    ids_serv = {c["service_id"] for c in calendar}
    ids_trip = set()
    for t in trips:
        ids_trip.add(t["trip_id"])
        if t["route_id"] not in ids_route:
            r.errores.append(f"Viaje {t['trip_id']} con ruta inexistente {t['route_id']}")
        if t["service_id"] not in ids_serv:
            r.errores.append(f"Viaje {t['trip_id']} con servicio inexistente {t['service_id']}")

    por_viaje: dict[str, list[dict]] = {}
    for st in stop_times:
        if st["stop_id"] not in ids_stop:
            r.errores.append(f"stop_times referencia parada inexistente {st['stop_id']}")
        if st["trip_id"] not in ids_trip:
            r.errores.append(f"stop_times referencia viaje inexistente {st['trip_id']}")
        por_viaje.setdefault(st["trip_id"], []).append(st)
    for trip_id, filas in por_viaje.items():
        filas.sort(key=lambda x: int(x["stop_sequence"]))
        anterior = -1
        for f in filas:
            ll, sa = hms_a_s(f["arrival_time"]), hms_a_s(f["departure_time"])
            if ll < anterior or sa < ll:
                r.errores.append(f"Tiempos no crecientes en {trip_id}, secuencia {f['stop_sequence']}")
                break
            anterior = sa
    sin_horario = ids_trip - set(por_viaje)
    if sin_horario:
        r.errores.append(f"{len(sin_horario)} viajes sin stop_times")
    for f in frequencies:
        if f["trip_id"] not in ids_trip:
            r.errores.append(f"frequencies referencia viaje inexistente {f['trip_id']}")
        if int(f["headway_secs"]) <= 0:
            r.errores.append(f"headway inválido en {f['trip_id']}")
    if not frequencies:
        r.avisos.append("Sin frequencies.txt: OTP solo verá un viaje por patrón")
    return r
