plugins {
    id("com.android.application")
}

android {
    namespace = "com.lazyneil.matte"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lazyneil.matte"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        // Debug-signed release so it installs without a keystore. Use a real key before publishing to a store.
        release { signingConfig = signingConfigs.getByName("debug") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.android.material:material:1.14.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
