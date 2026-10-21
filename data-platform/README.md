# data-platform

Convierte las líneas del Metro de OpenStreetMap en un GTFS que OpenTripPlanner entiende, sin inventar nada: lo que se estima queda declarado en `salida/procedencia.json`.

```powershell
python -m conecta_data descargar   # salida/osm_metro.json desde Overpass
python -m conecta_data generar     # salida/metro-pty-gtfs.zip (+ copia en infrastructure/otp/datos)
python -m conecta_data validar     # errores = no publicar
python -m conecta_data todo        # los tres pasos
python -m pytest -q                # pruebas (datos sintéticos en tests/fixtures)
```

Solo usa la librería estándar de Python. Los supuestos (frecuencias, velocidad, horario) están en `config/supuestos_metro.json`.

**Al correr `generar` por primera vez, revisa la salida:** debe listar la Línea 1, la Línea 2 y el ramal del aeropuerto con las estaciones esperadas. Si una estación sale duplicada o con otro nombre, corrígela en OpenStreetMap (beneficia a todos) o ajusta `clave_estacion` en `osm.py`.
