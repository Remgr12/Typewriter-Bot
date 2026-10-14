package at.typewriter.bot

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val domain: String = "edu.typewriter.at",
    val username: String = "",
    val password: String = "",
    val typingSpeedWpm: Int = 60,
    val errorRate: Double = 0.02,
    val minDelayMs: Int = 50,
    val maxDelayMs: Int = 200,
    val headless: Boolean = true,
    val lessonIds: List<Int> = emptyList(),
    val autoStart: Boolean = false,
    val pauseBetweenLessonsMs: Int = 2000,
    val humanLikeBehavior: Boolean = true,
    val randomizeSpeed: Boolean = true,
    val randomizeErrors: Boolean = true,
    val maxRetries: Int = 3,
    val timeoutSeconds: Int = 30,
) {
    val baseUrl: String
        get() = "https://$domain"

    val loginUrl: String
        get() = "$baseUrl/login"

    val lessonsUrl: String
        get() = "$baseUrl/lessons"

    companion object {
        fun loadFromFile(path: String): BotConfig {
            return try {
                val json = java.io.File(path).readText()
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(BotConfig.serializer(), json)
            } catch (e: Exception) {
                BotConfig()
            }
        }

        fun createDefault(path: String): BotConfig {
            val config = BotConfig()
            config.saveToFile(path)
            return config
        }
    }

    fun saveToFile(path: String) {
        val json = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }.encodeToString(BotConfig.serializer(), this)
        java.io.File(path).writeText(json)
    }
}