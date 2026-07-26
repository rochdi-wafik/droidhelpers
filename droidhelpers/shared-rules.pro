# ***********************************************************************
#   Shared-Rules
# ***********************************************************************
# - All ProGuard/R8 rules are defined once in this file.
#   This avoids duplicating rules across proguard-rules.pro and
#   consumer-rules.pro, since both reference this file in build.gradle:
#
#   $ proguardFiles    → applies rules when the module itself is minified
#   $ consumerProguardFiles → passes rules to any app using this module
# ***********************************************************************

##---------------Begin: Rules for Gson  ----------

# Keep generic type signatures (required for TypeToken and generic Gson usage)
# Keep annotation metadata (required for @SerializedName, @JsonAdapter, etc.)
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-dontwarn sun.misc.**

# All Models that will be serialized/deserialized by Gson
## Keep SqlPreferences objects
-keep class com.iorgana.droidhelpers.db.** { *; }
## Keep Crypto algorithms
-keep class javax.crypto.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep fields annotated with @SerializedName (Gson uses field names for JSON mapping)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Only if needed outside the model package, Keep constructors for classes that are instantiated by Gson via reflection.
# -keepclassmembers class * {
#     <init>(...);
# }

# Keep TypeToken and subclasses for R8 v3.0+ (required for generic type resolution)
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: Rules for Gson  ----------