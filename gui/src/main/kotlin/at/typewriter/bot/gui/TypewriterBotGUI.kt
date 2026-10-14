package at.typewriter.bot.gui

import at.typewriter.bot.BotConfig
import at.typewriter.bot.TypewriterBot
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.io.FileWriter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.UIManager
import javax.swing.border.TitledBorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class TypewriterBotGUI : JFrame("Typewriter Bot - Austrian Typewriter Platform") {
    private var config = BotConfig.loadFromFile("config.json")
    private var botJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val logArea = JTextArea()
    private var isRunning = false

    // UI Components
    private val domainField = JTextField(config.domain, 30)
    private val usernameField = JTextField(config.username, 30)
    private val passwordField = JPasswordField(config.password, 30)
    private val wpmSpinner = JSpinner(SpinnerNumberModel(config.typingSpeedWpm, 10, 300, 5))
    private val errorRateSlider = JSlider(0, 100, (config.errorRate * 100).toInt())
    private val minDelaySpinner = JSpinner(SpinnerNumberModel(config.minDelayMs, 10, 500, 10))
    private val maxDelaySpinner = JSpinner(SpinnerNumberModel(config.maxDelayMs, 50, 1000, 10))
    private val pauseSpinner = JSpinner(SpinnerNumberModel(config.pauseBetweenLessonsMs, 500, 10000, 100))
    private val retriesSpinner = JSpinner(SpinnerNumberModel(config.maxRetries, 1, 10, 1))
    private val timeoutSpinner = JSpinner(SpinnerNumberModel(config.timeoutSeconds, 10, 120, 1))
    private val headlessCheck = JCheckBox("Headless Mode", config.headless as Boolean)
    private val humanLikeCheck = JCheckBox("Human-like Behavior", config.humanLikeBehavior as Boolean)
    private val randomizeSpeedCheck = JCheckBox("Randomize Speed", config.randomizeSpeed as Boolean)
    private val randomizeErrorsCheck = JCheckBox("Randomize Errors", config.randomizeErrors as Boolean)
    private val lessonIdsField = JTextField(config.lessonIds.joinToString(", "), 40)
    private val startButton = JButton("Start Bot")
    private val saveButton = JButton("Save Config")
    private val clearLogButton = JButton("Clear Log")
    private val openBrowserButton = JButton("Open in Browser")

    init {
        setupLookAndFeel()
        setupUI()
        setupActions()
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setMinimumSize(Dimension(900, 700))
        setLocationRelativeTo(null)
    }

    private fun setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            // Use default
        }
    }

    private fun setupUI() {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Top toolbar
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        toolbar.add(saveButton)
        toolbar.add(openBrowserButton)
        mainPanel.add(toolbar, BorderLayout.NORTH)

        // Tabbed pane for settings
        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("Connection", createConnectionPanel())
        tabbedPane.addTab("Typing Behavior", createTypingPanel())
        tabbedPane.addTab("Lessons", createLessonsPanel())
        tabbedPane.addTab("Browser", createBrowserPanel())

        // Log area
        logArea.isEditable = false
        logArea.font = java.awt.Font(Font.MONOSPACED, Font.PLAIN, 12)
        val logScroll = JScrollPane(logArea)
        logScroll.border = BorderFactory.createTitledBorder("Activity Log")
        logScroll.preferredSize = Dimension(800, 200)

        val logToolbar = JPanel(FlowLayout(FlowLayout.RIGHT))
        logToolbar.add(clearLogButton)
        val logPanel = JPanel(BorderLayout())
        logPanel.add(logToolbar, BorderLayout.NORTH)
        logPanel.add(logScroll, BorderLayout.CENTER)

        // Split pane
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, logPanel)
        splitPane.resizeWeight = 0.7
        splitPane.dividerLocation = 450

        mainPanel.add(splitPane, BorderLayout.CENTER)

        // Bottom panel with start button
        val bottomPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        startButton.preferredSize = Dimension(200, 48)
        startButton.font = java.awt.Font(Font.SANS_SERIF, Font.BOLD, 16)
        bottomPanel.add(startButton)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        contentPane.add(mainPanel)
    }

    private fun createConnectionPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(createLabeledField("Domain:", domainField, "e.g., edu.typewriter.at"))
        panel.add(Box.createVerticalStrut(10))
        panel.add(createLabeledField("Username:", usernameField, "Your login username"))
        panel.add(Box.createVerticalStrut(10))
        panel.add(createLabeledField("Password:", passwordField, "Your login password"))
        panel.add(Box.createVerticalStrut(10))

        val hint = JLabel("<html><i>Supports any Typewriter domain (edu.typewriter.at, typewriter.at, etc.)</i></html>")
        hint.font = java.awt.Font(Font.SANS_SERIF, Font.ITALIC, 11)
        panel.add(hint)

        return panel
    }

    private fun createTypingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        var row = 0
        fun addRow(label: String, component: JComponent, tooltip: String = "") {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
            val lbl = JLabel(label)
            lbl.toolTipText = tooltip
            panel.add(lbl, gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            component.toolTipText = tooltip
            panel.add(component, gbc)
            row++
        }

        addRow("Typing Speed (WPM):", wpmSpinner, "Target words per minute (20-200)")
        (wpmSpinner.model as SpinnerNumberModel).minimum = 10
        (wpmSpinner.model as SpinnerNumberModel).maximum = 300
        (wpmSpinner.model as SpinnerNumberModel).stepSize = 5.0

        addRow("Error Rate:", errorRateSlider, "Percentage of typos (0-10%)")
        errorRateSlider.majorTickSpacing = 1
        errorRateSlider.minorTickSpacing = 1
        errorRateSlider.paintTicks = true
        errorRateSlider.paintLabels = true
        errorRateSlider.setLabelTable(errorRateSlider.createStandardLabels(1))

        addRow("Min Delay (ms):", minDelaySpinner, "Minimum keystroke delay")
        (minDelaySpinner.model as SpinnerNumberModel).minimum = 10
        (minDelaySpinner.model as SpinnerNumberModel).maximum = 500

        addRow("Max Delay (ms):", maxDelaySpinner, "Maximum keystroke delay")
        (maxDelaySpinner.model as SpinnerNumberModel).minimum = 50
        (maxDelaySpinner.model as SpinnerNumberModel).maximum = 1000

        addRow("", humanLikeCheck, "Enable realistic typing patterns (fatigue, variation)")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        panel.add(humanLikeCheck, gbc)
        row++; gbc.gridwidth = 1

        addRow("", randomizeSpeedCheck, "Vary typing speed ±20%")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        panel.add(randomizeSpeedCheck, gbc)
        row++; gbc.gridwidth = 1

        addRow("", randomizeErrorsCheck, "Randomize error positions")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        panel.add(randomizeErrorsCheck, gbc)

        return panel
    }

    private fun createLessonsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(createLabeledField("Lesson IDs (comma-separated):", lessonIdsField, "e.g., 1, 2, 3, 4"))
        panel.add(Box.createVerticalStrut(10))

        val hint = JLabel("<html><i>Find lesson IDs in the URL when viewing lessons on typewriter.at<br/>Example: https://edu.typewriter.at/lessons/42 → ID is 42</i></html>")
        hint.font = java.awt.Font(Font.SANS_SERIF, Font.ITALIC, 11)
        panel.add(hint)
        panel.add(Box.createVerticalStrut(20))

        panel.add(createLabeledField("Pause Between Lessons (ms):", pauseSpinner))
        (pauseSpinner.model as SpinnerNumberModel).minimum = 500
        (pauseSpinner.model as SpinnerNumberModel).maximum = 10000
        panel.add(Box.createVerticalStrut(10))

        panel.add(createLabeledField("Max Retries:", retriesSpinner))
        (retriesSpinner.model as SpinnerNumberModel).minimum = 1
        (retriesSpinner.model as SpinnerNumberModel).maximum = 10
        panel.add(Box.createVerticalStrut(10))

        panel.add(createLabeledField("Timeout (seconds):", timeoutSpinner))
        (timeoutSpinner.model as SpinnerNumberModel).minimum = 10
        (timeoutSpinner.model as SpinnerNumberModel).maximum = 120

        return panel
    }

    private fun createBrowserPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(headlessCheck)
        headlessCheck.toolTipText = "Run browser in background (faster, no window visible)"

        return panel
    }

    private fun createLabeledField(label: String, field: JComponent, tooltip: String = ""): JPanel {
        val panel = JPanel(BorderLayout(10, 0))
        val lbl = JLabel(label)
        lbl.preferredSize = Dimension(200, 30)
        lbl.toolTipText = tooltip
        field.toolTipText = tooltip
        panel.add(lbl, BorderLayout.WEST)
        panel.add(field, BorderLayout.CENTER)
        return panel
    }

    private fun setupActions() {
        saveButton.addActionListener { saveConfig() }
        clearLogButton.addActionListener { logArea.text = "" }
        openBrowserButton.addActionListener { openBrowser() }
        startButton.addActionListener { toggleBot() }

        // Update config from UI fields
        domainField.addActionListener { updateConfigFromUI() }
        usernameField.addActionListener { updateConfigFromUI() }
        passwordField.addActionListener { updateConfigFromUI() }
        wpmSpinner.addChangeListener { updateConfigFromUI() }
        errorRateSlider.addChangeListener { updateConfigFromUI() }
        minDelaySpinner.addChangeListener { updateConfigFromUI() }
        maxDelaySpinner.addChangeListener { updateConfigFromUI() }
        pauseSpinner.addChangeListener { updateConfigFromUI() }
        retriesSpinner.addChangeListener { updateConfigFromUI() }
        timeoutSpinner.addChangeListener { updateConfigFromUI() }
        headlessCheck.addActionListener { updateConfigFromUI() }
        humanLikeCheck.addActionListener { updateConfigFromUI() }
        randomizeSpeedCheck.addActionListener { updateConfigFromUI() }
        randomizeErrorsCheck.addActionListener { updateConfigFromUI() }
        lessonIdsField.addActionListener { updateConfigFromUI() }
    }

    private fun updateConfigFromUI() {
        config = config.copy(
            domain = domainField.text.trim(),
            username = usernameField.text.trim(),
            password = String(passwordField.password),
            typingSpeedWpm = wpmSpinner.value as Int,
            errorRate = errorRateSlider.value.toDouble() / 100.0,
            minDelayMs = minDelaySpinner.value as Int,
            maxDelayMs = maxDelaySpinner.value as Int,
            headless = headlessCheck.isSelected,
            lessonIds = lessonIdsField.text.split(",").map { it.trim() }.filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() },
            pauseBetweenLessonsMs = pauseSpinner.value as Int,
            humanLikeBehavior = humanLikeCheck.isSelected,
            randomizeSpeed = randomizeSpeedCheck.isSelected,
            randomizeErrors = randomizeErrorsCheck.isSelected,
            maxRetries = retriesSpinner.value as Int,
            timeoutSeconds = timeoutSpinner.value as Int
        )
    }

    private fun saveConfig() {
        updateConfigFromUI()
        try {
            config.saveToFile("config.json")
            addLog("Configuration saved to config.json")
            JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            addLog("Error saving config: ${e.message}")
            JOptionPane.showMessageDialog(this, "Error saving config: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun openBrowser() {
        updateConfigFromUI()
        try {
            Desktop.getDesktop().browse(java.net.URI(config.baseUrl))
            addLog("Opened ${config.baseUrl} in browser")
        } catch (e: Exception) {
            addLog("Error opening browser: ${e.message}")
        }
    }

    private fun toggleBot() {
        if (isRunning) {
            botJob?.cancel()
            isRunning = false
            updateStartButton()
            addLog("Bot stopped by user")
        } else {
            updateConfigFromUI()
            if (config.username.isBlank() || config.password.isBlank()) {
                JOptionPane.showMessageDialog(this, "Username and password are required!", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }
            if (config.lessonIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No lesson IDs configured!", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }

            isRunning = true
            updateStartButton()
            addLog("Starting bot for domain: ${config.domain}")
            addLog("Lessons: ${config.lessonIds.joinToString(", ")}")
            addLog("Speed: ${config.typingSpeedWpm} WPM, Error rate: ${(config.errorRate * 100).toInt()}%")

            botJob = scope.launch {
                try {
                    val bot = TypewriterBot(config)
                    bot.start()
                    SwingUtilities.invokeLater { addLog("All lessons completed successfully!") }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater { addLog("ERROR: ${e.message}") }
                } finally {
                    SwingUtilities.invokeLater {
                        isRunning = false
                        updateStartButton()
                    }
                }
            }
        }
    }

    private fun updateStartButton() {
        SwingUtilities.invokeLater {
            if (isRunning) {
                startButton.text = "Stop Bot"
                startButton.background = java.awt.Color(220, 50, 50)
            } else {
                startButton.text = "Start Bot"
                startButton.background = null
            }
        }
    }

    private fun addLog(message: String) {
        SwingUtilities.invokeLater {
            val timestamp = LocalTime.now().format(timeFormatter)
            logArea.append("[$timestamp] $message\n")
            logArea.caretPosition = logArea.document.length
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                TypewriterBotGUI().isVisible = true
            }
        }
    }
}