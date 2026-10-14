pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
            name = "JetBrains Compose"
        }
    }
}

rootProject.name = "typewriter-bot"

include("shared")
include("gui")