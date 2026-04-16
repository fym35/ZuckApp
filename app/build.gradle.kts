plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "eu.konggdev.zuckapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "eu.konggdev.zuckapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    ndkVersion = ndkVersion
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(kotlin("script-runtime"))
    implementation("org.jetbrains.kotlin:kotlin-script-runtime")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation ("org.whispersystems:curve25519-android:0.5.0")
    implementation ("com.google.guava:guava:32.0.1-jre")
    implementation ("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation ("com.goterl:lazysodium-android:5.1.0@aar")
    implementation ("net.java.dev.jna:jna:5.8.0@aar")
    implementation ("com.github.auties00:curve25519:1.2")
}