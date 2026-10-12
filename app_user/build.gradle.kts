// DTS-TEAM.v1: sadece client. Bu modul tamamen bagimsizdir:
// surumler ../gradle/libs.versions.toml'dan gelir, wrapper ve CI bu zip'in icindedir.
// Kotlin eklentisi satiri yok: AGP 9+ Kotlin'i dahili getirir.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hostuser"
    compileSdk { version = release(36) }

    // AGP 9.4.0'in varsayilan NDK'si. Telefonlarda 16 KB sayfa boyutu icin NDK r28+ gerekir.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.example.hostuser"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild { cmake { arguments += "-DANDROID_STL=c++_static" } }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Release imzasi: ortam degiskenleri verilirse onlar, yoksa debug anahtari.
    // (Debug anahtariyla imzali release APK telefona kurulur ama Play Store'a yuklenmez.)
    val ksPath = System.getenv("DTS-TEAM.v1_KEYSTORE")
    signingConfigs {
        if (!ksPath.isNullOrBlank()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("DTS-TEAM.v1_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DTS-TEAM.v1_KEY_ALIAS")
                keyPassword = System.getenv("DTS-TEAM.v1_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Bagimliliklar: Compose, Room, gibi ihtiyaclar daha eklenecek
