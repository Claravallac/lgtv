plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.himari.telatv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.himari.telatv"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.2"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "META-INF/versions/**/OSGI-INF/MANIFEST.MF" } }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // WebSocket (ws/wss) para falar com a TV. Única dependência de rede.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // SSH (apagar a tela por luna-send em TVs com root)
    implementation("com.github.mwiede:jsch:0.2.18")
    // Chaves ed25519 no Android: o JSch precisa do Bouncy Castle (o do sistema é escondido do app).
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    testImplementation("junit:junit:4.13.2")
}
