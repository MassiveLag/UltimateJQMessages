package nl.chimpgamer.ultimatejqmessages.paper.listeners

import nl.chimpgamer.ultimatejqmessages.paper.UltimateJQMessagesPlugin
import nl.chimpgamer.ultimatejqmessages.paper.extensions.getDisplayNamePlaceholder
import nl.chimpgamer.ultimatejqmessages.paper.extensions.parse
import nl.chimpgamer.ultimatejqmessages.paper.utils.Cooldown
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration

class PlayerConnectionListener(private val plugin: UltimateJQMessagesPlugin) : Listener {
    private val joinMessageCooldownKey = "JoinMessageCooldown"
    private val quitMessageCooldownKey = "QuitMessageCooldown"

    @EventHandler(priority = EventPriority.MONITOR)
    fun AsyncPlayerPreLoginEvent.onAsyncPlayerPreLogin() {
        if (loginResult !== AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        plugin.usersHandler.loadUser(uniqueId, name)
    }

    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        joinMessage(null)
        if (Cooldown.hasCooldown(player.uniqueId, joinMessageCooldownKey)) return
        val user = plugin.usersHandler.getUser(player.uniqueId) ?: return
        val joinMessage = user.customJoinMessage ?: user.joinMessage?.message
        joinMessage(joinMessage?.parse(getDisplayNamePlaceholder(player)))
        Cooldown(player.uniqueId, joinMessageCooldownKey, Duration.ofSeconds(plugin.settingsConfig.joinMessagesCooldown)).start()
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerQuit() {
        quitMessage(null)
        if (Cooldown.hasCooldown(player.uniqueId, quitMessageCooldownKey)) return
        val user = plugin.usersHandler.getUser(player.uniqueId) ?: return
        val quitMessage = user.customQuitMessage ?: user.quitMessage?.message
        quitMessage(quitMessage?.parse(getDisplayNamePlaceholder(player)))
        Cooldown(player.uniqueId, joinMessageCooldownKey, Duration.ofSeconds(plugin.settingsConfig.quitMessagesCooldown)).start()
    }
}