package io.github.poorgrammerdev.hammer;

import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

/**
 * Handles repairing and upgrading of custom area tools.
 * @author Thomas Tran
 */
public class RepairingManager implements Listener {
    private final Hammer plugin;

    public RepairingManager(Hammer plugin) {
        this.plugin = plugin;
    }
   
    /*
     * Matching custom tools can be repaired together.
     * Mixing a custom tool with a regular tool, or with a different custom tool type, is not allowed.
     */

    /**
     * Implements custom tool repairing rules for craft repairs
     */
    @EventHandler(ignoreCancelled = true)
    public void craftRepairing(PrepareItemCraftEvent event) {
        if (!event.isRepair()) return;

        //Find the two tools being repaired together
        final ArrayList<ItemStack> tools = new ArrayList<>();
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null) {
                tools.add(ingredient);
            }
        }

        if (tools.size() != 2) return;

        final CustomToolType toolType1 = this.plugin.getCustomToolType(tools.get(0));
        final CustomToolType toolType2 = this.plugin.getCustomToolType(tools.get(1));

        if (toolType1 != null && toolType1 == toolType2) {
            final ItemStack repairedTool = this.toCustomTool(event.getInventory().getResult(), toolType1);
            event.getInventory().setResult(repairedTool);
        }
        else if (toolType1 != toolType2) {
            event.getInventory().setResult(null);
        }
    }

    /**
     * Implements custom tool repairing rules for grindstone repairs
     */
    @EventHandler(ignoreCancelled = true)
    public void grindstoneRepairing(PrepareGrindstoneEvent event) {
        final ItemStack[] ingredients = event.getInventory().getStorageContents();
        if (ingredients.length != 2) return;

        //If player is grindstoning a single item (one of them is null), ignore
        if (ingredients[0] == null ^ ingredients[1] == null) return; //TODO: why is this XOR and anvil one is inclusive OR?

        final CustomToolType toolType1 = this.plugin.getCustomToolType(ingredients[0]);
        final CustomToolType toolType2 = this.plugin.getCustomToolType(ingredients[1]);

        if (toolType1 != null && toolType1 == toolType2) {
            final ItemStack repairedTool = this.toCustomTool(event.getResult(), toolType1);
            event.setResult(repairedTool);
        }
        else if (toolType1 != toolType2) {
            event.setResult(null);
        }
    }

    /**
     * Implements custom tool repairing rules for anvil repairs
     */
    @EventHandler(ignoreCancelled = true)
    public void anvilRepairing(PrepareAnvilEvent event) {
        final ItemStack[] items = event.getInventory().getStorageContents();
        if (items.length != 2) return;

        //If player is anviling a single item (maybe renaming) -> ignore
        if (items[0] == null || items[1] == null) return;

        if (this.plugin.isSupportedBaseTool(items[0].getType()) || this.plugin.isSupportedBaseTool(items[1].getType())) return;

        final CustomToolType toolType1 = this.plugin.getCustomToolType(items[0]);
        final CustomToolType toolType2 = this.plugin.getCustomToolType(items[1]);
        if (toolType1 != toolType2) {
            event.setResult(null);
        }
    }

    /**
     * Tool should be renamed to new tier when upgraded in smithing table
     */
    @EventHandler(ignoreCancelled = true)
    public void smithingTable(PrepareSmithingEvent event) {
        final ItemStack baseTool = event.getInventory().getInputEquipment();
        final ItemStack vanillaResult = event.getResult();
        if (baseTool == null || vanillaResult == null) return;

        final CustomToolType toolType = this.plugin.getCustomToolType(baseTool);
        if (toolType == null || !toolType.matchesBaseTool(vanillaResult.getType())) return;

        final ItemStack upgradedTool = baseTool.clone().withType(vanillaResult.getType());
        this.copyDamage(baseTool, upgradedTool);
        this.refreshToolAppearance(upgradedTool, toolType, baseTool);
        event.setResult(upgradedTool);
    }

    /**
     * Used in crafting and grindstone repairs to convert the output to a custom tool.
     * Anvil repairs preserve NBT so this is not needed there.
     * @param result The regular non-custom resulting item from repairing two matching custom tools together
     * @return A custom tool of the same mineral tier as the result item, with the correct durability
     */
    private ItemStack toCustomTool(final ItemStack result, final CustomToolType toolType) {
        //Convert the final result to a hammer but collect the damage first
        if (result != null && result.getItemMeta() instanceof Damageable meta) {
            final ItemStack customTool = this.plugin.createCustomTool(result.getType(), toolType);

            //Set the damage accordingly
            if (customTool != null && customTool.getItemMeta() instanceof Damageable customMeta) {

                customMeta.setDamage(meta.getDamage());
                customTool.setItemMeta(customMeta);
                return customTool;
            }
        }
        return null;
    }

    private void refreshToolAppearance(final ItemStack result, final CustomToolType toolType, final ItemStack sourceTool) {
        final ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) return;

        final ItemMeta sourceMeta = sourceTool.getItemMeta();
        final String expectedName = this.plugin.getToolName(result.getType(), toolType);
        final Component previousDisplayName = sourceMeta == null ? null : sourceMeta.displayName();
        final String previousName = previousDisplayName == null ? null : Text.plain(previousDisplayName);
        final String previousExpectedName = this.plugin.getToolName(sourceTool.getType(), toolType);

        if (previousName == null || previousName.equals(previousExpectedName)) {
            resultMeta.displayName(Text.miniMessage("<!italic><white>" + expectedName));
        }

        if (this.plugin.getConfig().getBoolean("write_description", true)) {
            resultMeta.lore(Collections.singletonList(Text.miniMessage("<!italic><gray>" + expectedName)));
        }

        result.setItemMeta(resultMeta);
    }

    private void copyDamage(final ItemStack from, final ItemStack to) {
        if (!(from.getItemMeta() instanceof Damageable fromMeta) || !(to.getItemMeta() instanceof Damageable toMeta)) return;

        toMeta.setDamage(fromMeta.getDamage());
        to.setItemMeta(toMeta);
    }

}
