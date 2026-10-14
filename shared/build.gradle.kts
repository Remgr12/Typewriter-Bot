plugins {
    id("org.jetbrains.kotlin.jvm")
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
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.seleniumhq.selenium:selenium-java:4.23.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.23.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.9.2")
}

application {
    mainClass.set("at.typewriter.bot.MainKt")
}

sourceSets {
    val main by getting {
        kotlin.srcDir("src/commonMain/kotlin")
        kotlin.srcDir("src/jvmMain/kotlin")
        resources.srcDir("src/commonMain/resources")
        resources.srcDir("src/main/resources")
    }
}

graalvmNative {
    binaries {
        all {
            imageName.set("typewriter-bot")
            buildArgs.addAll(
                "--initialize-at-build-time=org.slf4j,ch.qos.logback",
                "--initialize-at-run-time=kotlin,org.openqa.selenium,io.github.bonigarcia",
                "--report-unsupported-elements-at-runtime",
                "-H:+ReportExceptionStackTraces",
                "--enable-monitoring=heapdump,jfr",
                "--no-fallback",
                "--link-at-build-time=kotlin"
            )
            systemProperties.put("webdriver.chrome.driver", "")
        }
    }
}

tasks {
    jar {
        enabled = true
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "at.typewriter.bot.MainKt")
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("typewriter-bot")
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "at.typewriter.bot.MainKt")
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}