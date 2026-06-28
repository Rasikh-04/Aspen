plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "app.aspen.design"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        // The new AGP KMP library plugin makes Android resources opt-in (off by
        // default), unlike com.android.library. Compose Multiplatform packs its
        // composeResources into Android assets, so without this the assets never
        // enter the AAR/APK and stringResource() crashes at first composition with
        // MissingResourceException. See JetBrains CMP-9547.
        androidResources {
            enable = true
        }
    }

    // No iosX64: the Intel-Mac simulator target. Compose Multiplatform doesn't
    // publish iosX64 artifacts, and Apple-Silicon simulators use iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core-common"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.aspen.design.generated.resources"
}
