plugins {
    // AGP 9+ built-in Kotlin (same note as :androidApp — do not apply org.jetbrains.kotlin.android).
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.aspen.companion.overlay"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared:ui")) // sprites + CompanionController
    implementation(project(":shared:domain")) // behaviour machine + prefs models
    implementation(project(":shared:data")) // encrypted prefs store + AspenLocalStorage
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.savedstate)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
