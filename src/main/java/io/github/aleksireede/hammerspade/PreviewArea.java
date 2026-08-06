package io.github.aleksireede.hammerspade;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class PreviewArea {
    private final Player player;
    private final int distance;
    private final Material blockType;
    private final List<Location> previewLocations;

    public PreviewArea(Player player, int distance, Material blockType) {
        this.player = player;
        this.distance = distance;
        this.blockType = blockType;
        this.previewLocations = new ArrayList<>();
    }

    public void createPreview() {
        // Clear previous preview
        clearPreview();

        // Get the block the player is looking at
        RayTraceResult rayTrace = player.rayTraceBlocks(distance);
        if (rayTrace == null) return;

        assert rayTrace.getHitBlock() != null;
        rayTrace.getHitBlock().getLocation();
        Block targetBlock = rayTrace.getHitBlock();

        // Calculate preview area based on tool and block type
        calculatePreviewArea(targetBlock);

        // Display preview using particles
        displayPreview();
    }

    private void calculatePreviewArea(Block targetBlock) {
        // Simple 3x3x3 preview area around the target block
        int centerX = targetBlock.getX();
        int centerY = targetBlock.getY();
        int centerZ = targetBlock.getZ();

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                    Location location = new Location(targetBlock.getWorld(), x, y, z);
                    if (blockType == null || location.getBlock().getType() == blockType) {
                        previewLocations.add(location);
                    }
                }
            }
        }
    }

    private void displayPreview() {
        // Use Paper API to show particles for preview
        for (Location location : previewLocations) {
            if (location.getBlock().getType() != Material.AIR) {
                // Show particle effect at block location
                location.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location.add(0.5, 0.5, 0.5), 1,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1f));
            }
        }
    }

    public void clearPreview() {
        // Clear previous preview particles
        for (Location location : previewLocations) {
            if (location.getBlock().getType() != Material.AIR) {
                location.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location.add(0.5, 0.5, 0.5), 1,
                    new Particle.DustOptions(org.bukkit.Color.BLACK, 1f));
            }
        }
        previewLocations.clear();
    }
}
