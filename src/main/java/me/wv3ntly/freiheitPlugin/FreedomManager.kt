package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File

class FreedomManager(private val plugin: JavaPlugin) {
    private object DefaultConfig {
        const val WORLD = "world"
        const val CENTER_X = 0.0
        const val CENTER_Z = 0.0
        const val INITIAL_SIZE = 128.0
        const val WAIT_TIME_DAYS = 10
        const val INCREMENT = 128.0
        const val MAX_DAYS = 300
        const val MAX_SIZE = 29999984.0
        const val TRANSITION_TIME = 180 // 180s
        const val AUTO_EXPAND = true
        const val NOTIFY_PLAYERS = true
        const val LOG_UPDATES = true
        const val BLUEMAP_INTEGRATION = true
    }

    private var lastExpansionDay: Long = 0

    private fun <T> getConfigValue(path: String, default: T): T = plugin.config.get(path, default) as T

    private fun getWorld(): World {
        val worldName = getConfigValue("world", DefaultConfig.WORLD)
        return Bukkit.getWorld(worldName) ?: throw IllegalStateException("World '$worldName' not found!")
    }

    private fun withWorld(action: (World) -> Unit) {
        try {
            val world = getWorld()
            action(world)
        } catch (e: IllegalStateException) {
            plugin.logger.warning(e.message)
        }
    }

    fun initializeWorldBorder() {
        withWorld { world ->
            val worldBorder = world.worldBorder

            val centerX = getConfigValue("border.centerX", DefaultConfig.CENTER_X)
            val centerZ = getConfigValue("border.centerZ", DefaultConfig.CENTER_Z)
            val initialSize = getConfigValue("border.initialSize", DefaultConfig.INITIAL_SIZE)

            worldBorder.center = Location(world, centerX, 64.0, centerZ)
            worldBorder.size = initialSize

            plugin.logger.info("World border initialized: center=($centerX, $centerZ), size=$initialSize.")

            // Verify configuration consistency
            plugin.config.set("border.centerX", centerX)
            plugin.config.set("border.centerZ", centerZ)
            plugin.config.set("border.initialSize", initialSize)
            plugin.saveConfig()
        }
    }

    fun getDebugInfo(): List<String> {
        return try {
            val world = getWorld()
            val currentDay = world.fullTime / 24000
            val waitTime = getConfigValue("border.waitTime", DefaultConfig.WAIT_TIME_DAYS)
            val increment = getConfigValue("border.increment", DefaultConfig.INCREMENT)
            val initialSize = getConfigValue("border.initialSize", DefaultConfig.INITIAL_SIZE)
            val currentSize = world.worldBorder.size
            val maxDays = getConfigValue("border.maxDays", DefaultConfig.MAX_DAYS)
            val maxExpansions = maxDays / waitTime
            val expansionsDone = (currentDay / waitTime).toInt().coerceAtMost(maxExpansions)

            val bluemapEnabled = isBlueMapIntegrationEnabled()
            val bluemapInfo = if (bluemapEnabled) {
                val minX = (world.worldBorder.center.x - world.worldBorder.size / 2).toInt()
                val maxX = (world.worldBorder.center.x + world.worldBorder.size / 2).toInt()
                val minZ = (world.worldBorder.center.z - world.worldBorder.size / 2).toInt()
                val maxZ = (world.worldBorder.center.z + world.worldBorder.size / 2).toInt()
                listOf(
                    "§bBlueMap Integration: §aEnabled",
                    "§bBlueMap Render Bounds: §fminX=$minX, minZ=$minZ, maxX=$maxX, maxZ=$maxZ"
                )
            } else {
                listOf("§bBlueMap Integration: §cDisabled")
            }

            listOf(
                "§bCurrent Day: §f$currentDay",
                "§bLast Expansion Day: §f$lastExpansionDay",
                "§bWait Time (days): §f$waitTime",
                "§bIncrement Size: §f$increment blocks",
                "§bInitial Border Size: §f$initialSize blocks",
                "§bCurrent Border Size: §f$currentSize blocks",
                "§bMax Expansions Allowed: §f$maxExpansions",
                "§bExpansions Done: §f$expansionsDone"
            ) + bluemapInfo
        } catch (e: IllegalStateException) {
            listOf("§cWorld not found!")
        }
    }



    fun restoreDefaults() {
        // Скидаємо конфігурацію до стандартних значень
        plugin.config.apply {
            set("world", DefaultConfig.WORLD)
            set("border.centerX", DefaultConfig.CENTER_X)
            set("border.centerZ", DefaultConfig.CENTER_Z)
            set("border.initialSize", DefaultConfig.INITIAL_SIZE)
            set("border.waitTime", DefaultConfig.WAIT_TIME_DAYS)
            set("border.increment", DefaultConfig.INCREMENT)
            set("border.maxDays", DefaultConfig.MAX_DAYS)
            set("border.transitionTime", DefaultConfig.TRANSITION_TIME)
        }
        plugin.saveConfig()

        // Скидаємо межі світу
        withWorld { world ->
            val worldBorder = world.worldBorder

            worldBorder.center = Location(world, DefaultConfig.CENTER_X, 64.0, DefaultConfig.CENTER_Z)
            worldBorder.size = DefaultConfig.INITIAL_SIZE
            world.fullTime = 0

            lastExpansionDay = 0L

            plugin.logger.info("Defaults restored and verified.")
        }

        // Оновлюємо налаштування BlueMap і очищаємо карту
        val worldName = plugin.config.getString("world", DefaultConfig.WORLD)
        if (worldName != null && isBlueMapIntegrationEnabled()) {
            val minX = (DefaultConfig.CENTER_X - DefaultConfig.INITIAL_SIZE / 2).toInt()
            val maxX = (DefaultConfig.CENTER_X + DefaultConfig.INITIAL_SIZE / 2).toInt()
            val minZ = (DefaultConfig.CENTER_Z - DefaultConfig.INITIAL_SIZE / 2).toInt()
            val maxZ = (DefaultConfig.CENTER_Z + DefaultConfig.INITIAL_SIZE / 2).toInt()

            // Оновлення конфігурації BlueMap
            updateBlueMapConfig(worldName, minX, maxX, minZ, maxZ)

            // Команда для очищення карти
            val clearCommand = "bluemap purge $worldName"
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), clearCommand)
                plugin.logger.info("BlueMap map cleared with command: $clearCommand")
            } catch (e: Exception) {
                plugin.logger.severe("Failed to clear BlueMap map: ${e.message}")
            }

            val reloadCommand = "bluemap reload"
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reloadCommand)
                plugin.logger.info("BlueMap configuration reloaded with command: $reloadCommand")
            } catch (e: Exception) {
                plugin.logger.severe("Failed to reload BlueMap: ${e.message}")
            }


            // Команда для рендерингу карти заново
            val renderCommand = "bluemap update $worldName ${DefaultConfig.CENTER_X.toInt()} ${DefaultConfig.CENTER_Z.toInt()} ${(DefaultConfig.INITIAL_SIZE / 2).toInt()}"
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), renderCommand)
                    plugin.logger.info("BlueMap map re-rendered with command: $renderCommand")
                } catch (e: Exception) {
                    plugin.logger.severe("Failed to re-render BlueMap map: ${e.message}")
                }
            }, 20L * 10) // Delay execution by 10 seconds (20 ticks per second * 10 seconds)
        } else {
            plugin.logger.info("BlueMap integration is disabled or world name is missing. Skipping BlueMap reset.")
        }
    }

    fun freezeBlueMap(worldName: String) {
        if (!isBlueMapIntegrationEnabled()) {
            plugin.logger.info("BlueMap integration is disabled. Freeze command will not be executed.")
            return
        }

        val world = Bukkit.getWorld(worldName)
        if (world == null) {
            plugin.logger.warning("World '$worldName' does not exist. BlueMap freeze command will not be executed.")
            return
        }

        val command = "bluemap freeze $worldName"

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            plugin.logger.info("BlueMap has been frozen using command: $command")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to execute BlueMap freeze command: ${e.message}")
        }
    }

    fun triggerBlueMapUpdate(worldName: String, centerX: Int, centerZ: Int, radius: Int) {
        val command = "bluemap update $worldName $centerX $centerZ $radius"

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            plugin.logger.info("BlueMap update triggered with command: $command")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to execute BlueMap update command: ${e.message}")
        }
    }

    private fun updateBlueMapConfig(worldName: String, minX: Int, maxX: Int, minZ: Int, maxZ: Int) {
        val configPath = "/home/container/plugins/BlueMap/maps/$worldName.conf"

        val configFile = File(configPath)
        if (!configFile.exists()) {
            plugin.logger.warning("BlueMap configuration file for world '$worldName' not found!")
            return
        }

        try {
            val lines = configFile.readLines().toMutableList()
            val updatedLines = lines.map { line ->
                when {
                    line.startsWith("min-x:") -> "min-x: $minX"
                    line.startsWith("max-x:") -> "max-x: $maxX"
                    line.startsWith("min-z:") -> "min-z: $minZ"
                    line.startsWith("max-z:") -> "max-z: $maxZ"
                    else -> line
                }
            }

            configFile.writeText(updatedLines.joinToString("\n"))
            plugin.logger.info("BlueMap configuration updated for world '$worldName': min=($minX, $minZ), max=($maxX, $maxZ)")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to update BlueMap configuration for world '$worldName': ${e.message}")
        }
    }

    private fun reloadBlueMap() {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap reload")
            plugin.logger.info("BlueMap configuration reloaded.")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to reload BlueMap: ${e.message}")
        }
    }

    fun updateRenderArea(worldName: String, centerX: Int, centerZ: Int, radius: Int) {
        val minX = centerX - radius
        val maxX = centerX + radius
        val minZ = centerZ - radius
        val maxZ = centerZ + radius

        updateBlueMapConfig(worldName, minX, maxX, minZ, maxZ)
        reloadBlueMap()
    }

    fun scheduleSmoothBorderUpdates() {
        val waitTimeDays = getConfigValue("border.waitTime", DefaultConfig.WAIT_TIME_DAYS)
        val increment = getConfigValue("border.increment", DefaultConfig.INCREMENT)
        val maxDays = getConfigValue("border.maxDays", DefaultConfig.MAX_DAYS)
        val maxSize = getConfigValue("border.maxSize", DefaultConfig.MAX_SIZE)
        val initialSize = getConfigValue("border.initialSize", DefaultConfig.INITIAL_SIZE)
        val maxExpansions = maxDays / waitTimeDays

        object : BukkitRunnable() {
            override fun run() {
                withWorld { world ->
                    val autoExpand = getConfigValue("border.autoExpand", DefaultConfig.AUTO_EXPAND)
                    if (!autoExpand) return@withWorld

                    val currentDay = world.fullTime / 24000
                    val expansionsDone = (currentDay / waitTimeDays).toInt().coerceAtMost(maxExpansions)
                    val newSize = (initialSize + expansionsDone * increment).coerceAtMost(maxSize)

                    if (currentDay >= lastExpansionDay + waitTimeDays && expansionsDone < maxExpansions) {
                        val worldBorder = world.worldBorder
                        val transitionTime = getConfigValue("border.transitionTime", DefaultConfig.TRANSITION_TIME)

                        worldBorder.setSize(newSize, transitionTime*20L)
                        lastExpansionDay = currentDay

                        val centerX = worldBorder.center.blockX
                        val centerZ = worldBorder.center.blockZ
                        val radius = (newSize / 2).toInt()

                        // Verify and save updated configuration
                        plugin.config.set("border.centerX", centerX)
                        plugin.config.set("border.centerZ", centerZ)
                        plugin.config.set("border.currentSize", newSize)
                        plugin.saveConfig()

                        updateRenderArea(world.name, centerX, centerZ, radius)
                        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                            triggerBlueMapUpdate(world.name, centerX, centerZ, radius)
                        }, 20L * 20)

                        if (getConfigValue("border.notifyPlayers", DefaultConfig.NOTIFY_PLAYERS)) {
                            Bukkit.broadcastMessage("§aWorld border expanded to ${newSize.toInt()} blocks! Transition time: $transitionTime seconds.")
                        }

                        if (getConfigValue("border.logUpdates", DefaultConfig.LOG_UPDATES)) {
                            plugin.logger.info("World border updated: size=$newSize, day=$currentDay.")
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60)
    }

    fun isBlueMapIntegrationEnabled(): Boolean {
        return plugin.config.getBoolean("bluemapIntegration", DefaultConfig.BLUEMAP_INTEGRATION)
    }
}
