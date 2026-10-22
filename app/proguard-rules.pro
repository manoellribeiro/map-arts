# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn okhttp3.internal.Util

# Processing creates its renderer (PGraphicsAndroid2D) from a class name string via reflection
-keep class processing.** { *; }

# Retrofit only keeps services that still have @GET/@POST methods; an unused service (e.g. GeoapifyApiService)
# loses its methods, so R8 assumes nothing implements it and turns the Proxy cast into a ClassCastException
-keep,allowobfuscation interface manoellribeiro.dev.martp.core.data.network.**.*ApiService
