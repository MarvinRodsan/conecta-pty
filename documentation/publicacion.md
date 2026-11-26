# Publicar la app en Google Play

Requisitos previos: el backend ya funciona en el servidor con HTTPS (`infrastructure/produccion/README.md`).

## 1. Llave de firma (una sola vez)

Google Play identifica la app por esta llave. **Si se pierde, no se pueden publicar actualizaciones con la misma llave de subida** hasta pedir a Google que la reemplace. Guárdala con su contraseña en dos lugares seguros (por ejemplo, un gestor de contraseñas y una memoria USB), nunca en GitHub.

En PowerShell, con el JDK de Android Studio:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
  -keystore C:\llaves\conecta-pty.jks -alias conecta -keyalg RSA -keysize 4096 -validity 10000
```

## 2. Configurar el proyecto

En `mobile-android/local.properties` (no se sube a GitHub):

```properties
api.url=https://api.tu-dominio.com/
firma.archivo=C:/llaves/conecta-pty.jks
firma.alias=conecta
firma.clave=la-contraseña-de-la-llave
# opcional
sentry.dsn=https://...
```

Antes de cada versión, sube `versionCode` (entero, siempre mayor) y `versionName` en `mobile-android/app/build.gradle.kts`.

## 3. Generar y probar

```powershell
cd mobile-android
.\gradlew.bat :app:generateBaselineProfile   # opcional, con teléfono conectado: arranque más rápido
.\gradlew.bat bundleRelease
```

Resultado: `app/build/outputs/bundle/release/app-release.aab`. Antes de subirlo, instala la versión release en un teléfono (`.\gradlew.bat installRelease`) y prueba un viaje completo contra el servidor real: R8 a veces quita código que sí se usa.

## 4. Google Play Console

1. Crea la cuenta de desarrollador (pago único de US$25) en <https://play.google.com/console>.
2. Crea la app y activa **Play App Signing** (Google guarda la llave final; la tuya queda como llave de subida).
3. Ficha de la tienda: nombre, descripción corta y larga, ícono 512×512, gráfico de 1024×500 y al menos 2 capturas de pantalla.
4. **Política de privacidad** (URL pública, obligatoria porque la app pide ubicación). Debe decir: la ubicación se usa solo para buscar la estación más cercana y no se guarda; los reportes no incluyen datos personales; Sentry (si está activo) recibe informes de fallos sin datos personales.
5. **Seguridad de los datos**: declarar ubicación aproximada y precisa (uso: funcionalidad de la app, no se comparte, no se almacena) e informes de fallos si Sentry está activo.
6. Clasificación de contenido y público objetivo.
7. Sube el `.aab` primero a **Prueba interna** o **Prueba cerrada** con el grupo del piloto. Las cuentas personales nuevas deben hacer una prueba cerrada (actualmente, 12 testers durante 14 días) antes de poder publicar en producción; confirma el requisito vigente en la consola.

## 5. Aviso que debe seguir visible

Mientras no haya horarios oficiales, la app y la ficha de la tienda deben decir que los tiempos y frecuencias son **estimados**.
