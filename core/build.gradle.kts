plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nangmanroad.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        // Emulators reach the Mac's localhost at 10.0.2.2. A phone on Wi-Fi needs the Mac's LAN
        // address instead: sh gradlew -Pnangmanroad.syncUrl=http://192.168.0.10:8080 ...
        val syncUrl = providers.gradleProperty("nangmanroad.syncUrl").getOrElse("http://10.0.2.2:8080")
        buildConfigField("String", "SYNC_URL", "\"$syncUrl\"")
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
        buildConfig = true
    }
}

dependencies {
    api(project(":shared"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.foundation)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    api(libs.coil.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
}
