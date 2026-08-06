package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammerspade.common.Text;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class PreviewCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        MiniMessage Minimessage = MiniMessage.builder().build();
        if (args.length < 1) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /preview <tool> [distance] [blockType]"));
            return true;
        }

        int distance = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        Material blockType = args.length > 2 ? Material.getMaterial(args[2].toUpperCase()) : null;

        // Get the tool item in player's hand
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must hold a tool in your main hand."));
        }

        // Create preview area using Paper API
        PreviewArea preview = new PreviewArea(player, distance, blockType);
        preview.createPreview();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Preview created!"));
        return false;
    }
}
