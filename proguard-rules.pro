# Shared R8 rules for the release builds of the car and phone apps (vehicle/ and phone/ include this file).

# Pleos SDKs talk to the car's services over AIDL and reflection; keep them whole.
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-dontwarn ai.pleos.**

# NAVER Maps loads native code and views by name.
-keep class com.naver.maps.** { *; }
-dontwarn com.naver.maps.**

# Our own models are serialized to and from the sync server; keep the app code readable in crash logs.
-keep class com.nangmanroad.** { *; }

# Ktor finds its HTTP engine and serializers at runtime.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
