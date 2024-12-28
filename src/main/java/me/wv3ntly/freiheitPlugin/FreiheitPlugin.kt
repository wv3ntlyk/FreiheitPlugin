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
        if (freedomManager.isBlueMapIntegrationEnabled()) {
            logger.info("Attempting to freeze BlueMap...")
            if (worldName != null) {
                freedomManager.freezeBlueMap(worldName)
            }
        } else {
            logger.info("BlueMap integration is disabled in the configuration.")
        }

        // Initialize the world border from configuration
        freedomManager.initializeWorldBorder()

        // Schedule automatic updates
        freedomManager.scheduleSmoothBorderUpdates()

        // Register commands
        registerCommands(freedomManager)


    }


    override fun onDisable() {
        saveConfig() // Ensure all changes to the config are saved
        logger.info("Freedom Plugin deactivated!")
    }

    private fun registerCommands(freedomManager: FreedomManager) {
        val command = getCommand("freedom")
        if (command != null) {
            command.setExecutor(FreedomCommand(freedomManager))
            command.tabCompleter = FreedomTabCompleter() // Add tab completer here
        }
    }
}