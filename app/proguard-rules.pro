# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Modern Android ProGuard configuration

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep application class
-keep class tech.wdg.incomingactivitygateway.GatewayApplication { *; }

# Keep service classes
-keep class tech.wdg.incomingactivitygateway.SmsReceiverService { *; }
-keep class tech.wdg.incomingactivitygateway.NotificationListenerService { *; }

# Keep broadcast receivers
-keep class tech.wdg.incomingactivitygateway.*BroadcastReceiver { *; }

# Keep ForwardingConfig for serialization
-keep class tech.wdg.incomingactivitygateway.ForwardingConfig { *; }
-keep class tech.wdg.incomingactivitygateway.ForwardingConfig$ActivityType { *; }

# Keep activities for proper lifecycle
-keep class tech.wdg.incomingactivitygateway.*Activity { *; }

# Fix R8 missing classes issues
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.lang.model.element.Modifier
-dontwarn org.apache.commons.lang3.reflect.TypeUtils

# Keep Apache Commons Lang3 reflection utilities
-keep class org.apache.commons.lang3.reflect.** { *; }
-dontwarn org.apache.commons.lang3.reflect.**

# Keep Error Prone annotations
-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Modern optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep AndroidX components
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Apache Commons Text
-keep class org.apache.commons.text.** { *; }
-dontwarn org.apache.commons.text.**

# Modern Android compatibility
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Prevent obfuscation of native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep annotation classes
-keep @interface *

# Modern memory optimization - Enable for release builds
-dontshrink
-dontoptimize
