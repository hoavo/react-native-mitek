# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Dev\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


-keep public class * extends android.support.v4.** {*;}
-keep public class * extends android.app.Fragment

# Keep MiSnap API classes
-keep class com.miteksystems.misnap.analyzer.*
-keepclassmembers class com.miteksystems.misnap.analyzer.* {
	*;
}

-keep class com.miteksystems.misnap.events.*
-keepclassmembers class com.miteksystems.misnap.events.* {
	*;
}
-keepclassmembers class ** {
    public void onEvent*(**);
}
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-keep class com.miteksystems.misnap.params.*
-keepclassmembers class com.miteksystems.misnap.params.* {
	*;
}

-keepattributes InnerClasses,EnclosingMethod

# Keep MiSnap Workflow classes
-keep class com.miteksystems.misnap.misnapworkflow_UX2.**
-keepclassmembers class com.miteksystems.misnap.misnapworkflow_UX2.** {
    *;
}