# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.rokid.nutrition.phone.network.model.** { *; }
-keep class com.rokid.nutrition.phone.data.entity.** { *; }

# Rokid SDK
-keep class com.rokid.** { *; }
