package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammershared.SharedItemUpdater;
import io.github.aleksireede.hammershared.SharedText;
import io.github.aleksireede.hammerspade.common.ResourcePackListener;
import io.github.aleksireede.hammerspade.common.config;
import io.github.aleksireede.hammershared.SharedItemTextStyle;
import io.github.aleksireede.hammershared.SharedResourcePackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

public class Hammer extends JavaPlugin {
    private static Hammer instance;
    private final NamespacedKey hammerKey;
    private final NamespacedKey spadeKey;
    private SharedItemTextStyle itemTextStyle;

    public Hammer() {
        this.hammerKey = new NamespacedKey(this, "is_hammer");
        this.spadeKey = new NamespacedKey(this, "is_spade");
    }

    public static @NotNull Hammer getInstance() {
        return Objects.requireNonNull(instance, "Hammer plugin instance is not initialized yet.");
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.itemTextStyle = SharedItemTextStyle.fromConfig(this.getConfig());

        // Register item-specific description lore for hammer (10) and spade (11)
        SharedItemUpdater.registerLore(10, ItemLore::hammer_lore);
        SharedItemUpdater.registerLore(11, ItemLore::spade_lore);

        final SharedResourcePackManager resourcePackManager = SharedResourcePackManager.fromConfig(this, this.getConfig());
        resourcePackManager.logState();

        final Random random = new Random();
        final CraftingManager craftingManager = new CraftingManager(this);
        final HashMap<Material, NamespacedKey> hammerRecipeKeyMap = craftingManager.registerRecipes(CustomToolType.HAMMER);
        final HashMap<Material, NamespacedKey> spadeRecipeKeyMap = craftingManager.registerRecipes(CustomToolType.SPADE);
        this.logToolItemModels();

        final FauxBlockDamage fauxBlockDamage = new FauxBlockDamage(this, random);
        if (fauxBlockDamage.isEnabled()) {
            fauxBlockDamage.runTaskTimer(this, 0, 0);
            this.getServer().getPluginManager().registerEvents(fauxBlockDamage, this);
        }

        final EfficiencyLimiter efficiencyLimiter = new EfficiencyLimiter(this);
        if (efficiencyLimiter.isEnabled()) {
            this.getServer().getPluginManager().registerEvents(efficiencyLimiter, this);
        }

        this.getServer().getPluginManager().registerEvents(new HammerMechanism(this, random, fauxBlockDamage), this);
        this.getServer().getPluginManager().registerEvents(new RepairingManager(this), this);
        this.getServer().getPluginManager().registerEvents(new RecipeManager(hammerRecipeKeyMap, spadeRecipeKeyMap), this);
        if (resourcePackManager.isEnabled() && resourcePackManager.shouldSendOnJoin()) {
            this.getServer().getPluginManager().registerEvents(new ResourcePackListener(resourcePackManager), this);
        }

        GiveCommand giveCommand = new GiveCommand(this, hammerRecipeKeyMap, spadeRecipeKeyMap);
        this.registerCommand("givehammer", "Give players hammers or spades.", List.of(), giveCommand);
    }

    @Override
    public void onDisable() {
        if (instance == this) {
            instance = null;
        }
    }

    public boolean isCustomTool(final ItemStack item) {
        return this.getCustomToolType(item) != null;
    }

    public boolean isCustomTool(final ItemStack item, final CustomToolType type) {
        if (item == null || item.getType() == Material.AIR) return false;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().getOrDefault(this.getToolKey(type), PersistentDataType.BOOLEAN, false);
    }

    public CustomToolType getCustomToolType(final ItemStack item) {
        for (final CustomToolType type : CustomToolType.values()) {
            if (this.isCustomTool(item, type)) {
                return type;
            }
        }

        return null;
    }

    public ItemStack createCustomTool(final Material baseTool, final CustomToolType type) {
        if (!type.matchesBaseTool(baseTool)) return null;
        final String displayName = this.getToolName(baseTool, type);
        final String rarity = this.getRarityForMaterial(baseTool);
        final String itemType = type.getDisplayName();

        final ItemBuilder builder = new ItemBuilder(baseTool)
                .setItemModel(this.getItemModelKey(baseTool, type))
                .setName(SharedText.miniMessage(SharedItemUpdater.getColorFromRarity(rarity) + "<!italic>" + displayName))
                .setPersistentData(this.getToolKey(type), PersistentDataType.BOOLEAN, true)
                .setPersistentData(config.customIdKey(), PersistentDataType.INTEGER,
                        type == CustomToolType.HAMMER ? 10 : 11)
                .setPersistentData(config.rarityKey(), PersistentDataType.STRING, rarity.toLowerCase())
                .setPersistentData(config.itemTypeKey(), PersistentDataType.STRING, itemType);

        final ItemStack item = builder.build();
        SharedItemUpdater.updateChecker(item);
        return item;
    }

    /** Maps tool material tier to a rarity level for display purposes. */
    private String getRarityForMaterial(final Material material) {
        final String tier = material.toString().split("_")[0].toLowerCase();
        return switch (tier) {
            case "stone"     -> "Uncommon";
            case "iron"      -> "Rare";
            case "golden"    -> "Uncommon";
            case "diamond"   -> "Epic";
            case "netherite" -> "Legendary";
            default          -> "Common";
        };
    }

    public String getToolName(final Material type, final CustomToolType toolType) {
        String tier = type.toString().split("_")[0];

        return tier.charAt(0) + tier.substring(1).toLowerCase() + " " + toolType.getDisplayName();
    }

    public NamespacedKey getItemModelKey(final Material baseTool, final CustomToolType type) {
        return new NamespacedKey(this, this.cleanString(this.getToolName(baseTool, type)).toLowerCase(Locale.ROOT));
    }

    private String cleanString(final String value) {
        return value
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9/._-]", "");
    }

    private void logToolItemModels() {
        for (final CustomToolType type : CustomToolType.values()) {
            Stream.of(Material.values())
                    .filter(type::matchesBaseTool)
                    .forEach(material -> this.getLogger().info(
                            this.getToolName(material, type) + " item model: " + this.getItemModelKey(material, type)
                    ));
        }
    }

    public NamespacedKey getToolKey(final CustomToolType type) {
        return switch (type) {
            case HAMMER -> this.hammerKey;
            case SPADE -> this.spadeKey;
            default -> throw new IllegalArgumentException("Unsupported tool type: " + type);
        };
    }

    public boolean isUnsupportedBaseTool(final Material material) {
        return !CustomToolType.HAMMER.matchesBaseTool(material) && !CustomToolType.SPADE.matchesBaseTool(material);
    }
}
