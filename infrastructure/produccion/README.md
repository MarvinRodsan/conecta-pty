# Producción

Cómo poner Conecta PTY en un servidor propio con HTTPS. Pensado para un VPS Linux (Ubuntu 24.04) de **4 GB de RAM**: OpenTripPlanner es lo que más memoria usa.

```
Internet ──443──► Caddy (HTTPS automático) ──► backend:8080 ──► db (PostGIS)
                                                      └───────► otp (rutas)
```

Solo Caddy recibe conexiones de afuera. La base de datos, el motor de rutas y el backend viven en la red interna de Docker y no publican puertos.

| Archivo | Qué hace |
|---|---|
| `docker-compose.yml` | Los cinco servicios, con límites de memoria y reinicio automático |
| `Caddyfile` | HTTPS con Let's Encrypt; solo deja pasar `/api/v1/*` y `/actuator/health` |
| `.env.example` | Dominio y contraseñas (se copia a `.env`, que nunca se sube) |
| `db-init/20-usuarios.sh` | Crea el usuario de la app con permisos mínimos |
| `firewall.sh` | Cierra todo menos SSH y 443 |
| `respaldos/` | Respaldo diario y prueba semanal de restauración |

## 1. Preparar el servidor (una vez)

1. Contrata el VPS y apunta el dominio: un registro **A** de `api.tu-dominio.com` a la IP del servidor.
2. Entra por SSH, crea un usuario normal y deja de usar `root`. Usa llave SSH en vez de contraseña.
3. Instala Docker: <https://docs.docker.com/engine/install/ubuntu/> y agrega tu usuario al grupo `docker`.
4. Firewall:

   ```bash
   sudo sh firewall.sh
   ```

   Si cambiaste el puerto de SSH: `sudo PUERTO_SSH=2222 sh firewall.sh`. **No cierres la sesión SSH** hasta comprobar en otra ventana que puedes volver a entrar.

## 2. Instalar Conecta PTY

```bash
sudo git clone <url-del-repo> /opt/conecta-pty
sudo chown -R $USER /opt/conecta-pty
cd /opt/conecta-pty/infrastructure/produccion

cp .env.example .env && chmod 600 .env
nano .env          # dominio, correo y dos contraseñas distintas (openssl rand -base64 32)
```

Los datos que necesita el motor de rutas van en `infrastructure/otp/datos/` (no están en Git): `metro-pty-gtfs.zip` (lo genera `data-platform`) y `panama-latest.osm.pbf` (`../descargar_osm.sh`). Puedes generarlos en tu PC y subirlos con `scp`.

```bash
docker compose run --rm otp-construir   # arma el grafo (~2.5 GB de RAM, unos minutos)
docker compose up -d --build            # base de datos, motor de rutas, backend y Caddy
docker compose logs -f backend          # espera "Started ConectaPtyApplication"
```

Prueba: `https://api.tu-dominio.com/api/v1/salud`. La primera vez Caddy tarda unos segundos en obtener el certificado.

Luego compila la app de publicación apuntando al servidor: en `mobile-android/local.properties`, `api.url=https://api.tu-dominio.com/` (ver `documentation/publicacion.md`).

## 3. Actualizar

```bash
cd /opt/conecta-pty && git pull
cd infrastructure/produccion && docker compose up -d --build backend
```

**Datos nuevos** (GTFS o mapa): copia los archivos a `infrastructure/otp/datos/` y rearma el grafo con el resto apagado, porque en 4 GB no caben las dos cosas a la vez:

```bash
docker compose stop otp backend
docker compose run --rm otp-construir
docker compose up -d
```

El backend importa las estaciones del GTFS nuevo al arrancar.

## 4. Base de datos

Hay dos usuarios:

| Usuario | Lo usa | Puede |
|---|---|---|
| `conecta_admin` | Flyway (migraciones) y respaldos | Todo; es el dueño de las tablas |
| `conecta_app` | El backend al atender peticiones | Leer y escribir filas. No puede crear, borrar ni alterar tablas |

Si alguien encontrara una forma de inyectar SQL en el backend, con `conecta_app` no podría borrar tablas ni crear funciones. Las contraseñas están solo en `.env`.

`db-init/20-usuarios.sh` corre una única vez, cuando el volumen `pgdata` está vacío. Para cambiar una contraseña después:

```bash
docker compose exec db psql -U conecta_admin -d conecta -c "ALTER ROLE conecta_app PASSWORD 'nueva'"
# y actualiza .env, luego: docker compose up -d backend
```

## 5. Respaldos

```bash
crontab -e
```

```cron
# Respaldo diario a las 3:15 (se guardan 14 días en /var/backups/conecta)
15 3 * * *  /opt/conecta-pty/infrastructure/produccion/respaldos/respaldar.sh >> $HOME/conecta-respaldo.log 2>&1
# Cada domingo: restaurar el último respaldo en una base temporal y comprobar que tiene datos
45 3 * * 0  /opt/conecta-pty/infrastructure/produccion/respaldos/probar-restauracion.sh >> $HOME/conecta-respaldo.log 2>&1
```

Antes del primer cron: `sudo mkdir -p /var/backups/conecta && sudo chown $USER /var/backups/conecta`.

**Copia fuera del servidor.** Un respaldo que vive en el mismo servidor se pierde junto con él. Opciones gratis o baratas: activar los respaldos automáticos del proveedor del VPS (~20 % del precio), o copiar la carpeta a un bucket con [rclone](https://rclone.org) añadiendo al cron diario `rclone copy /var/backups/conecta remoto:conecta-respaldos`.

**Restaurar de verdad** (si se perdió la base):

```bash
docker compose stop backend
docker compose exec -T db sh -c 'dropdb -U conecta_admin conecta && createdb -U conecta_admin -T template0 conecta'
docker compose exec -T db sh -c 'pg_restore -U conecta_admin -d conecta' < /var/backups/conecta/conecta-AAAA-MM-DD-HHMM.dump
# Los permisos de las tablas vienen en el respaldo; los de la base (nueva) hay que repetirlos
docker compose exec -T db psql -U conecta_admin -d conecta -c "REVOKE ALL ON DATABASE conecta FROM PUBLIC; GRANT CONNECT ON DATABASE conecta TO conecta_app"
docker compose up -d backend
```

## 6. Monitoreo

`/actuator/health` responde `{"status":"UP"}` solo si el backend, la base de datos **y** el motor de rutas funcionan; si alguno falla responde `DOWN` con código 503.

En [UptimeRobot](https://uptimerobot.com) (plan gratis, revisa cada 5 minutos) crea dos monitores:

| Tipo | URL | Avisa si |
|---|---|---|
| Keyword, palabra `UP` | `https://api.tu-dominio.com/actuator/health` | Se cae el backend, la BD o el motor de rutas |
| SSL / dominio | `https://api.tu-dominio.com` | El certificado está por vencer (Caddy lo renueva solo; esto avisa si falla) |

Configura el aviso por correo o por la app del teléfono. [Better Stack](https://betterstack.com/uptime) sirve igual.

Para ver qué pasó: `docker compose ps` (estado), `docker compose logs --since 1h backend otp` (errores), `docker stats` (memoria).

## 7. Seguridad: qué está cubierto

- **Red:** firewall con solo SSH y 443; servicios internos sin puertos publicados.
- **HTTPS:** certificado automático, HSTS, HTTP/3. No se acepta HTTP sin cifrar.
- **Superficie mínima:** Caddy solo deja pasar la API y el chequeo de salud; `/docs` y el resto de `/actuator` están apagados en el perfil `prod`.
- **Abuso:** límite de peticiones por IP (30 búsquedas de ruta por minuto) usando la IP real que reenvía Caddy.
- **Errores:** sin detalles internos ni trazas en las respuestas.
- **Base de datos:** usuario de la app sin permisos de esquema; contraseñas en `.env`.
- **Contenedor:** el backend corre sin `root`.
- **Dependencias:** Dependabot y Trivy en GitHub (`.github/`) avisan de librerías e imágenes con vulnerabilidades conocidas.
- **Privacidad:** los reportes de la app no guardan IP, cuenta, ubicación ni identificador del teléfono.

Ningún sistema es imposible de atacar: el objetivo son varias capas y no guardar datos personales que no hacen falta. Mantén el servidor al día con `sudo apt update && sudo apt upgrade` (o activa `unattended-upgrades`).
