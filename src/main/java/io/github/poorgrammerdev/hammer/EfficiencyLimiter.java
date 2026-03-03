package io.github.poorgrammerdev.hammer;

import java.time.Duration;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Toggleable system that limits level of Efficiency that custom area tools can have
 * @author Thomas Tran
 */
public class EfficiencyLimiter implements Listener {
    private final Hammer plugin;
    private final CooldownManager messageCooldownManager;

    private final boolean enabled;
    private final int maxLevel;
    private final boolean sendMessageToPlayers;
    private final int messageCooldownMinutes;

    public EfficiencyLimiter(final Hammer plugin) {
        this.plugin = plugin;
        this.messageCooldownManager = new CooldownManager();

        this.enabled = plugin.getConfig().getBoolean("efficiency_limiter.enabled", false);
        this.maxLevel = plugin.getConfig().getInt("efficiency_limiter.max_level", 3);
        this.sendMessageToPlayers = plugin.getConfig().getBoolean("efficiency_limiter.send_info_message_to_players", true);
        this.messageCooldownMinutes = plugin.getConfig().getInt("efficiency_limiter.message_cooldown_minutes", 10);
    }

    /**
     * Implements limit visually for Enchanting Table GUI offers
     */
    @EventHandler(ignoreCancelled = true)
    private void onViewEnchants(final PrepareItemEnchantEvent event) {
        if (!this.enabled) return;

        final ItemStack item = event.getItem();
        if (!this.plugin.isCustomTool(item)) return;
        
        //For any Efficiency enchantment offering
        boolean madeAnyChanges = false;
        for (final EnchantmentOffer offer : event.getOffers()) {
            assert offer != null;
            if (offer.getEnchantment() != Enchantment.EFFICIENCY) continue;

            //Limit level to the max level
            if (offer.getEnchantmentLevel() > this.maxLevel) {
                offer.setEnchantmentLevel(this.maxLevel);
                madeAnyChanges = true;
            }
        }
        
        //Send message if setting enabled and item was changed at all
        final Player player = event.getEnchanter();
        if (madeAnyChanges && this.sendMessageToPlayers && !this.messageCooldownManager.isOnCooldown(player)) {
            player.sendRichMessage("<gray>[INFO] The max Efficiency level on custom tools has been set to <white>" + this.maxLevel + "</white>.</gray>");
            this.messageCooldownManager.setCooldown(player, Duration.ofMinutes(this.messageCooldownMinutes));
        }
    }

    /**
     * Implements end result for enchanting with Enchanting Tables
     */
    @EventHandler(ignoreCancelled = true)
    private void onTableEnchant(final EnchantItemEvent event) {
        if (!this.enabled) return;

        final ItemStack item = event.getItem();
        if (!this.plugin.isCustomTool(item)) return;

        final Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();

        final Integer level = enchants.getOrDefault(Enchantment.EFFICIENCY, null);
        if (level == null || level <= this.maxLevel) return;

        enchants.put(Enchantment.EFFICIENCY, this.maxLevel);
    }

    /**
     * Implements limit for enchanting with Anvils
     */
    @EventHandler(ignoreCancelled = true)
    private void onAnvilEnchant(final PrepareAnvilEvent event) {
        if (!this.enabled) return;

        final ItemStack result = event.getResult();
        if (!this.plugin.isCustomTool(result)) return;

        //Limit level to the max level
        assert result != null;
        if (result.getEnchantmentLevel(Enchantment.EFFICIENCY) <= this.maxLevel) return;
        result.removeEnchantment(Enchantment.EFFICIENCY);
        result.addEnchantment(Enchantment.EFFICIENCY, this.maxLevel);

        //Send message if setting enabled
        if (this.sendMessageToPlayers) {
            for (final HumanEntity player : event.getViewers()) {
                if (!this.messageCooldownManager.isOnCooldown(player.getUniqueId())) {
                    player.sendRichMessage("<gray>[INFO] The max Efficiency level on custom tools has been set to <white>" + this.maxLevel + "</white>.</gray>");
                    this.messageCooldownManager.setCooldown(player.getUniqueId(), Duration.ofMinutes(this.messageCooldownMinutes));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onQuit(final PlayerQuitEvent event) {
        if (!this.enabled || !this.sendMessageToPlayers) return;

        this.messageCooldownManager.removeCooldown(event.getPlayer());
    }

    /**
     * Gets if this system as a whole is enabled or not
     */
    public boolean isEnabled() {
        return this.enabled;
    }
    
}
