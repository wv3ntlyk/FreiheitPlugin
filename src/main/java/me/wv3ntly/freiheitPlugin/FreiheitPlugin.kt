package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class FreiheitPlugin : JavaPlugin() {

    override fun onEnable() {
        logger.info("Freedom Plugin activated!")

        // Save the default configuration if not present
        saveDefaultConfig()

        val freedomManager = FreedomManager(this)

        // Заморожуємо рендеринг для світу
        val worldName = config.getString("world", "world")
        val world = worldName?.let { Bukkit.getWorld(it) }
        if (freedomManager.isBlueMapIntegrationEnabled() && world != null) {
            logger.info("Attempting to freeze BlueMap...")
            freedomManager.freezeBlueMap(worldName)
            logger.info("BlueMap successfully frozen for world: $worldName.")
        } else if (world == null) {
            logger.warning("World '$worldName' not found! Skipping BlueMap integration.")
        } else {
            logger.info("BlueMap integration is disabled in the configuration.")
        }

        // Initialize the world border from configuration
        freedomManager.initializeWorldBorder()
        logger.info("World border initialized successfully.")

        // Schedule automatic updates
        freedomManager.scheduleSmoothBorderUpdates()
        logger.info("Scheduled smooth border updates.")

        // Register commands
        registerCommands(freedomManager)
        logger.info("Commands registered successfully.")
    }

    override fun onDisable() {
        saveConfig() // Ensure all changes to the config are saved
        logger.info("Freedom Plugin deactivated!")
    }

    private fun registerCommands(freedomManager: FreedomManager) {
        val commands = mapOf(
            "freedom" to FreedomCommand(freedomManager)
        )

        commands.forEach { (name, executor) ->
            val command = getCommand(name)
            if (command != null) {
                command.setExecutor(executor)
                command.tabCompleter = FreedomTabCompleter()
            } else {
                logger.warning("Command '$name' is not defined in plugin.yml!")
            }
        }
    }
}
