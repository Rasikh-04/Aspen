plugins {
    `kotlin-dsl`
}

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
            excludeGroup("org.jetbrains.kotlin")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
