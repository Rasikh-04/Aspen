plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "app.aspen.ui"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    val frameworkName = "Shared"
    // No iosX64: Compose Multiplatform doesn't publish iosX64 (Intel simulator).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkName
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core-design"))
            implementation(project(":shared:core-common"))
            implementation(project(":shared:domain"))
            implementation(compose.components.resources)
            implementation(libs.jetbrains.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.aspen.ui.generated.resources"
}
