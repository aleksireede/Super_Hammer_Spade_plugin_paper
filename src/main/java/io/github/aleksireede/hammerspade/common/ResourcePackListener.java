package io.github.aleksireede.hammerspade.common;

import io.github.aleksireede.hammershared.SharedResourcePackManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ResourcePackListener implements Listener {
    private final JavaPlugin plugin;
    private final SharedResourcePackManager resourcePackManager;

    public ResourcePackListener(
            final JavaPlugin plugin,
            final SharedResourcePackManager resourcePackManager
    ) {
        this.plugin = plugin;
        this.resourcePackManager = resourcePackManager;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!this.resourcePackManager.shouldSendOnJoin()) return;
        final Player player = event.getPlayer();
        this.plugin.getLogger().info("Sending configured resource pack to " + player.getName());
        this.resourcePackManager.sendToPlayer(player);
    }

    @EventHandler
    public void onResourcePackStatus(final PlayerResourcePackStatusEvent event) {
        this.plugin.getLogger().info(
                "Resource pack status for " + event.getPlayer().getName() + ": " + event.getStatus()
        );
    }
}
