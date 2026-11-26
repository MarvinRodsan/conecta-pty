import json
import zipfile
from datetime import date
from pathlib import Path

import pytest

from conecta_data import osm, publicar
from conecta_data.__main__ import main
from conecta_data.feriados import calendar_dates, domingo_de_pascua, feriados_entre
from conecta_data.gtfs import construir_feed, escribir_zip
from conecta_data.validar import validar_zip

AQUI = Path(__file__).parent
CONFIG = AQUI.parent / "config"
SUPUESTOS = json.loads((CONFIG / "supuestos_metro.json").read_text(encoding="utf-8"))
FERIADOS = json.loads((CONFIG / "feriados_panama.json").read_text(encoding="utf-8"))
MUESTRA = AQUI / "fixtures" / "overpass_muestra.json"


def _patrones():
    return osm.leer_patrones(json.loads(MUESTRA.read_text(encoding="utf-8")))[0]


# ---------- Feriados ----------

def test_pascua_y_feriados_moviles():
    assert domingo_de_pascua(2026) == date(2026, 4, 5)
    assert domingo_de_pascua(2027) == date(2027, 3, 28)
    fechas = {f.fecha: f.nombre for f in feriados_entre(FERIADOS, date(2026, 1, 1), date(2027, 12, 31))}
    assert fechas[date(2026, 2, 17)] == "Martes de Carnaval"
    assert fechas[date(2026, 4, 3)] == "Viernes Santo"
    assert fechas[date(2027, 2, 9)] == "Martes de Carnaval"
    assert fechas[date(2027, 3, 26)] == "Viernes Santo"
    assert fechas[date(2026, 11, 3)] == "Separación de Panamá de Colombia"


def test_feriado_en_dia_de_semana_cambia_a_horario_de_domingo():
    # 1 de mayo de 2026 es viernes: se apaga L-V y se enciende DOM
    filas = calendar_dates(feriados_entre(FERIADOS, date(2026, 5, 1), date(2026, 5, 1)),
                           SUPUESTOS["servicios"], "DOM")
    assert sorted((f["service_id"], f["exception_type"]) for f in filas) == [("DOM", "1"), ("LV", "2")]


def test_feriado_en_domingo_no_genera_excepciones():
    # 1 de enero de 2028 es sábado; 3 de noviembre de 2024 fue domingo
    filas = calendar_dates(feriados_entre(FERIADOS, date(2024, 11, 3), date(2024, 11, 3)),
                           SUPUESTOS["servicios"], "DOM")
    assert filas == []


def test_traslado_de_domingo_a_lunes_solo_si_se_activa():
    cfg = dict(FERIADOS, trasladar_domingo_a_lunes=True)
    fechas = [f.fecha for f in feriados_entre(cfg, date(2024, 11, 1), date(2024, 11, 6))]
    assert date(2024, 11, 4) in fechas
    assert date(2024, 11, 4) not in [f.fecha for f in feriados_entre(FERIADOS, date(2024, 11, 1), date(2024, 11, 6))]


def test_feed_con_feriados_es_valido(tmp_path):
    archivos, proc = construir_feed(_patrones(), SUPUESTOS, hoy=date(2026, 10, 21), feriados=FERIADOS)
    assert "calendar_dates.txt" in archivos
    assert proc["feriados"]["estado"] == "por_confirmar"
    assert {"fecha": "2026-11-03", "nombre": "Separación de Panamá de Colombia"} in proc["feriados"]["fechas"]
    ruta = tmp_path / "feed.zip"
    escribir_zip(archivos, str(ruta))
    rep = validar_zip(str(ruta))
    assert rep.ok, rep.errores
    assert not any("calendar_dates" in a for a in rep.avisos)


def test_validador_detecta_calendar_dates_roto(tmp_path):
    archivos, _ = construir_feed(_patrones(), SUPUESTOS, hoy=date(2026, 10, 21), feriados=FERIADOS)
    archivos["calendar_dates.txt"][0]["service_id"] = "NO_EXISTE"
    archivos["calendar_dates.txt"][1]["date"] = "2026-13-01"
    ruta = tmp_path / "malo.zip"
    escribir_zip(archivos, str(ruta))
    texto = " ".join(validar_zip(str(ruta)).errores)
    assert "servicio inexistente NO_EXISTE" in texto and "fecha inválida" in texto


# ---------- Accesibilidad ----------

def test_accesibilidad_solo_si_esta_verificada():
    acc = {"estaciones": {
        "san-miguelito": {"silla_ruedas": "si", "verificado_el": "2026-11-05", "fuente": "Visita en campo"},
        "a": {"silla_ruedas": "no"},  # sin fecha ni fuente: se ignora
    }}
    archivos, proc = construir_feed(_patrones(), SUPUESTOS, hoy=date(2026, 10, 21), accesibilidad=acc)
    por_id = {s["stop_id"]: s for s in archivos["stops.txt"]}
    assert por_id["EST_san-miguelito"]["wheelchair_boarding"] == "1"
    assert por_id["N3"]["wheelchair_boarding"] == "1"           # andén de San Miguelito
    assert por_id["EST_a"]["wheelchair_boarding"] == "0"
    assert proc["accesibilidad"]["estaciones_verificadas"] == ["san-miguelito"]


# ---------- Publicación y reversión ----------

def _feed(carpeta: Path, patrones) -> Path:
    archivos, proc = construir_feed(patrones, SUPUESTOS, hoy=date(2026, 10, 21))
    carpeta.mkdir(parents=True, exist_ok=True)
    escribir_zip(archivos, str(carpeta / "metro-pty-gtfs.zip"))
    (carpeta / "procedencia.json").write_text(json.dumps(proc), encoding="utf-8")
    return carpeta / "metro-pty-gtfs.zip"


def test_comparar_bloquea_si_desaparece_una_linea(tmp_path):
    patrones = _patrones()
    actual = _feed(tmp_path / "actual", patrones)
    sin_l2 = [p for p in patrones if not p.ruta_id.startswith("L2")]
    nuevo = _feed(tmp_path / "nuevo", sin_l2)
    c = publicar.comparar(actual, nuevo)
    assert c.rutas_perdidas == ["L2", "L2R"]
    assert "líneas completas" in c.bloqueante()
    assert publicar.comparar(actual, actual).bloqueante() is None


def test_respaldar_instalar_y_revertir(tmp_path):
    salida, otp = tmp_path / "salida", tmp_path / "otp"
    otp.mkdir()
    _feed(salida, _patrones())
    original = (salida / "metro-pty-gtfs.zip").read_bytes()

    respaldo = publicar.respaldar(salida)
    assert (respaldo / "metro-pty-gtfs.zip").read_bytes() == original

    _feed(tmp_path / "nuevo", _patrones()[:2])
    publicar.instalar(tmp_path / "nuevo", salida, otp)
    assert (salida / "metro-pty-gtfs.zip").read_bytes() != original
    assert (otp / "metro-pty-gtfs.zip").read_bytes() == (salida / "metro-pty-gtfs.zip").read_bytes()

    publicar.revertir(salida, otp)
    assert (salida / "metro-pty-gtfs.zip").read_bytes() == original
    assert (otp / "metro-pty-gtfs.zip").read_bytes() == original
    assert any(v.name.endswith("antes-de-revertir") for v in publicar.versiones(salida))


def test_revertir_sin_historial_avisa(tmp_path):
    with pytest.raises(FileNotFoundError):
        publicar.revertir(tmp_path, None)


def test_cli_actualizar_sin_red(tmp_path, monkeypatch):
    """`actualizar --entrada` hace todo el ciclo sin Internet."""
    import conecta_data.__main__ as cli
    monkeypatch.setattr(cli, "SALIDA", tmp_path)
    monkeypatch.setattr(cli, "ZIP", tmp_path / "metro-pty-gtfs.zip")
    monkeypatch.setattr(cli, "OTP_DIR", tmp_path / "no-existe")
    assert main(["actualizar", "--entrada", str(MUESTRA)]) == 0
    with zipfile.ZipFile(tmp_path / "metro-pty-gtfs.zip") as z:
        assert "calendar_dates.txt" in z.namelist()
    assert main(["actualizar", "--entrada", str(MUESTRA)]) == 0   # segunda vez: respalda la primera
    assert len(publicar.versiones(tmp_path)) == 1
