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

    // Secrets Gradle Plugin
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
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

        // Fix: Use localProperties to ensure the manifest gets the key
        manifestPlaceholders["MAPS_API_KEY"] = localProperties["MAPS_API_KEY"] ?: ""

        // Google Maps KEY
        buildConfigField(
            "String",
            "MAPS_API_KEY",
            "\"${localProperties["MAPS_API_KEY"] ?: ""}\""
            )


        // Supabase URL Key.
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties["SUPABASE_URL"] ?: ""}\""
        )

        // Supabase ANON Key.
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Migrating from kotlinOptions to compilerOptions
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// For Secrets Gradle Plugin.
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"

    // Make secrets available in AndroidManifest.xml as ${MAPS_API_KEY}
    // AND in Kotlin code as BuildConfig.MAPS_API_KEY
    ignoreList.add("sdk.*") // Don't expose sdk.dir as a BuildConfig field
    ignoreList.add("keyAlias") // Ignore signing config keys if present
    ignoreList.add("keyPassword")
    ignoreList.add("storeFile")
    ignoreList.add("storePassword")
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
    implementation(libs.androidx.compose.runtime)
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

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // App Navigation
    val navVersion = "2.8.9"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    // Nav with Fragments
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.7")

    // Maps
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // Retrofit for Directions/Routes API
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Maps Compose
    implementation("com.google.maps.android:maps-compose:8.2.2")


}
