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
    assert [p.osm_id for p in patrones] == [101, 102, 201]
    assert [x.nombre for x in patrones[0].paradas] == ["Estación A", "Estación B", "San Miguelito"]
    assert patrones[0].color == "E30613" and patrones[1].color == "E30613"
    assert any("999" in a for a in avisos)          # nodo sin coordenadas
    assert any("301" in a for a in avisos)          # relación con 1 parada


def test_feed_agrupa_estaciones_y_crea_transbordo():
    patrones, _ = _patrones()
    archivos, proc = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    padres = [s for s in archivos["stops.txt"] if s["location_type"] == "1"]
    assert {p["stop_id"] for p in padres} == {
        "EST_a", "EST_b", "EST_san-miguelito", "EST_cincuentenario", "EST_c"}
    # San Miguelito tiene 3 andenes (L1 ida, L1 vuelta, L2) -> 6 transbordos dirigidos
    sm = [t for t in archivos["transfers.txt"] if t["from_stop_id"] in {"N3", "N4", "N7"}]
    assert len(sm) == 6
    assert proc["frecuencias"]["estado"] == "estimado"
    assert proc["tarifas"]["estado"] == "desconocido"


def test_tiempos_crecientes_y_frecuencias():
    patrones, _ = _patrones()
    archivos, _ = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    viaje = [st for st in archivos["stop_times.txt"] if st["trip_id"] == "LV_101"]
    tiempos = [hms_a_s(st["arrival_time"]) for st in viaje]
    assert tiempos == sorted(tiempos) and tiempos[0] == hms_a_s("05:00:00")
    assert len(archivos["frequencies.txt"]) == 3 * 3  # 3 patrones x 3 servicios


def test_zip_generado_pasa_validacion(tmp_path):
    patrones, _ = _patrones()
    archivos, _ = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    ruta = tmp_path / "feed.zip"
    escribir_zip(archivos, str(ruta))
    rep = validar_zip(str(ruta))
    assert rep.ok, rep.errores
    with zipfile.ZipFile(ruta) as z:
        assert "frequencies.txt" in z.namelist()


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
