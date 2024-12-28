package me.wv3ntly.freiheitPlugin

import org.bukkit.plugin.java.JavaPlugin

class FreiheitPlugin : JavaPlugin() {

    override fun onEnable() {
        logger.info("FreiheitPlugin activated!")
        saveDefaultConfig()

        val worldBarrierManager = WorldBarrierManager(this)
        worldBarrierManager.initializeWorldBorder()
        worldBarrierManager.scheduleSmoothBorderUpdates()

        registerCommands(worldBarrierManager)
    }

    override fun onDisable() {
        logger.info("FreiheitPlugin deactivated!")
    }

    private fun registerCommands(worldBarrierManager: WorldBarrierManager) {
        getCommand("worldbarrier")?.setExecutor(WorldBarrierCommand(worldBarrierManager))
    }
}