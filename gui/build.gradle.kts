plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow")
    id("application")
    id("org.graalvm.buildtools.native") version "0.10.4"
}

group = "at.typewriter.bot"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application {
    mainClass.set("at.typewriter.bot.gui.MainKt")
}

graalvmNative {
    binaries {
        all {
            imageName.set("typewriter-bot-gui")
            buildArgs.addAll(
                "--enable-all-security-services",
                "--initialize-at-build-time=org.slf4j,ch.qos.logback,java.awt,javax.swing",
                "--initialize-at-run-time=org.openqa.selenium,io.github.bonigarcia",
                "--report-unsupported-elements-at-runtime",
                "-H:+ReportExceptionStackTraces",
                "-H:+AllowVMInspection",
                "--no-fallback"
            )
        }
    }
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("typewriter-bot-gui")
        mergeServiceFiles()
        manifest {
            attributes("Main-Class" to "at.typewriter.bot.gui.MainKt")
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}