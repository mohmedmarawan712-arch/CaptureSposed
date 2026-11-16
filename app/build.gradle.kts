plugins {
    alias(libs.plugins.app)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.keshav.capturesposed"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.keshav.capturesposed"
        minSdk = 34
        targetSdk = 37
        versionCode = 11
        versionName = "1.1.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    val keystorePath = System.getenv("KEYSTORE_PATH") ?: "keystore/release.keystore"
    val keystorePass = System.getenv("KEYSTORE_PASS") ?: "CaptureSposed2026!"
    val keyFile = rootProject.file(keystorePath)

    signingConfigs {
        create("release") {
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = keystorePass
                keyAlias = "capturesposed"
                keyPassword = keystorePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (keyFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            if (keyFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.tooling)
    implementation(libs.libxposed.service)
    implementation(libs.libsu.core)
    compileOnly(libs.libxposed.api)
}