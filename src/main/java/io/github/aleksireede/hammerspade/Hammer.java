package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammershared.SharedItemTextStyle;
import io.github.aleksireede.hammershared.SharedItemUpdater;
import io.github.aleksireede.hammershared.SharedResourcePackManager;
import io.github.aleksireede.hammershared.SharedText;
import io.github.aleksireede.hammerspade.common.ResourcePackListener;
import io.github.aleksireede.hammerspade.common.config;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Hammer extends JavaPlugin {
    private static Hammer instance;
    private final NamespacedKey hammerKey;
    private final NamespacedKey spadeKey;

    public Hammer() {
        this.hammerKey = new NamespacedKey(this, "is_hammer");
        this.spadeKey = new NamespacedKey(this, "is_spade");
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;

        loadConfiguration();
        registerItemLore();

        SharedResourcePackManager resourcePackManager = createResourcePackManager();

        Random random = new Random();

        RecipeMaps recipes = registerRecipes();
        registerUpgradeRecipes(recipes);

        FauxBlockDamage fauxBlockDamage = setupFauxBlockDamage(random);
        setupBlockOutlineHighlighter();

        registerListeners(resourcePackManager, recipes, fauxBlockDamage, random);
        registerCommands(recipes);
    }

    /* Load config.yml */
    private void loadConfiguration() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        SharedItemTextStyle.fromConfig(getConfig());
    }

    /* Custom Lore */
    private void registerItemLore() {
        // Hammer
        SharedItemUpdater.registerLore(10, () -> ItemLore.tool_lore("Mine"));

        // Spade
        SharedItemUpdater.registerLore(11, () -> ItemLore.tool_lore("Dig"));
    }

    /* Resource pack sender */
    private SharedResourcePackManager createResourcePackManager() {
        SharedResourcePackManager resourcePackManager =
                SharedResourcePackManager.fromConfig(this, getConfig());

        resourcePackManager.logState();

        return resourcePackManager;
    }

    /* Crafting table recipes*/
    private RecipeMaps registerRecipes() {
        CraftingManager craftingManager = new CraftingManager(this);

        HashMap<Material, NamespacedKey> hammerRecipes =
                craftingManager.registerRecipes(CustomToolType.HAMMER);

        HashMap<Material, NamespacedKey> spadeRecipes =
                craftingManager.registerRecipes(CustomToolType.SPADE);

        return new RecipeMaps(hammerRecipes, spadeRecipes);
    }

    /* Netherite upgrade recipes */
    private void registerUpgradeRecipes(RecipeMaps recipes) {
        UpgradeManager upgradeManager = new UpgradeManager(this);

        HashMap<Material, NamespacedKey> hammerUpgrades =
                upgradeManager.registerUpgradeRecipes(
                        CustomToolType.HAMMER
                );

        HashMap<Material, NamespacedKey> spadeUpgrades =
                upgradeManager.registerUpgradeRecipes(
                        CustomToolType.SPADE
                );

        recipes.hammerRecipes().putAll(hammerUpgrades);
        recipes.spadeRecipes().putAll(spadeUpgrades);
    }

    private FauxBlockDamage setupFauxBlockDamage(Random random) {
        FauxBlockDamage fauxBlockDamage = new FauxBlockDamage(this, random);

        if (fauxBlockDamage.isEnabled()) {
            fauxBlockDamage.runTaskTimer(this, 0, 0);
            getServer().getPluginManager().registerEvents(fauxBlockDamage, this);
        }

        return fauxBlockDamage;
    }

    /* Highlight block area outline with custom particles */
    private void setupBlockOutlineHighlighter() {
        BlockOutlineHighlighter outlineHighlighter =
                new BlockOutlineHighlighter(this);

        outlineHighlighter.runTaskTimer(this, 0, 2);
    }

    private void registerListeners(
            SharedResourcePackManager resourcePackManager,
            RecipeMaps recipes,
            FauxBlockDamage fauxBlockDamage,
            Random random
    ) {
        getServer().getPluginManager().registerEvents(
                new HammerMechanism(this, random, fauxBlockDamage),
                this
        );

        getServer().getPluginManager().registerEvents(
                new RepairingManager(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new RecipeManager(
                        recipes.hammerRecipes(),
                        recipes.spadeRecipes()
                ),
                this
        );

        if (resourcePackManager.isEnabled()
                && resourcePackManager.shouldSendOnJoin()) {

            getServer().getPluginManager().registerEvents(
                    new ResourcePackListener(resourcePackManager),
                    this
            );
        }
    }

    /* Give command */
    private void registerCommands(RecipeMaps recipes) {
        GiveCommand giveCommand = new GiveCommand(
                this,
                recipes.hammerRecipes(),
                recipes.spadeRecipes()
        );

        registerCommand(
                "givehammer",
                "Give players hammers or spades.",
                List.of(),
                giveCommand
        );
    }


    @Override
    public void onDisable() {
        if (instance == this) {
            instance = null;
        }
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
        if (type.matchesBaseTool(baseTool)) return null;
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

    /**
     * Maps tool material tier to a rarity level for display purposes.
     */
    private String getRarityForMaterial(final Material material) {
        final String tier = material.toString().split("_")[0].toLowerCase();
        return switch (tier) {
            case "stone", "golden" -> "Uncommon";
            case "iron" -> "Rare";
            case "diamond" -> "Epic";
            case "netherite" -> "Legendary";
            default -> "Common";
        };
    }

    public String getToolName(final Material type, final CustomToolType toolType) {
        String tier = type.toString().split("_")[0];

        return tier.charAt(0) + tier.substring(1).toLowerCase() + " " + toolType.getDisplayName();
    }

    public NamespacedKey getItemModelKey(final Material baseTool, final CustomToolType type) {
        return new NamespacedKey(this, this.cleanString(this.getToolName(baseTool, type)).toLowerCase(Locale.ROOT));
    }

    /* String helper */
    private String cleanString(final String value) {
        return value
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9/._-]", "");
    }

    public NamespacedKey getToolKey(final CustomToolType type) {
        return switch (type) {
            case HAMMER -> this.hammerKey;
            case SPADE -> this.spadeKey;
        };
    }

    public boolean isUnsupportedBaseTool(final Material material) {
        return CustomToolType.HAMMER.matchesBaseTool(material) && CustomToolType.SPADE.matchesBaseTool(material);
    }

    private record RecipeMaps(
            HashMap<Material, NamespacedKey> hammerRecipes,
            HashMap<Material, NamespacedKey> spadeRecipes
    ) {
    }
}
