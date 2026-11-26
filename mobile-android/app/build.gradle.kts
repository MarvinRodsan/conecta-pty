import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

// Ajustes privados: local.properties (no se sube a GitHub) o variables de entorno (CI).
val localProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun ajuste(propiedad: String, variableEntorno: String): String? =
    localProps.getProperty(propiedad) ?: System.getenv(variableEntorno)

android {
    namespace = "pa.conectapty.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "pa.conectapty.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Sin DSN, Sentry no se inicia y la app no envía nada (ver README de mobile-android)
        buildConfigField("String", "SENTRY_DSN", "\"${ajuste("sentry.dsn", "SENTRY_DSN") ?: ""}\"")
        // Estilo del mapa: OpenFreeMap es gratis y no pide API key
        buildConfigField("String", "MAPA_ESTILO", "\"https://tiles.openfreemap.org/styles/liberty\"")
    }

    signingConfigs {
        // Llave de Google Play: solo si está configurada en local.properties (ver documentation/publicacion)
        val llave = ajuste("firma.archivo", "FIRMA_ARCHIVO")
        if (llave != null) {
            create("publicacion") {
                storeFile = file(llave)
                storePassword = ajuste("firma.clave", "FIRMA_CLAVE")
                keyAlias = ajuste("firma.alias", "FIRMA_ALIAS")
                keyPassword = ajuste("firma.clave", "FIRMA_CLAVE")
            }
        }
    }

    buildTypes {
        debug {
            // Con `adb reverse tcp:8080 tcp:8080`, el teléfono (USB) o el emulador ven el backend
            // de tu PC como su propio localhost. Sin cambiar IPs ni abrir el firewall.
            buildConfigField("String", "API_URL", "\"http://localhost:8080/\"")
        }
        release {
            // R8: elimina código y recursos que no se usan -> APK más pequeño y rápido
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val api = ajuste("api.url", "CONECTA_API_URL") ?: "https://api.conectapty.example/"
            buildConfigField("String", "API_URL", "\"$api\"")
            // Sin llave propia se firma con la de depuración: sirve para probar, no para Google Play
            signingConfig = signingConfigs.findByName("publicacion") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.maplibre.android)
    implementation(libs.sentry.android.core)

    // Permite usar Baseline Profiles: arranque en frío más rápido
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Pruebas de pantallas (necesitan teléfono o emulador): ./gradlew connectedDebugAndroidTest
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    debugImplementation(libs.compose.ui.test.manifest)
}
