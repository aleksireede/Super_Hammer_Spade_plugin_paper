package io.github.poorgrammerdev.hammer;

import java.net.URI;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResourcePackListener implements Listener {
    private static final String RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/0vrlmtlcbinkxrh0udq6t/hammer_resourcepack.zip?rlkey=56e3085dn3gm3j6hlmaftzlb8&st=lzl8fvjx&dl=1";
    private static final String RESOURCE_PACK_HASH = "2e7a8f120f37141b688846230fee78dffe9cc5d2";
    private static final ResourcePackRequest RESOURCE_PACK_REQUEST = ResourcePackRequest.resourcePackRequest()
        .packs(ResourcePackInfo.resourcePackInfo()
            .uri(URI.create(RESOURCE_PACK_URL))
            .hash(RESOURCE_PACK_HASH)
            .build())
        .build();

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.sendResourcePacks(RESOURCE_PACK_REQUEST);
    }
}
