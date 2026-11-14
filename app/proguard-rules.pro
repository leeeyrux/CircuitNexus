# CircuitNexus ProGuard Rules

# Keep Room entities and DAOs
-keep class com.circuitnexus.model.** { *; }
-keep class com.circuitnexus.data.** { *; }

# Keep ViewModels
-keep class com.circuitnexus.viewmodel.** { *; }

# Keep Engine classes
-keep class com.circuitnexus.engine.** { *; }

# Keep Navigation
-keep class com.circuitnexus.navigation.** { *; }

# Keep DI
-keep class com.circuitnexus.di.** { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class com.circuitnexus.model.**$$serializer { *; }

# OkHttp / Retrofit (for AI)
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Room
-keep class androidx.room.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# Hilt (if used)
-keep class dagger.hilt.** { *; }

# Prevent obfuscation of enum values used in serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}