# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# kotlinx.serialization: keep the generated $serializer companions for @Serializable
# route objects in navigation/Routes.kt. Navigation-Compose reflects on these at
# runtime to (de)serialize route arguments; without the keep, R8 strips the
# companion metadata and the NavHost fails to resolve destinations in release.
-keepattributes InnerClasses
-keepclassmembers class dev.xitee.sleeptimer.navigation.** {
    public static ** Companion;
    public static final *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.xitee.sleeptimer.navigation.**$$serializer { *; }

# Shizuku AIDL + provider: the AAR ships consumer-proguard rules, but we reference
# rikka.shizuku.ShizukuProvider by fully-qualified name in AndroidManifest.xml.
# The manifest reference keeps the class; this keeps the IShizukuService AIDL stub
# we use in ShizukuShell.
-keep class moe.shizuku.server.IShizukuService { *; }
-keep class moe.shizuku.server.IShizukuService$Stub { *; }
-keep class moe.shizuku.server.IRemoteProcess { *; }
