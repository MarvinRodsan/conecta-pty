import json
import zipfile
from datetime import date
from pathlib import Path

from conecta_data import osm
from conecta_data.gtfs import construir_feed, escribir_zip, hms_a_s
from conecta_data.validar import validar_zip

AQUI = Path(__file__).parent
SUPUESTOS = json.loads((AQUI.parent / "config" / "supuestos_metro.json").read_text(encoding="utf-8"))


def _patrones():
    datos = json.loads((AQUI / "fixtures" / "overpass_muestra.json").read_text(encoding="utf-8"))
    return osm.leer_patrones(datos)


def test_clave_estacion_agrupa_variantes():
    assert osm.clave_estacion("Estación San Miguelito L2") == "san-miguelito"
    assert osm.clave_estacion("San Miguelito") == "san-miguelito"
    assert osm.clave_estacion("Vía Argentina") == "via-argentina"
    assert osm.clave_estacion("12 de Octubre") == "12-de-octubre"


def test_lee_patrones_en_orden_y_descarta_rotos():
    patrones, avisos = _patrones()
    assert [p.osm_id for p in patrones] == [101, 102, 201, 401]
    assert [x.nombre for x in patrones[0].paradas] == ["Estación A", "Estación B", "San Miguelito"]
    assert patrones[0].color == "E30613" and patrones[1].color == "E30613"
    assert any("999" in a for a in avisos)          # nodo sin coordenadas
    assert any("301" in a for a in avisos)          # relación con 1 parada


def test_feed_agrupa_estaciones_y_crea_transbordo():
    patrones, _ = _patrones()
    archivos, proc = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    padres = [s for s in archivos["stops.txt"] if s["location_type"] == "1"]
    assert {p["stop_id"] for p in padres} == {
        "EST_a", "EST_b", "EST_san-miguelito", "EST_cincuentenario", "EST_c", "EST_d"}
    # San Miguelito tiene 3 andenes (L1 ida, L1 vuelta, L2) -> 6 transbordos dirigidos
    sm = [t for t in archivos["transfers.txt"] if t["from_stop_id"] in {"N3", "N4", "N7"}]
    assert len(sm) == 6
    assert proc["frecuencias"]["estado"] == "estimado"
    assert proc["tarifas"]["estado"] == "desconocido"


def test_tiempos_crecientes_y_frecuencias():
    patrones, _ = _patrones()
    archivos, _ = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    viaje = [st for st in archivos["stop_times.txt"] if st["trip_id"] == "LV_101_0500"]
    tiempos = [hms_a_s(st["arrival_time"]) for st in viaje]
    assert tiempos == sorted(tiempos) and tiempos[0] == hms_a_s("05:00:00")
    # Un viaje por salida: L-V 05:00-23:00 c/5 min = 217, sáb 05:00-22:00 c/7 = 146, dom 07:00-22:00 c/10 = 91
    ids = {t["trip_id"] for t in archivos["trips.txt"]}
    assert len(archivos["trips.txt"]) == 4 * (217 + 146 + 91)
    assert "LV_101_2300" in ids and "DOM_101_0700" in ids and "DOM_101_0650" not in ids
    assert "frequencies.txt" not in archivos  # OTP 2.7 falla con viajes por frecuencia
    siguiente = [st for st in archivos["stop_times.txt"] if st["trip_id"] == "LV_101_0505"]
    assert hms_a_s(siguiente[-1]["arrival_time"]) - hms_a_s(viaje[-1]["arrival_time"]) == 300


def test_zip_generado_pasa_validacion(tmp_path):
    patrones, _ = _patrones()
    archivos, _ = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    ruta = tmp_path / "feed.zip"
    escribir_zip(archivos, str(ruta))
    rep = validar_zip(str(ruta))
    assert rep.ok, rep.errores
    with zipfile.ZipFile(ruta) as z:
        assert "frequencies.txt" not in z.namelist()


def test_validador_detecta_errores(tmp_path):
    patrones, _ = _patrones()
    archivos, _ = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    archivos["stops.txt"][0]["stop_lat"] = "40.0"            # fuera de Panamá
    archivos["stop_times.txt"][2]["arrival_time"] = "04:00:00"  # tiempo hacia atrás
    del archivos["calendar.txt"][0]                           # servicio inexistente
    ruta = tmp_path / "malo.zip"
    escribir_zip(archivos, str(ruta))
    rep = validar_zip(str(ruta))
    texto = " ".join(rep.errores)
    assert "fuera de Panamá" in texto and "no crecientes" in texto and "servicio inexistente" in texto


def test_refs_normalizadas_y_ramal():
    patrones, avisos = _patrones()
    por_id = {p.osm_id: p for p in patrones}
    assert por_id[101].ref == "1" and por_id[102].ref == "1" and por_id[101].ruta_id == "L1"
    assert por_id[401].ref == "2" and por_id[401].ruta_id == "L2R"
    assert por_id[401].nombre_ruta == "Línea 2 – Ramal"
    assert any("401" in a and "ramal" in a for a in avisos)
    assert osm.normalizar_ref("", "Metro de Panamá Línea 2: San Miguelito → Nuevo Tocumen") == "2"


def test_estaciones_de_transbordo():
    patrones, _ = _patrones()
    t = osm.estaciones_de_transbordo(patrones)
    assert t["San Miguelito"] == {"Línea 1", "Línea 2"}
    assert t["Cincuentenario"] == {"Línea 2", "Línea 2 – Ramal"}
