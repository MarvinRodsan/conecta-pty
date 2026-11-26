"""Feriados de Panamá -> calendar_dates.txt.

Sin calendar_dates el motor de rutas cree que un 1 de mayo que cae martes hay servicio
de lunes a viernes. Con esto, ese día se apaga el servicio normal y se enciende el de
domingo/feriados. Las fechas salen de config/feriados_panama.json (estado por confirmar).
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

DIAS = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]


@dataclass(frozen=True)
class Feriado:
    fecha: date
    nombre: str


def domingo_de_pascua(anio: int) -> date:
    """Algoritmo de Meeus/Jones/Butcher (calendario gregoriano)."""
    a = anio % 19
    b, c = divmod(anio, 100)
    d, e = divmod(b, 4)
    f = (b + 8) // 25
    g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30
    i, k = divmod(c, 4)
    l = (32 + 2 * e + 2 * i - h - k) % 7
    m = (a + 11 * h + 22 * l) // 451
    mes, dia = divmod(h + l - 7 * m + 114, 31)
    return date(anio, mes, dia + 1)


_MOVILES = {
    "martes_carnaval": lambda anio: domingo_de_pascua(anio) - timedelta(days=47),
    "viernes_santo": lambda anio: domingo_de_pascua(anio) - timedelta(days=2),
}


def feriados_entre(config: dict, desde: date, hasta: date) -> list[Feriado]:
    """Todos los feriados del rango [desde, hasta], ordenados y sin repetir fecha."""
    encontrados: dict[date, Feriado] = {}

    def agregar(f: date, nombre: str) -> None:
        if desde <= f <= hasta:
            encontrados.setdefault(f, Feriado(f, nombre))

    for anio in range(desde.year, hasta.year + 1):
        for fijo in config.get("fijos", []):
            mes, dia = (int(x) for x in fijo["dia"].split("-"))
            f = date(anio, mes, dia)
            agregar(f, fijo["nombre"])
            if config.get("trasladar_domingo_a_lunes") and f.weekday() == 6:
                agregar(f + timedelta(days=1), fijo["nombre"] + " (trasladado)")
        for movil in config.get("moviles", []):
            regla = _MOVILES.get(movil["regla"])
            if regla is None:
                raise ValueError(f"Regla de feriado desconocida: {movil['regla']}")
            agregar(regla(anio), movil["nombre"])
    for extra in config.get("adicionales", []):
        agregar(date.fromisoformat(extra["fecha"]), extra["nombre"])
    return sorted(encontrados.values(), key=lambda x: x.fecha)


def calendar_dates(feriados: list[Feriado], servicios: dict, servicio_feriado: str) -> list[dict]:
    """Filas de calendar_dates.txt.

    exception_type 2 = ese día NO corre el servicio; 1 = ese día SÍ corre (aunque no le toque).
    """
    if servicio_feriado not in servicios:
        raise ValueError(f"El servicio de feriados '{servicio_feriado}' no existe en supuestos_metro.json")
    filas = []
    for f in feriados:
        dia = DIAS[f.fecha.weekday()]
        for sid, s in servicios.items():
            corre_normalmente = dia in s["dias"]
            if sid == servicio_feriado and not corre_normalmente:
                filas.append({"service_id": sid, "date": f.fecha.strftime("%Y%m%d"), "exception_type": "1"})
            elif sid != servicio_feriado and corre_normalmente:
                filas.append({"service_id": sid, "date": f.fecha.strftime("%Y%m%d"), "exception_type": "2"})
    return filas
