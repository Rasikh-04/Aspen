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
