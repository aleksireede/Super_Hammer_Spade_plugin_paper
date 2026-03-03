package io.github.poorgrammerdev.hammer;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Handles unlocking the crafting recipes for Hammers
 * @author Thomas Tran
 */
public class RecipeManager implements Listener {
    /**
     * Maps vanilla pickaxe recipe key to corresponding hammer recipe key of the same type
     * e.g. Minecraft:iron_pickaxe -> hammer:iron_hammer
     */
    private final HashMap<NamespacedKey, NamespacedKey> recipeMapper;

    /*
     * Behavior:
     * The recipe for a given hammer is unlocked at the same time as
     * when the pickaxe of the same type is unlocked.
     * 
     * So when a player unlocks the recipe for an iron pickaxe, they also unlock for iron hammer.
     */

    public RecipeManager(
        final HashMap<Material, NamespacedKey> hammerRecipeMap,
        final HashMap<Material, NamespacedKey> spadeRecipeMap
    ) {
        this.recipeMapper = new HashMap<>();

        this.addRecipeMappings(hammerRecipeMap);
        this.addRecipeMappings(spadeRecipeMap);
    }

    /**
     * Listens for corresponding pickaxe recipe unlock, and unlocks hammer of that tier
     */
    @EventHandler(ignoreCancelled = true)
    public void unlockToolRecipe(PlayerRecipeDiscoverEvent event) {
        if (!this.recipeMapper.containsKey(event.getRecipe())) return;

        final NamespacedKey recipe = this.recipeMapper.get(event.getRecipe());
        if (recipe == null) return;

        event.getPlayer().discoverRecipe(recipe);
    }

    private void addRecipeMappings(final HashMap<Material, NamespacedKey> recipeMap) {
        for (final Material tool : recipeMap.keySet()) {
            this.recipeMapper.put(tool.getKey(), recipeMap.get(tool));
        }
    }
    
}
