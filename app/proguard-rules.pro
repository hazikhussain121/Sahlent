# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Moshi
-keep class com.sahlent.data.api.models.** { *; }
-keepclassmembers class com.sahlent.data.api.models.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
