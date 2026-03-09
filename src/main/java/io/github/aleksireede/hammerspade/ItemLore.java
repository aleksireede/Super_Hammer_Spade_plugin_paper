package io.github.aleksireede.hammerspade;

import io.github.aleksireede.hammershared.SharedText;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/** Item description lore for the custom hammer (ID 10) and spade (ID 11). */
public final class ItemLore {

    private ItemLore() {}

    public static List<Component> hammer_lore() {
        List<Component> lore = new ArrayList<>();
        lore.add(SharedText.miniMessage("<!i><gold>Ability: Area Mine"));
        lore.add(SharedText.miniMessage("<!i><white>Mines a 3×3 area of blocks simultaneously."));
        lore.add(SharedText.miniMessage("<!i><white>Hold Shift to mine a single block."));
        return lore;
    }

    public static List<Component> spade_lore() {
        List<Component> lore = new ArrayList<>();
        lore.add(SharedText.miniMessage("<!i><gold>Ability: Area Dig"));
        lore.add(SharedText.miniMessage("<!i><white>Digs a 3×3 area of blocks simultaneously."));
        lore.add(SharedText.miniMessage("<!i><white>Hold Shift to dig a single block."));
        return lore;
    }
}
