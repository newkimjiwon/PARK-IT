plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nangmanroad.phone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nangmanroad.phone"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    packaging {
        // Compress native libraries inside the APK (the NAVER map engine alone is ~24MB per ABI).
        jniLibs { useLegacyPackaging = true }
    }
    buildTypes {
        release {
            // Shrink code and resources and ship only the 64-bit ABIs (car head units and current
            // emulators) so the APK is small enough to hand in.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), rootProject.file("proguard-rules.pro"))
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
            // No release keystore yet: sign with the local debug key so the APK installs for demos and review.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.tooling.preview)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
