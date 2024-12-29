package me.wv3ntly.freiheitPlugin

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class FreedomTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (command.name.equals("freedom", ignoreCase = true)) {
            return when (args.size) {
                1 -> listOf("restore", "debug", "render").filter { it.startsWith(args[0], ignoreCase = true) }
                else -> null
            }
        }
        return null
    }
}

