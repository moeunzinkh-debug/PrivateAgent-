import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}
val allowDebugReleaseSigning =
    System.getenv("PRIVATELM_ALLOW_DEBUG_RELEASE_SIGNING") == "true"
val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
if (isReleaseBuild && !hasReleaseKeystore && !allowDebugReleaseSigning) {
    throw GradleException(
        "Release signing is not configured. Add android/key.properties using " +
            "android/key.properties.example. Do not publish an APK signed with a debug key."
    )
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.orailnoor.privatelm"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "com.orailnoor.privatelm"
        minSdk = 28
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        multiDexEnabled = true
        ndk {
            // Ship only ARM64 native code.
            //
            // Without this, the APK bundles the Flutter engine, the LiteRT-LM
            // AAR (litertlm-android ships .so files for every ABI) and the
            // Stable Diffusion runtime for armeabi-v7a + arm64-v8a + x86_64,
            // which is 3x the native size. Every phone shipping Android 9+
            // that can run this app is arm64-v8a (and the llama.cpp plugin
            // already builds arm64-only), so 32-bit/x86 copies are dead weight.
            //
            // Note: Flutter 3.35+ sets its own default abiFilters before this
            // block runs — clear() first so arm64-v8a wins in all cases.
            abiFilters.clear()
            abiFilters += listOf("arm64-v8a")
        }
    }


    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // R8 code shrinking + obfuscation and Android resource shrinking.
            // The Flutter Gradle plugin automatically pairs these with
            // proguard-android-optimize.txt, the engine's flutter_proguard_rules.pro,
            // and android/app/proguard-rules.pro (kept JNI surfaces for the
            // local llama / stable-diffusion plugins).
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    packaging {
        jniLibs {
            // Store .so files compressed inside the APK instead of the legacy
            // uncompressed layout. Requires API 23+ (minSdk is 28). This is
            // what Google Play does for App Bundles and typically saves 30-40%
            // of the native runtime size (llama/ggml, stable-diffusion,
            // LiteRT-LM, Flutter engine).
            useLegacyPackaging = false
            // If two sources ship the same native lib (e.g. the SD plugin's
            // bundled libomp.so), keep the first one deterministically
            // instead of failing the packaging step.
            pickFirsts += listOf("lib/arm64-v8a/libomp.so")
        }
        resources {
            // Drop license/meta noise duplicated across AARs.
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

flutter {
    source = "../.."
}
