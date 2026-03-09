package io.github.aleksireede.hammerspade;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import org.bukkit.event.Listener;

/**
 * Handles the crafting of the different Hammer items.
 * @author Thomas Tran
 */
public class CraftingManager implements Listener {
    private final Hammer plugin;

    public CraftingManager(Hammer plugin) {
        this.plugin = plugin;
    }

    public HashMap<Material, NamespacedKey> registerRecipes(final CustomToolType toolType) {
        final HashMap<Material, NamespacedKey> ret = new HashMap<>();
        final HashMap<RecipeChoice, Material> craftMap = this.getMineralsToToolsMap(toolType);

        for (final RecipeChoice choice : craftMap.keySet()) {
            final Material resultType = craftMap.get(choice);

            final String tier = resultType.name().split("_")[0];
            final NamespacedKey key = new NamespacedKey(plugin, tier.toLowerCase() + "_" + toolType.getKeySuffix());

            final ItemStack customTool = plugin.createCustomTool(resultType, toolType);
            final ShapedRecipe recipe = new ShapedRecipe(key, customTool);
            recipe.shape(toolType.getRecipeShape());

            recipe.setIngredient('*', choice);
            recipe.setIngredient('|', Material.STICK);

            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            plugin.getServer().addRecipe(recipe);
            ret.put(resultType, key);
        }

        return ret;
    }

    private HashMap<RecipeChoice, Material> getMineralsToToolsMap(final CustomToolType toolType) {
        final HashMap<RecipeChoice, Material> craftMap = new HashMap<>();
        final Iterator<Recipe> iterator = this.plugin.getServer().recipeIterator();
        while (iterator.hasNext()) {
            final Recipe recipe = iterator.next();

            if (!(recipe instanceof ShapedRecipe shaped)) continue;
            if (!toolType.matchesBaseTool(recipe.getResult().getType())) continue;

            if (!shaped.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) continue;

            final RecipeChoice headChoice = this.findHeadChoice(shaped);
            if (headChoice == null) continue;

            craftMap.put(headChoice, shaped.getResult().getType());
        }

        return craftMap;
    }

    private RecipeChoice findHeadChoice(final ShapedRecipe recipe) {
        for (final String row : recipe.getShape()) {
            for (int i = 0; i < row.length(); ++i) {
                final char key = row.charAt(i);
                if (key == ' ') continue;

                final RecipeChoice choice = recipe.getChoiceMap().get(key);
                if (choice == null || choice.test(new ItemStack(Material.STICK))) continue;

                return choice;
            }
        }

        return null;
    }

}
