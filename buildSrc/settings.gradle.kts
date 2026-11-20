// Maven Central first: plugins.gradle.org intermittently failed to serve the kotlin-stdlib / kotlin-gradle-plugin
// POMs for buildSrc in CI (2026-11-20), aborting the build in ~20 s before any code compiled.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}