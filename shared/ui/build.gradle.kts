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

        // The new AGP KMP library plugin makes Android resources opt-in (off by
        // default), unlike com.android.library. Compose Multiplatform packs its
        // composeResources (strings.*.cvr) into Android assets, so without this the
        // assets never enter the AAR/APK and stringResource() crashes at first
        // composition with MissingResourceException. See JetBrains CMP-9547.
        androidResources {
            enable = true
        }

        // Run commonTest as JVM-hosted Android unit tests (no device/emulator) so Dev B's UI/state
        // logic is verifiable on Linux/Windows, like the other modules' jvmTest. iOS test execution
        // stays a macOS-CI concern (docs/13 §1).
        withHostTestBuilder {}
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
            implementation(libs.phosphor.icon.compose)
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
