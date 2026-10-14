package at.typewriter.bot

import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "config.json"
    val configFile = File(configPath)

    val config = if (configFile.exists()) {
        BotConfig.loadFromFile(configPath)
    } else {
        println("Config file not found at $configPath. Creating default config...")
        BotConfig.createDefault(configPath)
    }

    if (config.username.isBlank() || config.password.isBlank()) {
        println("Please edit $configPath and set your username and password.")
        println("Also configure lessonIds to specify which lessons to run.")
        return
    }

    println("""
        Typewriter Bot Configuration:
        Domain: ${config.domain}
        Username: ${config.username}
        Typing Speed: ${config.typingSpeedWpm} WPM
        Error Rate: ${(config.errorRate * 100).toInt()}%
        Headless: ${config.headless}
        Lessons: ${config.lessonIds.joinToString(", ")}
        Human-like Behavior: ${config.humanLikeBehavior}
    """.trimIndent())

    runBlocking {
        val bot = TypewriterBot(config)
        bot.start()
    }
}