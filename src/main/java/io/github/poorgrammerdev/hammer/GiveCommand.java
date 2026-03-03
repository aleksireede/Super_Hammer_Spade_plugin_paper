package io.github.poorgrammerdev.hammer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jspecify.annotations.NonNull;

/**
 * Command to allow admins to give hammers to players
 * @author Thomas Tran
 */
public class GiveCommand implements CommandExecutor, TabCompleter {
    private final Hammer plugin;
    private final HashMap<String, ToolDefinition> toolMap;
    
    public GiveCommand(
        final Hammer plugin,
        final HashMap<Material, NamespacedKey> hammerRecipeKeyMap,
        final HashMap<Material, NamespacedKey> spadeRecipeKeyMap
    ) {
        this.plugin = plugin;
        this.toolMap = new HashMap<>();

        this.addTools(hammerRecipeKeyMap, CustomToolType.HAMMER);
        this.addTools(spadeRecipeKeyMap, CustomToolType.SPADE);
        this.toolMap.put("NETHERITE_HAMMER", new ToolDefinition(Material.NETHERITE_PICKAXE, CustomToolType.HAMMER));
        this.toolMap.put("NETHERITE_SPADE", new ToolDefinition(Material.NETHERITE_SHOVEL, CustomToolType.SPADE));
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, @NonNull String label, String @NonNull [] args) {
        // /givehammer <selector> <hammer-type> [count] [damage]
        if (!command.getName().equalsIgnoreCase("givehammer")) return false;
        if (!sender.hasPermission("hammer.givehammer")) {
            sender.sendRichMessage("<red>You do not have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendRichMessage("<red>Too few arguments.");
            return false;
        }

        // Make sure the hammer type is valid
        final String hammerType = args[1].toUpperCase();
        if (!this.toolMap.containsKey(hammerType)) {
            sender.sendRichMessage("<red>Invalid tool type.");
            return false;
        }
        final ToolDefinition toolDefinition = this.toolMap.get(hammerType);

        // Get all players represented by selector
        final List<Entity> entities;
        try {
            entities = plugin.getServer().selectEntities(sender, args[0]);
        }
        catch (IllegalArgumentException e) {
            sender.sendRichMessage("<red>Invalid selector.");
            return false;
        }

        final ArrayList<Player> players = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }

        //Make sure there is at least one player
        if (players.isEmpty()) {
            sender.sendRichMessage("<red>Selector returned no players.");
            return false;
        }

        //Get the hammer item
        final ItemStack tool = plugin.createCustomTool(toolDefinition.material, toolDefinition.type);
        if (tool == null) {
            sender.sendRichMessage("<red>Internal plugin error: failed to create tool.");
            return true;
        }

        //Adjust count if necessary
        if (args.length >= 3) {
            final int count;

            //atoi
            try {
                count = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e) {
                sender.sendRichMessage("<red>Invalid quantity, expected a number.");
                return false;
            }

            //Invalid quantity
            if (count <= 0 || count > 64) {
                sender.sendRichMessage("<red>Invalid quantity, expected a number between 1 and 64.");
                return false;
            }

            tool.setAmount(count);
        }

        //Adjust damage if necessary
        if (args.length >= 4) {
            if (!(tool.getItemMeta() instanceof Damageable meta)) {
                sender.sendRichMessage("<red>Internal plugin error.");
                return false;
            }

            final int damage;

            //atoi
            try {
                damage = Integer.parseInt(args[3]);
            }
            catch (NumberFormatException e) {
                sender.sendRichMessage("<red>Invalid damage, expected a number.");
                return false;
            }

            meta.setDamage(damage);
            tool.setItemMeta(meta);
        }
        
        //Give all specified players a custom tool
        for (final Player player : players) {
            player.getInventory().addItem(tool);

            sender.sendRichMessage(
                "<gray>Gave <white><amount></white> [<tool>]</gray> <gray>to <white><player></white></gray>",
                Placeholder.unparsed("amount", Integer.toString(tool.getAmount())),
                Placeholder.unparsed("tool", this.plugin.getToolName(toolDefinition.material, toolDefinition.type)),
                Placeholder.unparsed("player", player.getName())
            );
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, Command command, @NonNull String label, String @NonNull [] args) {
        // /givehammer <selector> <hammer-type> [count] [damage]
        if (!command.getName().equalsIgnoreCase("givehammer")) return null;
        if (!sender.hasPermission("hammer.givehammer")) return null;

        final ArrayList<String> ret = new ArrayList<>();

        //Best code I've ever written :)
        switch (args.length) {
            case 1:
                //Adds all online players that still might be typed
                ret.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> (name.toUpperCase().startsWith(args[0].toUpperCase()))).toList());
                //Adds all selectors that still might be typed
                ret.addAll(Stream.of("@a", "@p", "@r", "@s").filter(selector -> (selector.toUpperCase().startsWith(args[0].toUpperCase()))).toList());
                break;
            case 2:
                //Adds all tiers that still might be typed
                ret.addAll(this.toolMap.keySet().stream().filter(tier -> (tier.toUpperCase().startsWith(args[1].toUpperCase()))).map(String::toLowerCase).toList());
                break;
            default:
                break;
        }

        return ret;
    }

    private void addTools(final HashMap<Material, NamespacedKey> recipeKeyMap, final CustomToolType type) {
        for (final Material material : recipeKeyMap.keySet()) {
            final NamespacedKey key = recipeKeyMap.get(material);
            this.toolMap.put(key.getKey().toUpperCase(), new ToolDefinition(material, type));
        }
    }

    private record ToolDefinition(Material material, CustomToolType type) {
    }
    
}
