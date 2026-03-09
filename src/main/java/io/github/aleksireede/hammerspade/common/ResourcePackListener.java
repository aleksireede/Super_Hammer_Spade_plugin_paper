package io.github.aleksireede.hammerspade.common;

import io.github.aleksireede.hammershared.SharedResourcePackManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResourcePackListener implements Listener {
    private final SharedResourcePackManager resourcePackManager;

    public ResourcePackListener(final SharedResourcePackManager resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!this.resourcePackManager.shouldSendOnJoin()) return;
        final Player player = event.getPlayer();
        this.resourcePackManager.sendToPlayer(player);
    }
}
