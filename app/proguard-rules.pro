# Keep model classes used by kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.everybuy.mobile.**$$serializer { *; }
-keepclassmembers class com.everybuy.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.everybuy.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
