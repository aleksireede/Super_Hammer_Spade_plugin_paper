package io.github.aleksireede.hammerspade;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Shows a clean square block outline using end rod particles
 * Forms perfect box outlines around each block without any visible entities
 */
public class BlockOutlineHighlighter extends BukkitRunnable {
    private final Hammer plugin;

    public BlockOutlineHighlighter(Hammer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Update outline for each online player holding hammer/spade
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            CustomToolType toolType = plugin.getCustomToolType(tool);

            if (toolType == null || player.isSneaking()) {
                continue;
            }

            // Raycast to find block player is looking at
            RayTraceResult rayTrace = player.rayTraceBlocks(10.0);
            if (rayTrace == null || rayTrace.getHitBlock() == null || rayTrace.getHitBlockFace() == null) {
                continue;
            }

            Block targetBlock = rayTrace.getHitBlock();
            // Use the block face to determine plane orientation
            int planeIndex = getPlaneIndexFromFace(rayTrace.getHitBlockFace());

            if (planeIndex == -1) {
                continue;
            }

            // Get outer edge blocks
            Vector[] outerBlocks = getOuterEdgeBlocks(planeIndex);
            Location baseLocation = targetBlock.getLocation().clone();

            // Draw outline
            drawOutline(player, baseLocation, outerBlocks);
        }
    }

    private void drawOutline(Player player, Location baseLocation, Vector[] outerBlocks) {
        // Draw outline for ALL blocks in the selection, regardless of whether they can be mined
        for (Vector offset : outerBlocks) {
            Location blockLocation = baseLocation.clone().add(offset);
            drawBlockSquareOutline(blockLocation);
        }
    }

    /**
     * Get plane index from the block face being targeted
     * UP/DOWN = UP_DOWN plane
     * NORTH/SOUTH = NORTH_SOUTH plane
     * EAST/WEST = EAST_WEST plane
     */
    private int getPlaneIndexFromFace(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> 2; // UP_DOWN
            case NORTH, SOUTH -> 0; // NORTH_SOUTH
            case EAST, WEST -> 1; // EAST_WEST
            default -> -1;
        };
    }

    /**
     * Draw a clean square outline around a block using particles
     * Forms the perimeter of the block with end rod particles
     */
    private void drawBlockSquareOutline(Location blockLocation) {
        double x0 = blockLocation.getX();
        double y0 = blockLocation.getY();
        double z0 = blockLocation.getZ();
        double x1 = x0 + 1.0;
        double y1 = y0 + 1.0;
        double z1 = z0 + 1.0;

        // Use end rod particles for a clean, bright outline that doesn't fade quickly
        // Draw all 12 edges of the block

        // Bottom face (z = z0)
        drawEdge(blockLocation, new double[]{x0, y0, z0}, new double[]{x1, y0, z0}); // bottom-front
        drawEdge(blockLocation, new double[]{x1, y0, z0}, new double[]{x1, y0, z1}); // bottom-right
        drawEdge(blockLocation, new double[]{x1, y0, z1}, new double[]{x0, y0, z1}); // bottom-back
        drawEdge(blockLocation, new double[]{x0, y0, z1}, new double[]{x0, y0, z0}); // bottom-left

        // Top face (y = y1)
        drawEdge(blockLocation, new double[]{x0, y1, z0}, new double[]{x1, y1, z0}); // top-front
        drawEdge(blockLocation, new double[]{x1, y1, z0}, new double[]{x1, y1, z1}); // top-right
        drawEdge(blockLocation, new double[]{x1, y1, z1}, new double[]{x0, y1, z1}); // top-back
        drawEdge(blockLocation, new double[]{x0, y1, z1}, new double[]{x0, y1, z0}); // top-left

        // Vertical edges (connecting top and bottom)
        drawEdge(blockLocation, new double[]{x0, y0, z0}, new double[]{x0, y1, z0}); // front-left
        drawEdge(blockLocation, new double[]{x1, y0, z0}, new double[]{x1, y1, z0}); // front-right
        drawEdge(blockLocation, new double[]{x1, y0, z1}, new double[]{x1, y1, z1}); // back-right
        drawEdge(blockLocation, new double[]{x0, y0, z1}, new double[]{x0, y1, z1}); // back-left
    }

    /**
     * Draw a line of particles between two points
     */
    private void drawEdge(Location blockLocation, double[] start, double[] end) {
        double dx = end[0] - start[0];
        double dy = end[1] - start[1];
        double dz = end[2] - start[2];

        // Draw ~6 particles per edge for a solid outline
        int steps = 5;
        
        // Use REDSTONE particles with minimal lifetime for instant display/disappear
        var dustOptions = new Particle.DustOptions(
            org.bukkit.Color.fromRGB(0, 255, 255), // Cyan
            0.5f // Very small size for quick fade
        );

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start[0] + dx * t;
            double y = start[1] + dy * t;
            double z = start[2] + dz * t;

            // Use DUST particles with minimal lifetime - instant disappearance
            blockLocation.getWorld().spawnParticle(
                Particle.DUST,
                x, y, z,
                0, // count - 0 means single particle
                0, 0, 0, // no offset/velocity
                0.01,  // speed 0.01 for instant fade (particles appear then vanish immediately)
                dustOptions
            );
        }
    }

    /**
     * Get only the outer perimeter blocks for the given plane
     */
    private Vector[] getOuterEdgeBlocks(int planeIndex) {
        return switch (planeIndex) {
            case 0 -> // NORTH_SOUTH - front row, sides, back row (no center)
                new Vector[]{
                    // Front row (z=0)
                    new Vector(-1, -1, 0),
                    new Vector(0, -1, 0),
                    new Vector(1, -1, 0),
                    // Middle row (z=0) - sides only
                    new Vector(-1, 0, 0),
                    new Vector(1, 0, 0),
                    // Back row (z=0)
                    new Vector(-1, 1, 0),
                    new Vector(0, 1, 0),
                    new Vector(1, 1, 0),
                };
            case 1 -> // EAST_WEST - front row, sides, back row
                new Vector[]{
                    // Front row (z=0)
                    new Vector(0, -1, -1),
                    new Vector(0, -1, 0),
                    new Vector(0, -1, 1),
                    // Middle row (z=0) - sides only
                    new Vector(0, 0, -1),
                    new Vector(0, 0, 1),
                    // Back row (z=0)
                    new Vector(0, 1, -1),
                    new Vector(0, 1, 0),
                    new Vector(0, 1, 1),
                };
            case 2 -> // UP_DOWN - outer perimeter
                new Vector[]{
                    // Front row
                    new Vector(-1, 0, -1),
                    new Vector(0, 0, -1),
                    new Vector(1, 0, -1),
                    // Middle row - sides only
                    new Vector(-1, 0, 0),
                    new Vector(1, 0, 0),
                    // Back row
                    new Vector(-1, 0, 1),
                    new Vector(0, 0, 1),
                    new Vector(1, 0, 1),
                };
            default -> new Vector[0];
        };
    }
}

