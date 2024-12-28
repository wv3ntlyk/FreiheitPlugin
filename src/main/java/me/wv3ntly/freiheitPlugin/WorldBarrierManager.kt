package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable


class WorldBarrierManager(val plugin: JavaPlugin) {

    var lastExpansionDay = 0L

    fun initializeWorldBorder() {
        val worldName = plugin.config.getString("world", "world")
        val world: World? = worldName?.let { Bukkit.getWorld(it) }
        if (world == null) {
            plugin.logger.warning("World '$worldName' not found!")
            return
        }

        val worldBorder = world.worldBorder
        val centerX = plugin.config.getDouble("border.centerX", 0.0)
        val centerZ = plugin.config.getDouble("border.centerZ", 0.0)
        val initialSize = plugin.config.getDouble("border.initialSize", 64.0)

        worldBorder.center = Location(world, centerX, 64.0, centerZ)
        worldBorder.size = initialSize
        plugin.logger.info("World border successfully set to size $initialSize blocks.")
    }

    fun scheduleSmoothBorderUpdates() {
        val worldName = plugin.config.getString("world", "world")
        val world: World? = worldName?.let { Bukkit.getWorld(it) }
        if (world == null) {
            plugin.logger.warning("World '$worldName' not found for border updates!")
            return
        }

        val waitTimeDays = plugin.config.getInt("border.waitTime", 1)
        val increment = plugin.config.getDouble("border.increment", 64.0)
        val maxDays = plugin.config.getInt("border.maxDays", 9)
        val initialSize = plugin.config.getDouble("border.initialSize", 64.0)

        object : BukkitRunnable() {
            override fun run() {
                val currentDay = world.fullTime / 24000

                // Calculate total expansions possible within maxDays
                val maxExpansions = maxDays / waitTimeDays
                val expansionsDone = (currentDay / waitTimeDays).toInt().coerceAtMost(maxExpansions)

                // Calculate new size
                val newSize = initialSize + expansionsDone * increment

                // Expand only if size has changed
                if (expansionsDone > (lastExpansionDay / waitTimeDays).toInt()) {
                    val worldBorder = world.worldBorder
                    val transitionTime = plugin.config.getLong("border.transitionTime", 60L)
                    worldBorder.setSize(newSize, transitionTime)

                    Bukkit.broadcastMessage("§aWorld border updated to ${newSize.toInt()} blocks over $transitionTime seconds! Current day: $currentDay")
                    plugin.logger.info("World border updated: new size $newSize blocks, current day: $currentDay")

                    lastExpansionDay = (expansionsDone * waitTimeDays).toLong()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

}