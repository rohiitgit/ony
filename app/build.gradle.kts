import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ony.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ony.app"
        minSdk = 26
        targetSdk = 34
        // CI sets ONY_BUILD to its run number, so every published build installs over the last.
        val build = System.getenv("ONY_BUILD")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "0.1.$build"

        // Personal builds only: a free MusicLink key from local.properties (musiclink.apiKey=...).
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        // Never on CI: published APKs carry no key, users paste their own in the app.
        val onCi = System.getenv("CI") == "true"
        val musicLinkKey = if (onCi) "" else localProps.getProperty("musiclink.apiKey", "").replace("\"", "")
        buildConfigField("String", "MUSICLINK_API_KEY", "\"$musicLinkKey\"")
    }

    // Release key comes from the environment (CI secrets); it never lives in the repo.
    // Without it, a local release build falls back to the debug key.
    val releaseKeystore = System.getenv("ONY_KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ONY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ONY_KEY_ALIAS")
                keyPassword = System.getenv("ONY_KEY_PASSWORD")
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
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Custom Tabs: MusicLink sign-up opens in a browser tab over Ony, so coming back is one tap.
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in plain JVM tests; this is the real one.
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
