import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { load(it) }
    }
}

android {
    namespace = "com.example.scriptbreak"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.scriptbreak"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject Firebase configuration safely from local.properties or environment variables
        // This keeps sensitive API keys private and out of version control
        val firebaseApiKey = localProperties.getProperty("firebase.apiKey")
            ?: System.getenv("FIREBASE_API_KEY")
            ?: ""
        val firebaseAppId = localProperties.getProperty("firebase.appId")
            ?: System.getenv("FIREBASE_APP_ID")
            ?: "1:363407252393:android:9f5a011387091017f102b8"
        val firebaseProjectId = localProperties.getProperty("firebase.projectId")
            ?: System.getenv("FIREBASE_PROJECT_ID")
            ?: "script-break-01"
        val firebaseSenderId = localProperties.getProperty("firebase.senderId")
            ?: System.getenv("FIREBASE_SENDER_ID")
            ?: "363407252393"
        val firebaseStorageBucket = localProperties.getProperty("firebase.storageBucket")
            ?: System.getenv("FIREBASE_STORAGE_BUCKET")
            ?: "script-break-01.firebasestorage.app"
        val firebaseDatabaseUrl = localProperties.getProperty("firebase.databaseUrl")
            ?: System.getenv("FIREBASE_DATABASE_URL")
            ?: "https://script-break-01-default-rtdb.asia-southeast1.firebasedatabase.app"

        resValue("string", "google_api_key", firebaseApiKey)
        resValue("string", "google_app_id", firebaseAppId)
        resValue("string", "project_id", firebaseProjectId)
        resValue("string", "gcm_defaultSenderId", firebaseSenderId)
        resValue("string", "google_storage_bucket", firebaseStorageBucket)
        resValue("string", "firebase_database_url", firebaseDatabaseUrl)
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Family alert to the trusted contact's phone (Realtime Database + anonymous sign-in, free plan)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    // "Pay from a QR image": reads the UPI QR code in a picked image (classic decoder, no AI model)
    implementation("com.google.zxing:core:3.5.3")
    // Live QR scanner: camera preview + frames (CameraX 1.4.1 has 16 KB page alignment for Android 15+)
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}