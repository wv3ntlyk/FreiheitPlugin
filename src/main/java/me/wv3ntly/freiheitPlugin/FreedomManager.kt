package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

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
        const val TRANSITION_TIME = 3600L // 180s
        const val AUTO_EXPAND = true
        const val NOTIFY_PLAYERS = true
        const val LOG_UPDATES = true
        const val BLUEMAP_INTEGRATION = false
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
        }
    }

    fun restoreDefaults() {
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

        withWorld { world ->
            val worldBorder = world.worldBorder

            worldBorder.center = Location(world, DefaultConfig.CENTER_X, 64.0, DefaultConfig.CENTER_Z)
            worldBorder.size = DefaultConfig.INITIAL_SIZE
            world.fullTime = 0

            lastExpansionDay = 0L
            plugin.logger.info("Defaults restored.")
        }
    }

    fun isBlueMapIntegrationEnabled(): Boolean {
        return plugin.config.getBoolean("bluemapIntegration", DefaultConfig.BLUEMAP_INTEGRATION)
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

            listOf(
                "§bCurrent Day: §f$currentDay",
                "§bLast Expansion Day: §f$lastExpansionDay",
                "§bWait Time (days): §f$waitTime",
                "§bIncrement Size: §f$increment blocks",
                "§bInitial Border Size: §f$initialSize blocks",
                "§bCurrent Border Size: §f$currentSize blocks",
                "§bMax Expansions Allowed: §f$maxExpansions",
                "§bExpansions Done: §f$expansionsDone"
            )
        } catch (e: IllegalStateException) {
            listOf("§cWorld not found!")
        }
    }

    fun triggerBlueMapUpdate() {
        fun triggerBlueMapUpdate() {
            if (!isBlueMapIntegrationEnabled()) {
                plugin.logger.info("BlueMap integration is disabled in the configuration.")
                return
            }
        }

        val worldName = plugin.config.getString("world", DefaultConfig.WORLD)
        val world = worldName?.let { Bukkit.getWorld(it) }

        if (world == null) {
            plugin.logger.warning("World '$worldName' not found for BlueMap integration!")
            return
        }

        val worldBorder = world.worldBorder
        val center = worldBorder.center
        val radius = (worldBorder.size / 2.0).toInt()

        val command = "bluemap update $worldName ${center.blockX} ${center.blockZ} $radius"

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            plugin.logger.info("BlueMap update triggered with command: $command")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to execute BlueMap update command: ${e.message}")
        }
    }

    fun scheduleSmoothBorderUpdates() {
        val worldName = getConfigValue("world", DefaultConfig.WORLD)
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

                        worldBorder.setSize(newSize, transitionTime)

                        lastExpansionDay = currentDay
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
}