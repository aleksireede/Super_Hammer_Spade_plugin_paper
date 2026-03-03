package io.github.poorgrammerdev.hammer;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public enum CustomToolType {
    HAMMER("hammer", "Hammer") {
        @Override
        public boolean matchesBaseTool(final Material material) {
            return Tag.ITEMS_PICKAXES.isTagged(material);
        }

        @Override
        public boolean canMine(final Block block, final ItemStack tool) {
            return (
                Tag.MINEABLE_PICKAXE.isTagged(block.getType()) &&
                block.getBlockData().isPreferredTool(tool)
            );
        }

        @Override
        public String[] getRecipeShape() {
            return new String[]{
                "***",
                "*|*",
                " | "
            };
        }
    },
    SPADE("spade", "Spade") {
        @Override
        public boolean matchesBaseTool(final Material material) {
            return Tag.ITEMS_SHOVELS.isTagged(material);
        }

        @Override
        public boolean canMine(final Block block, final ItemStack tool) {
            return (
                Tag.MINEABLE_SHOVEL.isTagged(block.getType()) &&
                block.getBlockData().isPreferredTool(tool)
            );
        }

        @Override
        public String[] getRecipeShape() {
            return new String[]{
                " * ",
                "*|*",
                " | "
            };
        }
    };

    private final String keySuffix;
    private final String displayName;

    CustomToolType(final String keySuffix, final String displayName) {
        this.keySuffix = keySuffix;
        this.displayName = displayName;
    }

    public String getKeySuffix() {
        return this.keySuffix;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public abstract boolean matchesBaseTool(Material material);

    public abstract boolean canMine(Block block, ItemStack tool);

    public abstract String[] getRecipeShape();
}
