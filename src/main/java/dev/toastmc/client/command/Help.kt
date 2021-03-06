package dev.toastmc.client.command

import com.mojang.brigadier.CommandDispatcher
import dev.toastmc.client.ToastClient
import dev.toastmc.client.command.util.Command
import dev.toastmc.client.command.util.does
import dev.toastmc.client.command.util.register
import dev.toastmc.client.command.util.rootLiteral
import dev.toastmc.client.util.Color
import dev.toastmc.client.util.sendMessage
import net.minecraft.server.command.CommandSource

class Help : Command("help") {
    override fun register(dispatcher: CommandDispatcher<CommandSource>) {
        dispatcher register rootLiteral("help") {
            does {
                val str = ToastClient.COMMAND_MANAGER.commandsToString(true)
                sendMessage("Commands (${ToastClient.COMMAND_MANAGER.commands.size}): $str", Color.GRAY)
                0
            }
        }
    }
}