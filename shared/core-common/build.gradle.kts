plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "app.aspen.core.common"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // No iosX64: the Intel-Mac simulator target. Compose Multiplatform doesn't
    // publish iosX64 artifacts, and Apple-Silicon simulators use iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
