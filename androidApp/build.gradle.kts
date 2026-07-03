plugins {
    // AGP 9+ ships Kotlin built-in; applying org.jetbrains.kotlin.android is now a
    // hard error (see https://kotl.in/gradle/agp-built-in-kotlin). Kotlin compilation
    // is provided by com.android.application; the `kotlin {}` block below still works.
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.aspen.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.aspen.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:data"))
    implementation(project(":companion-overlay-android"))
    implementation(libs.androidx.work.runtime)
    implementation(compose.components.resources)
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    // Phase 6: transport for the optional account + server-routed AI (docs/00 #11). The entry
    // constructs the HttpClient; :shared:data stays engine-less (tests inject MockEngine).
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
}
