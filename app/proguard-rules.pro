# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.kami911.wifienabler.data.** { *; }

# Keep BroadcastReceivers and Services
-keep class com.kami911.wifienabler.receiver.** { *; }
-keep class com.kami911.wifienabler.service.** { *; }

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
