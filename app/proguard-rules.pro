# Nothing app-specific yet: OkHttp, Compose and AndroidX ship their own keep rules.

# Google code scanner (ML Kit): its internals are created by reflection and break when R8 renames them.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
