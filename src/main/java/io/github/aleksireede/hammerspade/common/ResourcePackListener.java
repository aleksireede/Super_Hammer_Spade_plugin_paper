package io.github.aleksireede.hammerspade.common;

import io.github.aleksireede.hammershared.SharedText;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ResourcePackListener implements Listener {
    private static final String RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/3yrdxl0isw3pan58h7d1g/hammer_spade_drill_Res-pack.zip?rlkey=zayu37yhqc6ucor5ox9hk8etm&st=v9aqek1q&dl=1";
    private static final byte[] RESOURCE_PACK_SHA1 = hexToBytes("dd616440073a4921b2ad2483a9aa4e196b802d26");
    private static final String RESOURCE_PACK_PROMPT = "<gray>This server uses a custom resource pack for tool models.</gray>";
    private final JavaPlugin plugin;

    public ResourcePackListener(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.plugin.getLogger().info("Sending the HammerSpade resource pack to " + player.getName());
        player.setResourcePack(
                RESOURCE_PACK_URL,
                RESOURCE_PACK_SHA1,
                SharedText.miniMessage(RESOURCE_PACK_PROMPT),
                true
        );
    }

    @EventHandler
    public void onResourcePackStatus(final PlayerResourcePackStatusEvent event) {
        this.plugin.getLogger().info(
                "Resource pack status for " + event.getPlayer().getName() + ": " + event.getStatus()
        );
    }

    private static byte[] hexToBytes(final String hex) {
        final byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
