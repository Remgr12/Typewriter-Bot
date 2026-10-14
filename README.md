# Typewriter Bot

A modern, configurable Kotlin bot for the Austrian Typewriter platform (edu.typewriter.at and other domains). Automates typing lessons with human-like behavior, configurable speed, error rates, and more.

## Features

- **Multi-domain support** - Works with `edu.typewriter.at`, `typewriter.at`, or any custom domain
- **Human-like typing** - Realistic delays, speed variation, and configurable error rates
- **Fully configurable** - JSON-based configuration with sensible defaults
- **Cross-platform** - Runs on Windows, Linux, and macOS (JVM-based)
- **Headless mode** - Run without GUI for servers/CI
- **Retry logic** - Automatic retries on transient failures
- **Lesson queuing** - Run multiple lessons sequentially
- **Modern Kotlin** - Coroutines, serialization, type-safe configuration
- **GUI Application** - Visual configuration and monitoring (Swing)
- **JVM JARs** - Universal Java archives for all platforms

## Quick Start

### Prerequisites

- Java 21+ (JDK) - for JAR version
- Google Chrome / Chromium installed

### Installation (JAR)

```bash
# Clone the repository
git clone https://github.com/Remgr12/Typewriter-Bot.git
cd Typewriter-Bot

# Build the project
./gradlew shadowJar

# Copy example config and edit it
cp config.json.example config.json
# Edit config.json with your credentials
```

### Configuration

Edit `config.json` with your settings:

```json
{
  "domain": "edu.typewriter.at",
  "username": "your_username",
  "password": "your_password",
  "typingSpeedWpm": 60,
  "errorRate": 0.02,
  "minDelayMs": 50,
  "maxDelayMs": 200,
  "headless": true,
  "lessonIds": [1, 2, 3],
  "autoStart": false,
  "pauseBetweenLessonsMs": 2000,
  "humanLikeBehavior": true,
  "randomizeSpeed": true,
  "randomizeErrors": true,
  "maxRetries": 3,
  "timeoutSeconds": 30
}
```

### Run CLI Version

```bash
# Using the built JAR
java -jar shared/build/libs/typewriter-bot-1.0.0.jar [config.json]

# Or directly with Gradle
./gradlew :shared:run --args="config.json"
```

### Run GUI Version

```bash
# Using the built JAR
java -jar gui/build/libs/typewriter-bot-gui-1.0.0.jar

# Or directly with Gradle
./gradlew :gui:run
```

## Download Pre-built Binaries

Pre-built binaries are available on the [Releases page](https://github.com/Remgr12/Typewriter-Bot/releases):

| Platform | CLI | GUI |
|----------|-----|-----|
| Windows | `typewriter-bot-1.0.0.jar` | `typewriter-bot-gui-1.0.0.jar` |
| Linux | `typewriter-bot-1.0.0.jar` | `typewriter-bot-gui-1.0.0.jar` |
| macOS | `typewriter-bot-1.0.0.jar` | `typewriter-bot-gui-1.0.0.jar` |

> **Note**: Native executables (`.exe`, flatpak) are currently experimental. See [Native Build Status](#native-build-status) below.

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `domain` | String | `"edu.typewriter.at"` | Typewriter platform domain |
| `username` | String | *required* | Your login username |
| `password` | String | *required* | Your login password |
| `typingSpeedWpm` | Int | `60` | Target typing speed in words per minute |
| `errorRate` | Double | `0.02` | Probability of making a typo (0.0-1.0) |
| `minDelayMs` | Int | `50` | Minimum delay between keystrokes |
| `maxDelayMs` | Int | `200` | Maximum delay between keystrokes |
| `headless` | Boolean | `true` | Run browser in headless mode |
| `lessonIds` | List<Int> | `[]` | Lesson IDs to run in sequence |
| `pauseBetweenLessonsMs` | Int | `2000` | Delay between lessons |
| `humanLikeBehavior` | Boolean | `true` | Enable realistic typing patterns |
| `randomizeSpeed` | Boolean | `true` | Vary typing speed slightly |
| `randomizeErrors` | Boolean | `true` | Randomize error positions |
| `maxRetries` | Int | `3` | Retry attempts per lesson |
| `timeoutSeconds` | Int | `30` | Selenium wait timeout |

## GUI Features

The GUI application provides:
- **Visual configuration** - All settings adjustable via sliders, inputs, and toggles
- **Real-time log** - Live activity feed with timestamps
- **One-click start** - Launch bot with current settings
- **Config persistence** - Auto-saves to `config.json`
- **Domain switcher** - Easy switching between `edu.typewriter.at`, `typewriter.at`, etc.

## Building from Source

### JVM JARs (All Platforms - Recommended)

```bash
./gradlew shadowJar
# Output: shared/build/libs/typewriter-bot-1.0.0.jar
#         gui/build/libs/typewriter-bot-gui-1.0.0.jar
```

### Native Executables (Experimental)

> **⚠️ Known Issue**: Native compilation with GraalVM currently fails due to a Kotlin/GraalVM compatibility issue with `kotlin.DeprecationLevel` class initialization. This is a known limitation in the GraalVM/Kotlin ecosystem.

```bash
# CLI native (currently failing)
./gradlew :shared:nativeCompile

# GUI native (currently failing)
./gradlew :gui:nativeCompile
```

### Flatpak (Linux - Experimental)

```bash
# Build JARs first
./gradlew shadowJar

# Install flatpak-builder and runtimes
sudo apt install flatpak-builder
flatpak install flathub org.freedesktop.Sdk//23.08 org.freedesktop.Platform//23.08

# Build flatpaks
cd flatpak/cli && flatpak-builder --user --install build-dir at.typewriter.Bot.yml
cd flatpak/gui && flatpak-builder --user --install build-dir at.typewriter.Bot.GUI.yml

# Create bundles
flatpak build-bundle --user repo-cli typewriter-bot.flatpak at.typewriter.Bot
flatpak build-bundle --user repo-gui typewriter-bot-gui.flatpak at.typewriter.Bot.GUI
```

## Native Build Status

| Platform | Status | Notes |
|----------|--------|-------|
| Linux (x86_64) | ❌ Failing | `kotlin.DeprecationLevel` init at build time |
| macOS (arm64) | ❌ Failing | Same Kotlin/GraalVM compatibility issue |
| Windows (x86_64) | ❌ Failing | Same Kotlin/GraalVM compatibility issue |

**Root Cause**: GraalVM's native-image analyzer initializes `kotlin.DeprecationLevel` at build time despite `--initialize-at-run-time=kotlin` configuration. This is a known GraalVM/Kotlin compatibility issue tracked in the GraalVM and Kotlin communities.

**Workaround**: Use the JVM JARs which work perfectly on all platforms with Java 21+.

## Project Structure

```
Typewriter-Bot/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Module inclusion
├── .github/workflows/            # CI/CD pipelines
├── shared/                       # Core bot logic (CLI)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/at/typewriter/bot/
│       │   ├── Config.kt         # Configuration data class
│       │   └── TypewriterBot.kt  # Core bot logic
│       ├── jvmMain/kotlin/at/typewriter/bot/
│       │   └── Main.kt           # CLI entry point
│       ├── main/resources/META-INF/native-image/...  # GraalVM config
│       └── commonMain/resources/
├── gui/                          # GUI application (Swing)
│   ├── build.gradle.kts
│   └── src/main/kotlin/at/typewriter/bot/gui/
│       ├── Main.kt               # GUI entry point
│       └── TypewriterBotGUI.kt   # Settings UI
├── flatpak/                      # Flatpak manifests
├── config.json.example           # Example configuration
└── LICENSE
```

## How It Works

1. **Login** - Navigates to login page, enters credentials, submits form
2. **Lesson Loading** - Opens each lesson URL, waits for text to load
3. **Text Extraction** - Reads lesson text via JavaScript (handles dynamic content)
4. **Typing Simulation** - Types character by character with:
   - Configurable base delay from WPM
   - Position-based fatigue simulation
   - Random variation (±30%)
   - Optional typo injection with backspace correction
5. **Submission** - Clicks submit/finish button
6. **Result Waiting** - Waits for completion confirmation

## Human-Like Behavior Details

- **Speed variation**: ±20% around target WPM when `randomizeSpeed` enabled
- **Fatigue simulation**: Gradual slowdown over lesson duration
- **Keystroke jitter**: 30-100% of base delay per character
- **Realistic errors**: Keyboard-neighbor typos with immediate correction
- **Anti-detection**: Disables `navigator.webdriver`, uses real user agent

## Supported Platforms

| Platform | JVM JAR | GUI |
|----------|---------|-----|
| Windows 10/11 | ✅ | ✅ |
| Linux (Ubuntu, Debian, Arch, etc.) | ✅ | ✅ |
| macOS (Intel & Apple Silicon) | ✅ | ✅ |

## Requirements

- Chrome/Chromium (auto-managed via WebDriverManager)
- Java 21+ runtime (for JAR)

## GitHub Actions CI/CD

Automated builds on every push/tag:
- **JVM JARs** - Ubuntu, Windows, macOS
- **Native builds** - Experimental (currently failing due to Kotlin/GraalVM issue)

Artifacts uploaded as release assets on tagged releases.

## Troubleshooting

### "Could not find lesson text element"
The site structure may have changed. Add custom selectors in `TypewriterBot.kt` or run with `headless: false` to inspect.

### Login fails
- Verify credentials in config
- Check if domain uses 2FA (not supported)
- Try `headless: false` to see what's happening

### ChromeDriver version mismatch
WebDriverManager handles this automatically. If issues persist:
```bash
# Clear cache
rm -rf ~/.cache/selenium
```

### GUI won't start on Linux
Ensure you have a display server (X11/Wayland) and required libraries:
```bash
sudo apt install libx11-6 libxext6 libxrender1 libxtst6 libxi6 libfreetype6
```

## Security Notes

- **Never commit `config.json`** - Contains credentials (gitignored)
- Use `config.local.json` for local overrides
- Consider using environment variables for production

## License

MIT License - see [LICENSE](LICENSE) for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a PR

## Acknowledgments

- Inspired by [CreatorT/Typewriter-Bot](https://github.com/CreatorT/Typewriter-Bot)
- Built with Kotlin, Selenium, Swing, Gradle