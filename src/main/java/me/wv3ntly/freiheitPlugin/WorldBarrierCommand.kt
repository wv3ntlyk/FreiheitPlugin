package me.wv3ntly.freiheitPlugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
class WorldBarrierCommand(private val worldBarrierManager: WorldBarrierManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§aCommands for /worldbarrier:")
            sender.sendMessage("§a/worldbarrier set <size> - Set the world border size")
            sender.sendMessage("§a/worldbarrier setCenter <x> <z> - Set the world border center")
            sender.sendMessage("§a/worldbarrier setWaitTime <days> - Set the wait time between expansions (in days)")
            sender.sendMessage("§a/worldbarrier setMaxDays <days> - Set the maximum number of days for expansions")
            sender.sendMessage("§a/worldbarrier setIncrement <size> - Set the increment size for each expansion")
            sender.sendMessage("§a/worldbarrier setTransitionTime <seconds> - Set the transition time for expansions")
            sender.sendMessage("§a/worldbarrier debug - Display debug information")
            return true
        }

        when (args[0].lowercase()) {
            "set" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cSpecify the border size (in blocks)!")
                    return true
                }
                val newSize = args[1].toDoubleOrNull()
                if (newSize == null || newSize <= 0) {
                    sender.sendMessage("§cInvalid border size. Use a positive number!")
                    return true
                }
                val world = worldBarrierManager.plugin.config.getString("world", "world")?.let { Bukkit.getWorld(it) }
                if (world != null) {
                    world.worldBorder.size = newSize
                    sender.sendMessage("§aWorld border size set to ${newSize.toInt()} blocks.")
                } else {
                    sender.sendMessage("§cWorld not found!")
                }
            }

            "setcenter" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cSpecify the X and Z coordinates for the border center!")
                    return true
                }
                val centerX = args[1].toDoubleOrNull()
                val centerZ = args[2].toDoubleOrNull()
                if (centerX == null || centerZ == null) {
                    sender.sendMessage("§cInvalid coordinates. Use numbers!")
                    return true
                }
                val world = worldBarrierManager.plugin.config.getString("world", "world")?.let { Bukkit.getWorld(it) }
                if (world != null) {
                    world.worldBorder.center = Location(world, centerX, 64.0, centerZ)
                    sender.sendMessage("§aWorld border center set to: X=${centerX}, Z=${centerZ}.")
                } else {
                    sender.sendMessage("§cWorld not found!")
                }
            }

            "setwaittime" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cSpecify the wait time between expansions (in days)!")
                    return true
                }
                val waitTime = args[1].toIntOrNull()
                if (waitTime == null || waitTime <= 0) {
                    sender.sendMessage("§cInvalid wait time. Use a positive number!")
                    return true
                }
                worldBarrierManager.plugin.config.set("border.waitTime", waitTime)
                worldBarrierManager.plugin.saveConfig()
                sender.sendMessage("§aWait time between expansions set to $waitTime days.")
            }

            "setmaxdays" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cSpecify the maximum number of days for expansions!")
                    return true
                }
                val maxDays = args[1].toIntOrNull()
                if (maxDays == null || maxDays <= 0) {
                    sender.sendMessage("§cInvalid number of days. Use a positive number!")
                    return true
                }
                worldBarrierManager.plugin.config.set("border.maxDays", maxDays)
                worldBarrierManager.plugin.saveConfig()
                sender.sendMessage("§aMaximum number of days for expansions set to $maxDays.")
            }

            "setincrement" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cSpecify the increment size (in blocks) for expansions!")
                    return true
                }
                val increment = args[1].toDoubleOrNull()
                if (increment == null || increment <= 0) {
                    sender.sendMessage("§cInvalid increment size. Use a positive number!")
                    return true
                }
                worldBarrierManager.plugin.config.set("border.increment", increment)
                worldBarrierManager.plugin.saveConfig()
                sender.sendMessage("§aIncrement size for expansions set to $increment blocks.")
            }

            "settransitiontime" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cSpecify the transition time (in seconds) for expansions!")
                    return true
                }
                val transitionTime = args[1].toLongOrNull()
                if (transitionTime == null || transitionTime <= 0) {
                    sender.sendMessage("§cInvalid transition time. Use a positive number!")
                    return true
                }
                worldBarrierManager.plugin.config.set("border.transitionTime", transitionTime)
                worldBarrierManager.plugin.saveConfig()
                sender.sendMessage("§aTransition time for expansions set to $transitionTime seconds.")
            }

            "debug" -> {
                val worldName = worldBarrierManager.plugin.config.getString("world", "world")
                val world: World? = worldName?.let { Bukkit.getWorld(it) }
                if (world != null) {
                    val currentDay = world.fullTime / 24000
                    val waitTime = worldBarrierManager.plugin.config.getInt("border.waitTime")
                    val nextExpansionDay = worldBarrierManager.lastExpansionDay + waitTime
                    val increment = worldBarrierManager.plugin.config.getDouble("border.increment")
                    val initialSize = worldBarrierManager.plugin.config.getDouble("border.initialSize")
                    val currentSize = world.worldBorder.size
                    val expansionsDone = (currentDay / waitTime).toInt()
                    val maxDays = worldBarrierManager.plugin.config.getInt("border.maxDays")
                    val maxExpansions = maxDays / waitTime
                    sender.sendMessage("§6=== §eWorld Border Debug Info §6===")
                    sender.sendMessage("§bCurrent Day: §f$currentDay")
                    sender.sendMessage("§bNext Expansion Day: §f$nextExpansionDay")
                    sender.sendMessage("§bLast Expansion Day: §f${worldBarrierManager.lastExpansionDay}")
                    sender.sendMessage("§bWait Time (days): §f$waitTime")
                    sender.sendMessage("§bIncrement Size: §f$increment blocks")
                    sender.sendMessage("§bInitial Border Size: §f$initialSize blocks")
                    sender.sendMessage("§bCurrent Border Size: §f$currentSize blocks")
                    sender.sendMessage("§bMax Expansions Allowed: §f$maxExpansions")
                    sender.sendMessage("§bExpansions Done: §f${expansionsDone.coerceAtMost(maxExpansions)}")
                    sender.sendMessage("§6==============================")
                } else {
                    sender.sendMessage("§cWorld '$worldName' not found!")
                }
            }

            else -> sender.sendMessage("§cUnknown command. Use /worldbarrier for a list of available commands.")
        }
        return true
    }
}

