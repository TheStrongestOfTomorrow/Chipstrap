# ProGuard rules for Chipstrap

# ─────────────────────────────────────────────────────────────────────
# Keep our own classes. R8 keeps Activity/Application/Service classes
# automatically (they're referenced in the manifest), but inner classes
# and reflective-loaded classes need explicit keeps.
# ─────────────────────────────────────────────────────────────────────
-keep class com.chipstrap.rbx.** { *; }
-keepclassmembers class com.chipstrap.rbx.** { *; }
-dontwarn com.chipstrap.rbx.**

# Keep Application / Activity subclasses explicitly (defensive — R8 should
# already do this from the manifest, but some AGP versions have bugs).
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ─────────────────────────────────────────────────────────────────────
# kotlinx.serialization
# ─────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chipstrap.rbx.**$$serializer { *; }
-keepclassmembers class com.chipstrap.rbx.** {
    *** Companion;
}
-keepclasseswithmembers class com.chipstrap.rbx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─────────────────────────────────────────────────────────────────────
# Compose / AndroidX — defensive keeps to avoid Compose runtime issues
# under R8 minification. The Compose compiler plugin already adds keeps
# for @Composable functions, but we add belt-and-braces here.
# ─────────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.datastore.** { *; }
-dontwarn androidx.compose.**

# ─────────────────────────────────────────────────────────────────────
# OkHttp / Okio
# ─────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ─────────────────────────────────────────────────────────────────────
# Kotlin coroutines — keep internal state machine classes
# ─────────────────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
