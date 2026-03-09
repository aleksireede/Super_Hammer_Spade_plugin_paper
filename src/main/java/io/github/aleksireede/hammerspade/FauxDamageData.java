package io.github.aleksireede.hammerspade;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * [STRUCT-LIKE CLASS]
 * Used to hold data pertaining to block damage displaying
 */
public class FauxDamageData {
    private static final int MAX_ADJACENT_LENGTH = 8;

    /**
     * IDs to use for the block damagers (randomly generated once)
     */
    public final int[] ids;

    /**
     * Effect is active for player
     */
    public boolean active;

    /**
     * Center or target block the player is using the hammer on
     */
    public Block centerBlock;
    
    /**
     * Array of locations surrounding block in the plane
     */
    public final Location[] adjacentBlocks;

    /**
     * How many locations are valid in the adjacentBlocks array
     * The valid range is [0, adjacentCount - 1]
     * The remaining values are either null or garbage
     */
    public int adjacentCount;

    /**
     * Progression of effect, measured in elapsed ticks
     */
    public int ticks;

    public FauxDamageData(final Block centerBlock, final Random random) {
        this.centerBlock = centerBlock;
        this.ticks = 0;
        this.active = false;

        // Pre-allocate location objects so they can be mutated and re-used.
        final Location baseLocation = centerBlock.getLocation();
        this.adjacentBlocks = new Location[MAX_ADJACENT_LENGTH];
        for (int i = 0; i < this.adjacentBlocks.length; i++) {
            this.adjacentBlocks[i] = baseLocation.clone();
        }
        this.adjacentCount = 0;
        
        //Generate random id values (always max length so we don't have to regenerate)
        this.ids = new int[MAX_ADJACENT_LENGTH];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = random.nextInt();
        }
    }

    public void reset(final Block centerBlock) {
        this.centerBlock = centerBlock;
        this.ticks = 0;
        this.adjacentCount = 0;
        this.active = true;
    }

    public void setAdjacentBlock(final int index, final Block block) {
        block.getLocation(this.adjacentBlocks[index]);
    }
}
