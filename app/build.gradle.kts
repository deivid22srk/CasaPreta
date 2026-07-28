import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.casapreta.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.casapreta.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // --- Signing config for release APK ---
    // Keystore is injected by CI (decoded from KEYSTORE_BASE64 secret).
    // Local fallback: app/keystore/keystore.properties
    val keystoreProperties = Properties().apply {
        val localFile = rootProject.file("app/keystore/keystore.properties")
        if (localFile.exists()) {
            load(localFile.inputStream())
        }
    }
    val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
    val keystorePasswordEnv = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
    val keyAliasEnv = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("KEY_ALIAS")
    val keyPasswordEnv = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (!keystoreBase64.isNullOrEmpty()) {
                // CI mode: decode base64 keystore to a temp file
                val decoded = Base64.getDecoder().decode(keystoreBase64)
                val tmpFile = File(rootProject.buildDir, "tmp_keystore.jks")
                tmpFile.parentFile?.mkdirs()
                tmpFile.writeBytes(decoded)
                storeFile = tmpFile
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            } else if (keystoreProperties.getProperty("KEYSTORE_PATH") != null) {
                storeFile = File(keystoreProperties.getProperty("KEYSTORE_PATH"))
                storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
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

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // DataStore for settings persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Shizuku API (v13.1.5) - allows using system APIs with ADB/root privileges
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
