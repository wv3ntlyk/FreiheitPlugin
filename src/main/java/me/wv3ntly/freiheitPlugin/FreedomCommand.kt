package me.wv3ntly.freiheitPlugin

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class FreedomCommand(private val freedomManager: FreedomManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§eCommands for /freedom:")
            sender.sendMessage("§e/freedom debug - Display debug information")
            sender.sendMessage("§e/freedom restore - Restore default settings")
            sender.sendMessage("§e/freedom render - Trigger BlueMap update for the current world border.")
            sender.sendMessage("")
            return true
        }

        when (args[0].lowercase()) {
            "restore" -> {
                freedomManager.restoreDefaults()
                sender.sendMessage("§aDefaults restored! Restart the server to apply the changes. ")
            }
            "debug" -> {
                val worldInfo = freedomManager.getDebugInfo()
                sender.sendMessage("§6=== §eWorld Border Debug Info §6===")
                worldInfo.forEach { sender.sendMessage(it) }
                sender.sendMessage("§6==============================")
            }
            "render" -> {
                freedomManager.triggerBlueMapUpdate()
                sender.sendMessage("§aBlueMap update triggered for current world border area.")
            }

            else -> sender.sendMessage("§cUnknown command. Use /freedom for a list of available commands.")
        }
        return true
    }
}
