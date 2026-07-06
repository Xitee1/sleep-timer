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

# Shizuku user service: ShellUserService is instantiated by name inside the
# Shizuku-spawned shell process, and the AIDL stub crosses the binder — neither
# may be renamed or stripped.
-keep class dev.xitee.sleeptimer.core.service.shizuku.ShellUserService { *; }
-keep class dev.xitee.sleeptimer.core.service.shizuku.IShellUserService { *; }
-keep class dev.xitee.sleeptimer.core.service.shizuku.IShellUserService$Stub { *; }
