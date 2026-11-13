package io.github.aleksireede.hammerspade;

import java.util.Random;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Implements the hammer area mining mechanism
 * @author Thomas Tran
 */
public class HammerMechanism implements Listener {
    //Constants
    private static final float BLOCK_BREAK_EXHAUSTION = 0.005f; //Source: Minecraft Wiki, 2023
    private static final int NORTH_SOUTH = 0;
    private static final int EAST_WEST = 1;
    private static final int UP_DOWN = 2;
    private static final Set<Material> PATHABLE_BLOCKS = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.MYCELIUM,
            Material.ROOTED_DIRT
    );

    private final Hammer plugin;
    private final Random random;
    private final FauxBlockDamage fauxBlockDamage;
    private final org.bukkit.NamespacedKey drillCooldownKey;

    private final Vector[][] planeOffsets;
    
    //Config Options
    private final double raytraceDistance;
    private final float hardnessBuffer;
    private final float exhaustionMultiplier;

    public HammerMechanism(final Hammer plugin, final Random random, final FauxBlockDamage fauxBlockDamage) {
        this.plugin = plugin;
        this.random = random;
        this.fauxBlockDamage = fauxBlockDamage;
        this.drillCooldownKey = new org.bukkit.NamespacedKey(plugin, "drill_bedrock_cooldown_until");

        //Load options from config
        this.raytraceDistance = plugin.getConfig().getDouble("raytrace_distance", 10.0);
        this.hardnessBuffer = (float) plugin.getConfig().getDouble("hardness_buffer", 3.0);
        this.exhaustionMultiplier = (float) plugin.getConfig().getDouble("exhaustion_multiplier", 2.0);

        //Populate baked-in values for the adjacent offsets
        this.planeOffsets = new Vector[3][];
        this.planeOffsets[NORTH_SOUTH] = new Vector[]{
            new Vector(0, 1, 0),
            new Vector(0, -1, 0),
            new Vector(1, 0, 0),
            new Vector(1, 1, 0),
            new Vector(1, -1, 0),
            new Vector(-1, 0, 0),
            new Vector(-1, 1, 0),
            new Vector(-1, -1, 0),
        };

        this.planeOffsets[EAST_WEST] = new Vector[]{
            new Vector(0, 1, 0),
            new Vector(0, -1, 0),
            new Vector(0, 0, 1),
            new Vector(0, 1, 1),
            new Vector(0, -1, 1),
            new Vector(0, 0, -1),
            new Vector(0, 1, -1),
            new Vector(0, -1, -1),
        };

        this.planeOffsets[UP_DOWN] = new Vector[]{
            new Vector(1, 0, 0),
            new Vector(-1, 0, 0),
            new Vector(0, 0, 1),
            new Vector(1, 0, 1),
            new Vector(-1, 0, 1),
            new Vector(0, 0, -1),
            new Vector(1, 0, -1),
            new Vector(-1, 0, -1),
        };
    }

    /**
     * Breaks blocks in a 3x3 flat plane when mining with the hammer
     */
    @EventHandler(ignoreCancelled = true)
    public void useHammer(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final boolean creativeMode = (player.getGameMode() == GameMode.CREATIVE);
        final Block targetBlock = event.getBlock();

        this.fauxBlockDamage.deactivate(player, targetBlock);
        if (player.isSneaking()) return;

        final EntityEquipment equipment = player.getEquipment();

        final ItemStack tool = equipment.getItemInMainHand();
        final CustomToolType toolType = this.plugin.getCustomToolType(tool);
        if (toolType == null) return;
        if (toolType == CustomToolType.DRILL) {
            if (targetBlock.getType() == Material.BEDROCK) return;
            useDrillArea(player, targetBlock, tool, creativeMode);
            return;
        }

        final ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageableMeta)) return;

        if (!canBlockActivateAbility(targetBlock, tool, toolType)) return;

        // Get plane of blocks to break in
        final int planeIndex = getPlaneIndex(player, targetBlock);
        if (planeIndex == -1) return;

        final Location middleLocation = targetBlock.getLocation();
        final float hardness = targetBlock.getType().getHardness() + this.hardnessBuffer;

        //Hammer information
        final int unbreaking = damageableMeta.getEnchantLevel(Enchantment.UNBREAKING);
        final int maxDamage = tool.getType().getMaxDurability() - 1;
        int damage = damageableMeta.getDamage();
        
        //Loop through blocks in plane and break them if they can be broken
        int count = 0;
        for (final Vector offset : this.planeOffsets[planeIndex]) {
            //If tool is broken, stop area mining early
            if (damage >= maxDamage) break;

            //Get location of current block
            targetBlock.getLocation(middleLocation);
            final Block adjacent = middleLocation.add(offset).getBlock();

            if (isBlockAreaMineable(adjacent, tool, hardness, toolType)) {
                //Break block -- do not drop items if dug in creative mode
                adjacent.breakNaturally(!creativeMode ? tool : new ItemStack(Material.AIR));

                //Add damage to tool with respect to Unbreaking enchantment
                //(Unbreaking calculation is derived from Minecraft Wiki as of 2023)
                if ((!meta.isUnbreakable() && !creativeMode) && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
                    ++damage;
                }
                
                ++count;
            }
        }

        //No need to continue if in creative mode -- the remaining parts only affect survival
        if (creativeMode) return;

        //Check if tool should be broken
        if (damage >= maxDamage) {
            //Play SFX & VFX for tool breaking
            final World world = player.getWorld();
            world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            world.spawnParticle(Particle.ITEM, player.getEyeLocation().subtract(0, 0.25, 0).add(player.getEyeLocation().getDirection().normalize().multiply(0.5f)), 15, 0.05, 0.01, 0.05, 0.1, tool);

            //Break tool
            tool.setAmount(tool.getAmount() - 1);
        }
        //Otherwise commit damage to tool
        else {
            damageableMeta.setDamage(damage);
            tool.setItemMeta(damageableMeta);
        }

        //Apply exhaustion/hunger cost to player
        player.setExhaustion(player.getExhaustion() + (count * BLOCK_BREAK_EXHAUSTION * this.exhaustionMultiplier));
    }

    /**
     * Shows block damage (cracking) effect on adjacent blocks while mining
     */
    @EventHandler(ignoreCancelled = true)
    public void adjacentBlockCracking(final BlockDamageEvent event) {
        if (event.getInstaBreak()) return;

        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        final ItemStack tool = event.getItemInHand();
        final CustomToolType toolType = this.plugin.getCustomToolType(tool);
        if (toolType == null) return;

        final Block targetBlock = event.getBlock();
        if (!canBlockActivateAbility(targetBlock, tool, toolType)) return;

        // Get plane of blocks the player wants to break
        final int planeIndex = getPlaneIndex(player, targetBlock);
        if (planeIndex == -1) return;

        final Location location = targetBlock.getLocation();
        final float hardness = targetBlock.getType().getHardness() + this.hardnessBuffer;

        //Registers the player into the system and then updates the adjacent blocks
        final FauxDamageData data = this.fauxBlockDamage.register(player, targetBlock);
        if (data == null) return; //System is disabled (or something else went wrong)

        int i = 0;
        for (final Vector offset : this.planeOffsets[planeIndex]) {
            targetBlock.getLocation(location);
            location.add(offset);

            //Calculate block location if hammerable
            final Block adjacentBlock = location.getBlock();
            if (isBlockAreaMineable(adjacentBlock, tool, hardness, toolType)) {
                data.setAdjacentBlock(i, adjacentBlock);
                i++;
            }
        }

        data.adjacentCount = i;
    }

    /** Lets a drill finish mining bedrock, whose vanilla hardness is unbreakable. */
    @EventHandler(ignoreCancelled = true)
    public void drillBedrock(final BlockDamageEvent event) {
        if (event.getBlock().getType() != Material.BEDROCK) return;
        if (this.plugin.getCustomToolType(event.getItemInHand()) != CustomToolType.DRILL) return;
        event.setInstaBreak(true);
    }

    private void useDrillArea(Player player, Block targetBlock, ItemStack tool, boolean creativeMode) {
        if (player.isSneaking()) return;
        int plane = getPlaneIndex(player, targetBlock);
        if (plane == -1) return;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;
        int height = meta.getPersistentDataContainer().getOrDefault(
                this.plugin.getDrillHeightKey(), PersistentDataType.INTEGER, 3);
        int unbreaking = damageable.getEnchantLevel(Enchantment.UNBREAKING);
        int damage = damageable.getDamage();
        int maxDamage = tool.getType().getMaxDurability() - 1;
        int count = 0;
        int start = -(height / 2);
        // Use a centered square grid so mining and the preview use the same 3x3/6x6 area.
        for (int y = start; y < start + height; y++) {
            for (int x = start; x < start + height; x++) {
                if (damage >= maxDamage) break;
                if (x == 0 && y == 0) continue;
                Vector offset = drillOffset(plane, x, y);
                Block adjacent = targetBlock.getRelative(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ());
                if (adjacent.getType() == Material.BEDROCK) continue;
                if (!isBlockAreaMineable(adjacent, tool, targetBlock.getType().getHardness() + this.hardnessBuffer, CustomToolType.DRILL)) continue;
                adjacent.breakNaturally(!creativeMode ? tool : new ItemStack(Material.AIR));
                count++;
                if (!meta.isUnbreakable() && !creativeMode && (unbreaking == 0 || random.nextInt(unbreaking + 1) == 0)) damage++;
            }
        }
        if (creativeMode) return;
        if (damage >= maxDamage) tool.setAmount(tool.getAmount() - 1);
        else { damageable.setDamage(damage); tool.setItemMeta(damageable); }
        player.setExhaustion(player.getExhaustion() + count * BLOCK_BREAK_EXHAUSTION * this.exhaustionMultiplier);
    }

    private Vector drillOffset(int plane, int width, int y) {
        return switch (plane) {
            case NORTH_SOUTH -> new Vector(width, y, 0);
            case EAST_WEST -> new Vector(0, y, width);
            default -> new Vector(width, 0, y);
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void enforceDrillCooldown(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEDROCK) return;
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (this.plugin.getCustomToolType(tool) != CustomToolType.DRILL) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long until = player.getPersistentDataContainer().getOrDefault(this.drillCooldownKey, PersistentDataType.LONG, 0L);
        if (now < until) {
            event.setCancelled(true);
            player.sendMessage("Your drill is cooling down for " + ((until - now + 59_999) / 60_000) + " more minute(s).");
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        String rarity = meta == null ? "common" : meta.getPersistentDataContainer().getOrDefault(
                io.github.aleksireede.hammerspade.common.config.rarityKey(), PersistentDataType.STRING, "common");
        long minutes = switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "uncommon" -> 45; case "rare" -> 30; case "epic" -> 15; case "legendary" -> 5; default -> 60;
        };
        player.getPersistentDataContainer().set(this.drillCooldownKey, PersistentDataType.LONG, now + minutes * 60_000L);
    }

    @EventHandler(ignoreCancelled = true)
    public void toggleDrillHeight(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack tool = event.getItem();
        if (this.plugin.getCustomToolType(tool) != CustomToolType.DRILL) return;
        assert tool != null;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;
        int current = meta.getPersistentDataContainer().getOrDefault(this.plugin.getDrillHeightKey(), PersistentDataType.INTEGER, 3);
        int next = current >= 3 && current < 6 ? current + 1 : 3;
        meta.getPersistentDataContainer().set(this.plugin.getDrillHeightKey(), PersistentDataType.INTEGER, next);
        tool.setItemMeta(meta);
        event.setCancelled(true);
        event.getPlayer().sendMessage("Drill mining area: " + next + "x" + next + ".");
    }

    @EventHandler(ignoreCancelled = true)
    public void dropDrillBedrock(final BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEDROCK) return;
        if (this.plugin.getCustomToolType(event.getPlayer().getInventory().getItemInMainHand()) != CustomToolType.DRILL) return;

        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                new ItemStack(Material.BEDROCK)
        );
    }

    /**
     * Stops displaying adjacent breaking effects to the player if they stop mining
     */
    @EventHandler(ignoreCancelled = true)
    public void stopDigging(BlockDamageAbortEvent event) {
        // No need to check if they are using a hammer here because
        // (1) they can swap off of it
        // (2) if they weren't using it they wouldn't have been registered in the first place so this would do nothing
        this.fauxBlockDamage.deactivate(event.getPlayer(), event.getBlock());
    }

    /**
     * Creates a 3x3 dirt path area when using a custom spade on valid blocks.
     */
    @EventHandler(ignoreCancelled = true)
    public void useSpadePathing(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        final ItemStack tool = event.getItem();
        if (this.plugin.getCustomToolType(tool) != CustomToolType.SPADE) return;
        if (event.getBlockFace() != BlockFace.UP) return;
        if (canFlattenToPath(clickedBlock)) return;

        assert tool != null;
        final ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageableMeta)) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        final boolean creativeMode = player.getGameMode() == GameMode.CREATIVE;
        final int unbreaking = damageableMeta.getEnchantLevel(Enchantment.UNBREAKING);
        final int maxDamage = tool.getType().getMaxDurability() - 1;
        int damage = damageableMeta.getDamage();
        int flattenedCount = 0;

        final Location center = clickedBlock.getLocation();
        final World world = clickedBlock.getWorld();

        outer:
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (damage >= maxDamage) break outer;

                final Block target = world.getBlockAt(center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                if (canFlattenToPath(target)) continue;

                target.setType(Material.DIRT_PATH, true);
                flattenedCount++;

                if ((!meta.isUnbreakable() && !creativeMode) && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
                    damage++;
                }
            }
        }

        if (flattenedCount == 0) return;

        world.playSound(center, Sound.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0f, 1.0f);

        if (creativeMode) return;

        if (damage >= maxDamage) {
            world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            world.spawnParticle(Particle.ITEM, player.getEyeLocation().subtract(0, 0.25, 0).add(player.getEyeLocation().getDirection().normalize().multiply(0.5f)), 15, 0.05, 0.01, 0.05, 0.1, tool);
            tool.setAmount(tool.getAmount() - 1);
        } else {
            damageableMeta.setDamage(damage);
            tool.setItemMeta(damageableMeta);
        }
    }

    /**
     * Ray casts to find target block face and returns matching plane index
     * @param player player using the hammer
     * @param targetBlock center block the player is using the hammer on
     * @return plane index or -1 if failure
     */
    private int getPlaneIndex(final Player player, final Block targetBlock) {
        //Ray cast out from the player to find the face of the block that they're targeting
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(this.raytraceDistance);
        if (rayTraceResult == null || rayTraceResult.getHitBlock() == null || !rayTraceResult.getHitBlock().equals(targetBlock)) return -1;

        final BlockFace blockFace = rayTraceResult.getHitBlockFace();
        if (blockFace == null) return -1;

        //Get the plane of blocks to break
        return switch (blockFace) {
            case NORTH, SOUTH -> NORTH_SOUTH;
            case EAST, WEST -> EAST_WEST;
            case UP, DOWN -> UP_DOWN;
            default -> -1;
        };
    }

    /**
     * Checks if an adjacent block can be broken by the hammer's area mining ability
     * @param block Block to check for viability
     * @param tool Hammer
     * @param centerHardness Hardness value to compare against (with the offset already added)
     * @return if the adjacent block can be broken by the hammer
     */
    private boolean isBlockAreaMineable(
        final Block block,
        final ItemStack tool,
        final float centerHardness,
        final CustomToolType toolType
    ) {
        return (
            canBlockActivateAbility(block, tool, toolType) &&
            block.getType().getHardness() <= centerHardness // Hardness value is within range of the center block
        );
    }

    /**
     * Checks if a center block can activate the hammer's ability
     * @param block Block to check for viability
     * @param tool Hammer
     * @return if the block can activate the hammer's breaking ability
     */
    private boolean canBlockActivateAbility(final Block block, final ItemStack tool, final CustomToolType toolType) {
        if (!toolType.canMine(block, tool)) return false;
        return block.getType() != Material.BEDROCK || toolType == CustomToolType.DRILL;
    }

    private boolean canFlattenToPath(final Block block) {
        if (!PATHABLE_BLOCKS.contains(block.getType())) return true;

        final Block above = block.getRelative(BlockFace.UP);
        return !above.getType().isAir();
    }

}
