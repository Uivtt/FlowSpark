# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Gson 反射模型（AI DTO）
-keep class com.flowspark.app.ai.dto.** { *; }
-keep class com.flowspark.app.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
