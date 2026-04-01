import java.util.Properties

// Load local.properties
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Supabase uses Kotlin serialization
    kotlin("plugin.serialization") version "2.2.10"
}

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
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties["SUPABASE_URL"] ?: ""}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties["SUPABASE_ANON_KEY"] ?: ""}\""
        )
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
    // Deprecated (20/03/2026)
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val supabaseVersion = "3.4.1"
val ktorVersion = "3.4.1"

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.benchmark.traceprocessor.android)
    implementation(libs.androidx.navigation.compose)

//    implementation(libs.androidx.benchmark.traceprocessor.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Material Compose
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Supabase dependencies
    implementation(platform("io.github.jan-tennert.supabase:bom:${supabaseVersion}"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt") // PostgresSQL
    implementation("io.github.jan-tennert.supabase:auth-kt") // Authentication
    implementation("io.github.jan-tennert.supabase:realtime-kt") // Realtime DB

    // Ktor (for Supabase)
    implementation("io.ktor:ktor-client-android:${ktorVersion}") // Supabase HTTP Engine

    // ViewModel, LiveData dependencies (MVVM architecture)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // App Navigation
    val nav_version = "2.8.9"
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Session Persistance
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}