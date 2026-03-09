package io.github.aleksireede.hammerspade;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Manages cooldowns for players
 * Referencing code from this wiki post: <a href="https://www.spigotmc.org/wiki/feature-command-cooldowns">...</a>
 */
public class CooldownManager {
    private final HashMap<UUID, Instant> cooldownMap;

    public CooldownManager() {
        this.cooldownMap = new HashMap<>();
    }

    /**
     * Checks if a player is on cooldown with their UUID
     * @param playerID player id
     * @return true if player is still on cooldown and false if not
     */
    public boolean isOnCooldown(final UUID playerID) {
        final Instant cooldown = this.cooldownMap.getOrDefault(playerID, null);
        return (cooldown != null && Instant.now().isBefore(cooldown));
    }

    /**
     * Checks if a player is on cooldown
     * @param player player to check
     * @return true if player is still on cooldown and false if not
     */
    public boolean isOnCooldown(final Player player) {
        return this.isOnCooldown(player.getUniqueId());
    }

    /**
     * Sets a player on cooldown with their UUID
     * @param playerID player id
     * @param duration how long to set their cooldown for
     */
    public void setCooldown(final UUID playerID, final Duration duration) {
        this.cooldownMap.put(playerID, Instant.now().plus(duration));
    }

    /**
     * Sets a player on cooldown
     * @param player player to set
     * @param duration how long to set their cooldown for
     */
    public void setCooldown(final Player player, final Duration duration) {
        this.setCooldown(player.getUniqueId(), duration);
    }

    /**
     * Remove a player's remaining cooldown with their UUID
     *
     * @param playerID player id
     */
    public void removeCooldown(final UUID playerID) {
        this.cooldownMap.remove(playerID);
    }

    /**
     * Remove a player's remaining cooldown
     *
     * @param player player to remove
     */
    public void removeCooldown(final Player player) {
        this.removeCooldown(player.getUniqueId());
    }


}
