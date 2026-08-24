# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
   <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.muslimvn.data.local.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.ui.** { *; }

# Hilt
-keep class dagger.hilt.android.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.ComponentEntryPoint
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Adhan
-keep class com.batoulapps.adhan.** { *; }

# General Compose & Kotlin
-keep class androidx.compose.ui.platform.** { *; }
-keep class com.example.muslimvn.domain.models.** { *; }
-keep class com.example.muslimvn.presentation.viewmodels.** { *; }

# Gson/Serialization if used (Adhan uses it internally sometimes)
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Retrofit (Aladhan API)
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-keep class com.example.muslimvn.data.remote.** { *; }
-keep class com.example.muslimvn.data.local.entities.HijriDayEntity { *; }

# Gson parse assets/NameAllah.json — giữ DTO để reflection theo tên trường không vỡ khi minify
-keep class com.example.muslimvn.data.repository.NameAllahRepositoryImpl$* { *; }
