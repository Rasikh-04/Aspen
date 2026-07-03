plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Pin bytecode to the project's JVM 17 standard: the Gradle daemon may run a newer JDK, and
    // the :server toolchain (17) must be able to load these classes.
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    androidLibrary {
        namespace = "app.aspen.api"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // No iosX64: the Intel-Mac simulator target. Compose Multiplatform doesn't
    // publish iosX64 artifacts, and Apple-Silicon simulators use iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
