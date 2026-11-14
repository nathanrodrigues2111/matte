import java.util.Properties

plugins {
    id("com.android.application")
}

// Release key lives outside the repo: ~/.config/matte/keystore.properties
// (storeFile, storePassword, keyAlias, keyPassword). Without it, release falls back to the debug key.
val keyProps = Properties().apply {
    val f = file(System.getProperty("user.home") + "/.config/matte/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.lazyneil.matte"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lazyneil.matte"
        minSdk = 30
        targetSdk = 37
        versionCode = 3
        versionName = "0.1.2"
    }
    signingConfigs {
        if (keyProps.isNotEmpty()) create("release") {
            storeFile = file(keyProps.getProperty("storeFile"))
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
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
