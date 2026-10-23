plugins { id("com.android.application") }

android {
    namespace = "dev.pocoglass"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.pocoglass"
        minSdk = 33          // RenderEffect/AGSL; Poco on HyperOS is Android 14+
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.0")
    implementation("io.github.libxposed:service:101.0.0")
}
