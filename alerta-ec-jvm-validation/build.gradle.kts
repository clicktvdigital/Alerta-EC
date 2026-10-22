import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    named("main") {
        kotlin {
            srcDir(file("../app/src/main/kotlin/org/breezyweather/alertaec"))
            include("AlertaEcEventIdentityV2.kt")
            include("AlertaEcEventIdentityCompatibility.kt")
            include("AlertaEcPhenomenon.kt")
        }
    }
    named("test") {
        kotlin {
            srcDir(file("../app/src/test/kotlin/org/breezyweather/alertaec"))
            include("AlertaEcEventIdentityV2Test.kt")
            include("AlertaEcEventIdentityCompatibilityTest.kt")
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("io.kotest:kotest-assertions-core:6.2.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "same_thread")
}
