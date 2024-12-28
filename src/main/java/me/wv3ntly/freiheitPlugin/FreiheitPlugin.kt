package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class FreiheitPlugin : JavaPlugin() {

    override fun onEnable() {
        logger.info("OurFreiheit activated!")
        logCurrentDay()
        initializeWorldBorder()
        scheduleBorderExpansion()
    }

    override fun onDisable() {
        logger.info("OurFreiheit deactivated!")
    }

    private fun initializeWorldBorder() {
        // Отримуємо основний світ
        val worldName = "world" // Замініть "world" на назву вашого світу, якщо інша
        val world: World? = Bukkit.getWorld(worldName)
        if (world == null) {
            logger.warning("Світ '$worldName' не знайдено!")
            return
        }

        // Налаштовуємо бар'єр світу
        val worldBorder = world.worldBorder
        worldBorder.center = Location(world, 0.0, 64.0, 0.0) // Центр світу на висоті 64
        worldBorder.size = 64.0 // Розмір 4x4 чанки (64 блоків)
        logger.info("Бар'єр світу успішно встановлений з розміром 64 блоки.")
    }

    private fun scheduleBorderExpansion() {
        val worldName = "world" // Замініть "world" на назву вашого світу, якщо інша
        val world: World? = Bukkit.getWorld(worldName)
        if (world == null) {
            logger.warning("Світ '$worldName' не знайдено для розширення бар'єру!")
            return
        }

        object : BukkitRunnable() {
            private var lastDayChecked = -1L
            private var expansionStep = 1
            override fun run() {
                val currentDay = world.fullTime / 24000 // Отримуємо ігровий день

                if (currentDay != lastDayChecked) { // Розширяємо лише при зміні дня
                    lastDayChecked = currentDay

                    val worldBorder = world.worldBorder
                    val newSize = 64.0 + expansionStep * 64.0 // Кожен крок додає 64 блоки
                    worldBorder.size = newSize

                    // Повідомлення гравцям
                    Bukkit.broadcastMessage("§aБар'єр світу розширено до ${newSize.toInt()} блоків!")

                    expansionStep++
                    if (expansionStep > 10) { // Зупиняємо після 10 розширень
                        cancel()
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L) // Перевіряємо щосекунди (20 тіків)
    }

    private fun logCurrentDay() {
        val worldName = "world" // Замініть "world" на назву вашого світу, якщо інша
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            val daysPassed = world.fullTime / 24000
            logger.info("Теперішній ігровий день: $daysPassed")
        } else {
            logger.warning("Світ '$worldName' не знайдено!")
        }
    }
}
