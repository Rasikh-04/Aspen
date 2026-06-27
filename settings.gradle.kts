@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal {
            content {
                // plugins.gradle.org's CDN can flake on large jars; always pull Kotlin artifacts
                // (e.g. kotlin-gradle-plugin, a transitive of the AGP KMP plugin) from Maven Central.
                excludeGroup("org.jetbrains.kotlin")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aspen"

include(":androidApp")
include(":shared:core-common")
include(":shared:core-design")
include(":shared:domain")
include(":shared:ui")
