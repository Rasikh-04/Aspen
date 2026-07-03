plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "app.aspen.data"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // No iosX64: kept symmetric with the other shared modules (Apple-Silicon simulators use
    // iosSimulatorArm64). The crypto-backed ConsentStore actuals live in iosMain.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:core-common"))
            // Aspen server wire contract (Phase 6) — same DTO module the :server routes use.
            implementation(project(":shared:server-api"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            // Tier-2 cloud client transport (docs/04 ADR-003). Engine-less on purpose: no live
            // endpoint is wired (DisabledAiClient is the DI default); tests inject MockEngine.
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            // Tier-1 companion ranker (docs/04 ADR-003): small text-EMBEDDER model (select/personalise
            // from the curated library, never generate). The model asset is optional; absence falls
            // back to deterministic selection.
            implementation(libs.mediapipe.tasks.text)
        }
        jvmTest.dependencies {
            // Parity test reads the canonical crisis JSON from the repo and asserts it matches the
            // in-code registry (single source of truth, no drift — same pattern as the token lexicon).
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
