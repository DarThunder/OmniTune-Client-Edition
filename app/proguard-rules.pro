# Reglas de R8 (solo aplican si activas isMinifyEnabled en release).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.omnitune.app.data.** { kotlinx.serialization.KSerializer serializer(...); }
