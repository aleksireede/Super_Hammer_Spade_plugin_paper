package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammershared.SharedText;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/** Item description lore for custom area tools. */
public final class ItemLore {

    private ItemLore() {}

    public static List<Component> tool_lore(String action) {
        List<Component> lore = new ArrayList<>();
        lore.add(SharedText.miniMessage("<!i><gold>Ability: Area " + action));
        lore.add(SharedText.miniMessage(
                "<!i><white>" + action + "s a 3×3 area of blocks simultaneously."
        ));
        lore.add(SharedText.miniMessage(
                "<!i><white>Hold Shift to " + action.toLowerCase() + " a single block."
        ));
        return lore;
    }

    public static List<Component> drill_lore() {
        List<Component> lore = new ArrayList<>();
        lore.add(SharedText.miniMessage("<!i><gold>Ability: Break bedrock"));
        lore.add(SharedText.miniMessage("<!i><white>Breaks the targeted bedrock block."));
        return lore;
    }

}
