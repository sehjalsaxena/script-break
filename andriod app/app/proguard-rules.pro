# ScriptBreak ProGuard / R8 Rules
# Keep ScriptBreak domain classes and models
-keep class com.example.scriptbreak.** { *; }

# Keep Firebase dependencies
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Kotlin Coroutines service loaders
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Jetpack Compose internals
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep CameraX & ZXing
-keep class androidx.camera.** { *; }
-keep class com.google.zxing.** { *; }
