# NutriMate keeps all sensitive data on-device.
-keepattributes Signature
-keepattributes *Annotation*

# Room entities are reflected at runtime.
-keep class com.nutrimate.app.data.local.entity.** { *; }

# kotlinx.serialization generated serializers.
-keepclassmembers class com.nutrimate.app.data.ai.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.nutrimate.app.data.ai.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}