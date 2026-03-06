# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and attributes for debugging and reflection
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Hide the original source file name in stack traces
-renamesourcefileattribute SourceFile

# --- Room ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
# Removed unresolved TableChange rule

# --- Retrofit ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# --- Gson ---
# Keep only what's necessary for Gson to function with reflection
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keep @com.google.gson.annotations.SerializedName class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Hilt / Dagger ---
# Hilt usually provides its own rules, but keeping these for safety
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclassmembers class * {
  @javax.inject.Inject <init>(...);
}

# --- Firebase ---
# Firebase provides its own consumer rules, so we keep only what's needed for our data models
-keepattributes *Annotation*

# --- App Specific Data Models (Keep DTOs and Entities for Serialization/Reflection) ---
# These are critical for Room and Retrofit/Gson
-keep class com.skillmorph.skillmorph.data.local.entities.** { *; }
-keep class com.skillmorph.skillmorph.data.remote.dtos.** { *; }

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# Removed invalid C-style handler rule

# --- Compose ---
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }

# --- Credential Manager & Google ID ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**