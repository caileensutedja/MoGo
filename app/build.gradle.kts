// To allow for Auth set up
// Exposes these dependencies to local.properties via BuildConfig.
import java.util.Properties

val localProperties = Properties()
val localPropsFile = File(rootProject.rootDir, "local.properties")
if (localPropsFile.exists() && localPropsFile.isFile) {
    localPropsFile.inputStream().use {
        localProperties.load(it)
    }
}

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // For DB (Supabase) set up.
    // Current Kotlin version used is based on libs.versions.toml file (17/03/2026).
    // Serialization is for encoding and decoding custom objects. -Kotlin ref v3.0 supabase.com
    kotlin("plugin.serialization") version "2.3.10" // Replace with actual version in project.
}

// IDE says the syntax is deprecated. Check for newer version (if documentation exists).
android {
    namespace = "com.fit3161.fit3162.mogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fit3161.fit3162.mogo"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase
//        buildConfigField("String", "SUPABASE_URL",
//            localProperties.getProperty("SUPABASE_URL")!!)

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")

        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    kotlinOptions { // Also deprecated?
//        jvmTarget = "11"
//    }

    buildFeatures {
        buildConfig = true
        compose = true
//        resValues = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.4.1"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt") // PostgresSQL DB
    implementation("io.github.jan-tennert.supabase:auth-kt") // DB authentication
    implementation("io.github.jan-tennert.supabase:realtime-kt") // Realtime DB
    implementation("io.ktor:ktor-client-android:3.4.1") // Ktor engine (required for Supabase Kotlin SDK)
//    implementation("io.ktor:ktor-client-okhttp:3.4.1")


    // KTX dependencies for coroutines (for LifecycleScope, ViewModelScope, liveData).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0") // for kotlin coroutine??

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

}