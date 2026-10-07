plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.jahanzaib.edgedirectcall"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jahanzaib.edgedirectcall"
        minSdk = 26
        targetSdk = 34
        // versionCode must keep increasing so each CI build installs over the last one
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    signingConfigs {
        // Fixed key committed to the repo so every build is signed the same way
        // and can be installed as an update over the previous one.
        create("release") {
            storeFile = file("release.jks")
            storePassword = "directcall"
            keyAlias = "directcall"
            keyPassword = "directcall"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Samsung's Edge-panel classes live in the phone's firmware (uses-library),
    // so we only compile against stubs and never ship them.
    compileOnly(project(":slook-stub"))
}
