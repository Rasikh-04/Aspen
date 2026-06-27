plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "app.aspen.domain"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // No iosX64: the Intel-Mac simulator target. Kept symmetric with the Compose
    // modules, which can't target iosX64. Apple-Silicon simulators use iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core-common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
