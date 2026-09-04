pluginManagement {
    // NOTE: កុំ crash ពេល CI មិនទាន់មាន local.properties ។
    // `flutter pub get` / `flutter build` នឹងបង្កើត local.properties ដោយស្វ័យប្រវត្តិ។
    // នៅលើ CI យើង fallback ទៅ FLUTTER_ROOT ដែល flutter-action  export ឲ្យ។
    val flutterSdkPath: String =
        run {
            val properties = java.util.Properties()
            val localProperties = file("local.properties")
            if (localProperties.exists()) {
                localProperties.inputStream().use { properties.load(it) }
            }
            properties.getProperty("flutter.sdk")
                ?: System.getenv("FLUTTER_ROOT")
                ?: System.getenv("FLUTTER_SDK")
                ?: throw GradleException(
                    "flutter.sdk not set in local.properties and FLUTTER_ROOT is not set. " +
                        "Run `flutter pub get` (or `flutter build apk`) first to generate android/local.properties."
                )
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

include(":app")
