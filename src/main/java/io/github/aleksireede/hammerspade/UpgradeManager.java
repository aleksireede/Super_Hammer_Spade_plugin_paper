package io.github.aleksireede.hammerspade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.HashMap;

/**
 * Handles the upgrading of hammer/spade tools to netherite using a smithing table.
 * Similar to how vanilla tools are upgraded to netherite.
 */
public class UpgradeManager {
    private final Hammer plugin;

    public UpgradeManager(Hammer plugin) {
        this.plugin = plugin;
    }

    public HashMap<Material, NamespacedKey> registerUpgradeRecipes(
            final CustomToolType toolType) {
        final HashMap<Material, NamespacedKey> ret = new HashMap<>();

        // Get the diamond version of the tool
        Material diamondTool = this.getDiamondToolMaterial(toolType);
        if (diamondTool == null) {
            return ret;
        }

        // Get the netherite version of the tool
        Material netheriteTool = this.getNetheritToolMaterial(toolType);
        if (netheriteTool == null) {
            return ret;
        }

        // Create the upgrade recipe
        final String tier = "netherite";
        final NamespacedKey key = new NamespacedKey(plugin, tier.toLowerCase() + "_" + toolType.getKeySuffix());

        // Create netherite version of the tool
        final ItemStack netheriteCustomTool = plugin.createCustomTool(netheriteTool, toolType);

        // Use the diamond version as the base for upgrading
        final ItemStack diamondCustomTool = plugin.createCustomTool(diamondTool, toolType);

        // Create smithing recipe: diamond tool + netherite ingot -> netherite tool
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                netheriteCustomTool,
                new RecipeChoice.MaterialChoice(
                        Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                ),
                new RecipeChoice.ExactChoice(diamondCustomTool),
                new RecipeChoice.MaterialChoice(
                        Material.NETHERITE_INGOT
                )
        );

        plugin.getServer().addRecipe(recipe);

        ret.put(netheriteTool, key);
        return ret;
    }

    private Material getDiamondToolMaterial(CustomToolType toolType) {
        return switch (toolType) {
            case HAMMER -> Material.DIAMOND_PICKAXE;
            case SPADE -> Material.DIAMOND_SHOVEL;
        };
    }

    private Material getNetheritToolMaterial(CustomToolType toolType) {
        return switch (toolType) {
            case HAMMER -> Material.NETHERITE_PICKAXE;
            case SPADE -> Material.NETHERITE_SHOVEL;
        };
    }
}
