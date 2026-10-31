package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammershared.SharedItemUpdater;
import io.github.aleksireede.hammershared.SharedText;
import io.github.aleksireede.hammerspade.common.config;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Keeps custom tool lore in sync with enchantments stored on the item. */
public final class ItemLoreUpdater implements Listener {
    private final Hammer plugin;

    public ItemLoreUpdater(Hammer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshInventory(player));
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshInventory(player));
    }

    @EventHandler
    public void onAnvilPreview(PrepareAnvilEvent event) {
        refreshResult(event.getResult(), event::setResult);
    }

    @EventHandler
    public void onSmithingPreview(PrepareSmithingEvent event) {
        refreshResult(event.getResult(), event::setResult);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshInventory(event.getPlayer()));
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        refreshLore(item);
        player.getInventory().setItem(event.getNewSlot(), item);
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshInventory(event.getPlayer()));
    }

    private void refreshInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            refreshLore(item);
            player.getInventory().setItem(slot, item);
        }
        ItemStack cursor = player.getItemOnCursor();
        refreshLore(cursor);
        player.setItemOnCursor(cursor);
    }

    private void refreshLore(ItemStack item) {
        CustomToolType type = plugin.getCustomToolType(item);
        if (type == null) return;

        SharedItemUpdater.updateChecker(item);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = new ArrayList<>();
        String action = type == CustomToolType.HAMMER ? "Mine" : "Dig";
        if (plugin.getConfig().getBoolean("write_description", true)) lore.addAll(ItemLore.tool_lore(action));
        if (!meta.getEnchants().isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            meta.getEnchants().forEach((enchantment, level) -> lore.add(SharedText.miniMessage(
                    "<!i><gray>" + formatName(enchantment.getKey().getKey()) + " " + level
            )));
        }

        String rarity = meta.getPersistentDataContainer().get(config.rarityKey(), PersistentDataType.STRING);
        String itemType = meta.getPersistentDataContainer().get(config.itemTypeKey(), PersistentDataType.STRING);
        if (rarity != null) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            String typeName = itemType == null ? "" : itemType.toUpperCase(java.util.Locale.ROOT);
            lore.add(SharedText.miniMessage(SharedItemUpdater.getColorFromRarity(rarity)
                    + "<b>" + rarity.toUpperCase(java.util.Locale.ROOT) + " " + typeName + "</b>"));
        }
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    private void refreshResult(ItemStack result, java.util.function.Consumer<ItemStack> setter) {
        if (result == null || plugin.getCustomToolType(result) == null) return;
        refreshLore(result);
        setter.accept(result);
    }

    private String formatName(String key) {
        String[] words = key.split("_");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
            }
        }
        return String.join(" ", words);
    }

}
