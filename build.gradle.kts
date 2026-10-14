plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("org.graalvm.buildtools.native") version "0.10.4" apply false
}

group = "at.typewriter.bot"
version = "1.0.0"

repositories {
    mavenCentral()
}