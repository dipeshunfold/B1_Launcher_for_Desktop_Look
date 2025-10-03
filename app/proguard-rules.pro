# --- General Android ---
-keep class androidx.core.app.CoreComponentFactory { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.android.gms.ads.** { *; }

# --- Glide ---
# Keeps AppGlideModule implementations and generated API
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType
-keep public interface com.bumptech.glide.load.resource.transcode.ResourceTranscoder
-keep public class * implements com.bumptech.glide.load.resource.transcode.ResourceTranscoder {
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
   public <init>(...);
}
# For Applications that use Generated APIs
-keep public class com.bumptech.glide.GeneratedAppGlideModuleImpl

# --- Retrofit & OkHttp ---
# Keeps interfaces and models used by Retrofit
-keep interface retrofit2.http.** { *; }
-dontwarn retrofit2.Platform$Java8
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**

# --- Google Material Components ---
-keep class com.google.android.material.theme.MaterialComponentsViewInflater { *; }

# --- Lottie ---
# Keeps classes and methods that Lottie might access dynamically
-keep class com.airbnb.lottie.** { *; }

# --- Room ---
# Room uses annotations to generate code, so we don't need to keep model classes
# unless you are using them in a way that R8 might remove.
# The following rule is a safe catch-all for entities.
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
    public static ** a(...);
}
-keep @androidx.room.Entity public class *
-keep @androidx.room.Dao public interface * {
    *;
}

# --- ViewBinding ---
# Keeps the binding classes generated from your layouts
-keepclassmembers class **.databinding.*Binding {
    public static ** inflate(...);
    public static ** bind(...);
}

# --- Keep your Application's Models/Entities ---
# IMPORTANT: Keep all of your data model classes that are used with libraries like Retrofit or Room.
-keep class com.bluelight.computer.winlauncher.prolauncher.model.** { *; }
-keep class com.bluelight.computer.winlauncher.prolauncher.database.** { *; }
-keep class com.bluelight.computer.winlauncher.prolauncher.apicalling.** { *; }