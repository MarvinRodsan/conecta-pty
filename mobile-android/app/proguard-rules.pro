# kotlinx.serialization y Retrofit 3 traen sus propias reglas de R8.
# Mantener los nombres de los DTO ayuda a leer errores del servidor en reportes de fallos.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep,includedescriptorclasses class pa.conectapty.app.datos.remoto.**$$serializer { *; }
-keepclassmembers class pa.conectapty.app.datos.remoto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
