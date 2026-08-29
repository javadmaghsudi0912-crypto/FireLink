import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val props = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

fun prop(name: String) = props.getProperty(name, "")

android {
    namespace = "com.firelink.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.firelink.app"
        minSdk = 26
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"${prop("FIREBASE_API_KEY")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_APP_ID",
            "\"${prop("FIREBASE_APP_ID")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_DATABASE_URL",
            "\"${prop("FIREBASE_DATABASE_URL")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            "\"${prop("FIREBASE_PROJECT_ID")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {

    implementation(
        platform("androidx.compose:compose-bom:2025.02.00")
    )

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    implementation(
        platform("com.google.firebase:firebase-bom:33.9.0")
    )

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1"
    )
}
