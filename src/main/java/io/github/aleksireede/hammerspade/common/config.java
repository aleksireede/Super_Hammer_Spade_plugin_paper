package io.github.aleksireede.hammerspade.common;

import io.github.aleksireede.hammershared.SharedItemKeys;
import org.bukkit.NamespacedKey;

public class config {

    public static NamespacedKey customIdKey() {
        return SharedItemKeys.customIdKey();
    }

    public static NamespacedKey rarityKey() {
        return SharedItemKeys.rarityKey();
    }

    public static NamespacedKey itemTypeKey() {
        return SharedItemKeys.itemTypeKey();
    }
}