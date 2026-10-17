# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Coil
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.**

# Keep data models used across process death / saved state
-keep class com.ats.galery.data.** { *; }
