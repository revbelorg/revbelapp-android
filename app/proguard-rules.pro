# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/alexandrfrantskevich/Library/Android/sdk/tools/proguard/proguard-android.txt
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

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class com.news.revbel.database.**
-keep class android.support.v7.widget.SearchView { *; }
-keep class com.news.revbel.utilities.AnarchyRotatePulseIndicator { *; }
-keep class com.news.revbel.network.TorifiedGlideModule { *; }
-keep class com.news.revbel.utilities.RevShareProvider { *; }
-keep class android.support.v7.widget.ShareActionProvider { *; }

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }
-keep class com.artifex.mupdfdemo.** {*;}

-keep class javamail.** {*;}
-keep class javax.mail.** {*;}
-keep class javax.activation.** {*;}
-keep class com.sun.mail.dsn.** {*;}
-keep class com.sun.mail.handlers.** {*;}
-keep class com.sun.mail.smtp.** {*;}
-keep class com.sun.mail.util.** {*;}
-keep class mailcap.** {*;}
-keep class mimetypes.** {*;}
-keep class myjava.awt.datatransfer.** {*;}
-keep class org.apache.harmony.awt.** {*;}
-keep class org.apache.harmony.misc.** {*;}

-keep public class okhttp3.**
-keep public interface okhttp3.**

-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection
-keep class * extends android.webkit.WebChromeClient { *; }

-keeppackagenames org.jsoup.nodes

-dontwarn java.awt.**
-dontwarn java.beans.Beans
-dontwarn javax.security.**

-dontwarn im.delight.android.webview.**

-dontwarn okio.**
-dontwarn com.zhihu.matisse.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn java.lang.invoke.*
-dontwarn rx.**
-dontwarn **$$Lambda$*
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}