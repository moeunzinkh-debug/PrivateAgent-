# =============================================================
# PrivateLM — app-level R8/ProGuard rules (release builds only)
#
# The Flutter Gradle plugin automatically adds, when minification
# is enabled:
#   1. proguard-android-optimize.txt   (AGP defaults)
#   2. flutter_proguard_rules.pro      (engine/plugin keeps)
#   3. this file (android/app/proguard-rules.pro)
#
# Third-party AARs (Firebase, LiteRT-LM, WebView, ...) ship their
# own consumer ProGuard rules.
#
# Everything below covers the LOCAL native plugins: their C/C++ code
# looks Java classes/methods up by name over JNI, so R8 must not
# rename or strip those symbols or the app crashes at runtime with
# NoSuchMethodError / NoSuchFieldError.
# =============================================================

# Native methods are invoked from C/C++ by name. Keep them on every
# class that survives shrinking (covers LlamaFlutterAndroidPlugin,
# SdFlutterAndroidPlugin and any future local JNI surface).
-keepclasseswithmembers class * {
    native <methods>;
}

# llama_flutter_android: the Kotlin layer hands a (Result<Unit>) -> Unit
# lambda to libllama_jni.so, which then calls invoke(Object) on that
# object by name (GetMethodID "invoke" "(Ljava/lang/Object;)Ljava/lang/Object;").
-keep class kotlin.jvm.functions.Function1 {
    *;
}

# sd_flutter_android: libsd_jni.so calls onProgress(II)V by name on the
# ProgressCallback inner-class instance (GetMethodID "onProgress" "(II)V").
-keep class com.example.sd_flutter_android.SdFlutterAndroidPlugin$ProgressCallback {
    public void onProgress(int, int);
}
