package at.typewriter.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import io.github.bonigarcia.wdm.WebDriverManager
import java.time.Duration
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

class TypewriterBot(private val config: BotConfig) {
    private lateinit var driver: WebDriver
    private lateinit var wait: WebDriverWait
    private val random = Random()
    private val logger = org.slf4j.LoggerFactory.getLogger(TypewriterBot::class.java)

    fun start() {
        logger.info("Starting Typewriter Bot for domain: ${config.domain}")
        setupDriver()
        try {
            login()
            if (config.lessonIds.isNotEmpty()) {
                runLessons()
            } else {
                logger.warn("No lesson IDs configured. Exiting.")
            }
        } catch (e: Exception) {
            logger.error("Bot encountered an error: ${e.message}", e)
            throw e
        } finally {
            cleanup()
        }
    }

    private fun setupDriver() {
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions().apply {
            if (config.headless) {
                addArguments("--headless=new")
            }
            addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu")
            addArguments("--window-size=1920,1080")
            addArguments("--disable-blink-features=AutomationControlled")
            addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            setExperimentalOption("excludeSwitches", listOf("enable-automation"))
            setExperimentalOption("useAutomationExtension", false)
        }
        driver = ChromeDriver(options)
        wait = WebDriverWait(driver, Duration.ofSeconds(config.timeoutSeconds.toLong()))

        (driver as JavascriptExecutor).executeScript(
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        )
    }

    private fun login() {
        logger.info("Navigating to login page: ${config.loginUrl}")
        driver.get(config.loginUrl)

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")))

        val usernameField = driver.findElement(By.name("username"))
        val passwordField = driver.findElement(By.name("password"))
        val loginButton = driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']"))

        humanType(usernameField, config.username)
        humanDelay()
        humanType(passwordField, config.password)
        humanDelay()

        loginButton.click()
        wait.until(ExpectedConditions.urlContains("/dashboard"))

        logger.info("Login successful")
    }

    private fun runLessons() {
        for (lessonId in config.lessonIds) {
            var retryCount = 0
            while (retryCount < config.maxRetries) {
                try {
                    runLesson(lessonId)
                    break
                } catch (e: Exception) {
                    retryCount++
                    logger.warn("Lesson $lessonId failed (attempt $retryCount/${config.maxRetries}): ${e.message}")
                    if (retryCount >= config.maxRetries) {
                        logger.error("Lesson $lessonId failed after ${config.maxRetries} attempts")
                        throw e
                    }
                    Thread.sleep(2000)
                }
            }
            if (lessonId != config.lessonIds.last()) {
                logger.info("Pausing ${config.pauseBetweenLessonsMs}ms before next lesson")
                Thread.sleep(config.pauseBetweenLessonsMs.toLong())
            }
        }
        logger.info("All lessons completed!")
    }

    private fun runLesson(lessonId: Int) {
        val lessonUrl = "${config.lessonsUrl}/$lessonId"
        logger.info("Starting lesson $lessonId: $lessonUrl")
        driver.get(lessonUrl)

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".lesson-text, [data-lesson-text], .text-to-type")))

        val textElement = findTextElement()
        val textToType = extractText(textElement)
        logger.info("Lesson text length: ${textToType.length} characters")

        val inputField = findInputField()
        inputField.click()

        typeText(inputField, textToType)

        val submitButton = findSubmitButton()
        if (submitButton != null) {
            humanDelay()
            submitButton.click()
            logger.info("Lesson $lessonId submitted")
        }

        waitForResult()
    }

    private fun findTextElement(): WebElement {
        val selectors = listOf(
            ".lesson-text",
            "[data-lesson-text]",
            ".text-to-type",
            ".lesson-content .text",
            "#lesson-text",
            ".exercise-text"
        )
        for (selector in selectors) {
            try {
                return wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)))
            } catch (e: Exception) {
                continue
            }
        }
        throw RuntimeException("Could not find lesson text element")
    }

    private fun extractText(element: WebElement): String {
        return (driver as JavascriptExecutor).executeScript(
            "return arguments[0].innerText || arguments[0].textContent", element
        ) as String
    }

    private fun findInputField(): WebElement {
        val selectors = listOf(
            "input[type='text']",
            "textarea",
            ".typewriter-input",
            "[data-input-field]",
            "#type-input",
            ".lesson-input"
        )
        for (selector in selectors) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)))
            } catch (e: Exception) {
                continue
            }
        }
        throw RuntimeException("Could not find input field")
    }

    private fun findSubmitButton(): WebElement? {
        val selectors = listOf(
            "button[type='submit']",
            ".submit-btn",
            "#submit-lesson",
            "button:contains('Submit')",
            "button:contains('Finish')"
        )
        for (selector in selectors) {
            try {
                return driver.findElement(By.cssSelector(selector))
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun waitForResult() {
        Thread.sleep(1000)
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".result, .lesson-complete, .success")),
                ExpectedConditions.urlContains("/lessons")
            ))
        } catch (e: Exception) {
            logger.debug("Result wait timeout, continuing...")
        }
    }

    private fun typeText(inputField: WebElement, text: String) {
        val chars = text.toCharArray()
        val baseDelay = calculateBaseDelay()

        for (i in chars.indices) {
            val char = chars[i]
            val shouldError = config.randomizeErrors && random.nextDouble() < config.errorRate

            if (shouldError && char.isLetterOrDigit()) {
                val wrongChar = generateWrongChar(char)
                inputField.sendKeys(wrongChar.toString())
                humanDelay(baseDelay * 2)
                inputField.sendKeys(org.openqa.selenium.Keys.BACK_SPACE)
                humanDelay(baseDelay)
            }

            inputField.sendKeys(char.toString())
            humanDelay(calculateCharDelay(baseDelay, i, chars.size))
        }
    }

    private fun calculateBaseDelay(): Long {
        val wpm = if (config.randomizeSpeed) {
            (config.typingSpeedWpm * (0.8 + random.nextDouble() * 0.4)).toInt()
        } else {
            config.typingSpeedWpm
        }
        return (60000.0 / (wpm * 5)).toLong()
    }

    private fun calculateCharDelay(baseDelay: Long, index: Int, total: Int): Long {
        var delay = baseDelay
        if (config.humanLikeBehavior) {
            val positionFactor = 1.0 + (index.toDouble() / total) * 0.3
            val variation = 0.7 + random.nextDouble() * 0.6
            delay = (delay * positionFactor * variation).toLong()
        }
        return delay.coerceIn(config.minDelayMs.toLong(), config.maxDelayMs.toLong())
    }

    private fun humanType(element: WebElement, text: String) {
        for (char in text) {
            element.sendKeys(char.toString())
            Thread.sleep(ThreadLocalRandom.current().nextLong(80, 180))
        }
    }

    private fun humanDelay(baseDelay: Long = 100) {
        val delay = if (config.humanLikeBehavior) {
            (baseDelay * (0.5 + random.nextDouble())).toLong()
        } else {
            baseDelay
        }
        Thread.sleep(delay.coerceIn(config.minDelayMs.toLong(), config.maxDelayMs.toLong()))
    }

    private fun generateWrongChar(original: Char): Char {
        val neighbors = mapOf<Char, List<Char>>(
            'a' to listOf('s', 'q', 'z'), 'b' to listOf('v', 'g', 'n'), 'c' to listOf('x', 'd', 'f', 'v'), 'd' to listOf('s', 'e', 'r', 'f', 'c', 'x'),
            'e' to listOf('w', 'r', 'd'), 'f' to listOf('d', 'r', 't', 'g', 'c', 'v'), 'g' to listOf('f', 't', 'y', 'h', 'v', 'b'), 'h' to listOf('g', 'y', 'u', 'j', 'n', 'b'),
            'i' to listOf('u', 'j', 'k', 'o'), 'j' to listOf('h', 'u', 'i', 'k', 'm', 'n'), 'k' to listOf('j', 'i', 'o', 'l', 'm'), 'l' to listOf('k', 'o', 'p'),
            'm' to listOf('n', 'j', 'k'), 'n' to listOf('b', 'h', 'j', 'm'), 'o' to listOf('i', 'k', 'l', 'p'), 'p' to listOf('o', 'l'),
            'q' to listOf('w', 'a'), 'r' to listOf('e', 't', 'd', 'f'), 's' to listOf('a', 'z', 'x', 'd', 'e', 'w', 'q'), 't' to listOf('r', 'f', 'g', 'y', 'h'),
            'u' to listOf('y', 'h', 'j', 'i'), 'v' to listOf('c', 'f', 'g', 'b'), 'w' to listOf('q', 'a', 's', 'e'), 'x' to listOf('z', 's', 'd', 'c'),
            'y' to listOf('t', 'u', 'g', 'h'), 'z' to listOf('a', 's', 'x'),
            '1' to listOf('2'), '2' to listOf('1', '3'), '3' to listOf('2', '4'), '4' to listOf('3', '5'),
            '5' to listOf('4', '6'), '6' to listOf('5', '7'), '7' to listOf('6', '8'), '8' to listOf('7', '9'),
            '9' to listOf('8', '0'), '0' to listOf('9')
        )
        val lower: Char = original.toLowerCase()
        val neighborsList: List<Char>? = neighbors[lower]
        if (neighborsList == null || neighborsList.isEmpty()) return original
        val wrongChar: Char = neighborsList.random()
        return if (original.isUpperCase()) wrongChar.uppercaseChar() else wrongChar
    }

    private fun cleanup() {
        try {
            driver.quit()
        } catch (e: Exception) {
            logger.warn("Error during cleanup: ${e.message}")
        }
    }
}