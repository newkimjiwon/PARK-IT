import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// NAVER Maps needs a key that stays out of git: naver.ncpKeyId=... in local.properties, or
// -Pnangmanroad.naverKeyId=... on the command line. Without one the apps draw the Canvas map.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val naverKeyId: String = providers.gradleProperty("nangmanroad.naverKeyId").orNull
    ?: localProperties.getProperty("naver.ncpKeyId").orEmpty()
// "ncp" for a key ID from the new Maps console (NcpKeyClient); "legacy" for a Client ID labelled
// X-NCP-APIGW-API-KEY-ID (NaverCloudPlatformClient). A 401 with one usually means the other.
val naverClientType: String = providers.gradleProperty("nangmanroad.naverClientType").orNull
    ?: localProperties.getProperty("naver.clientType") ?: "ncp"

android {
    namespace = "com.nangmanroad.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        // Emulators reach the Mac's localhost at 10.0.2.2. A phone on Wi-Fi needs the Mac's LAN
        // address instead: sh gradlew -Pnangmanroad.syncUrl=http://192.168.0.10:8080 ...
        val syncUrl = providers.gradleProperty("nangmanroad.syncUrl").getOrElse("http://10.0.2.2:8080")
        buildConfigField("String", "SYNC_URL", "\"$syncUrl\"")
        val configUrl = providers.gradleProperty("nangmanroad.configUrl").getOrElse("https://34.64.170.97/map-config.json")
        buildConfigField("String", "MAP_CONFIG_URL", "\"$configUrl\"")
        buildConfigField("String", "NAVER_NCP_KEY_ID", "\"${naverKeyId.trim()}\"")
        buildConfigField("String", "NAVER_CLIENT_TYPE", "\"${naverClientType.trim()}\"")
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
    api(libs.naver.map.compose)
    api(libs.naver.map.sdk)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
}
