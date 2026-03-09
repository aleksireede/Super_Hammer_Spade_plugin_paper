package io.github.aleksireede.hammerspade.common;

import io.github.aleksireede.hammerspade.Hammer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

public class config {
    public static FileConfiguration config;

    private static String getMetadataKeyName(String path, String fallback) {
        return Hammer.getInstance().getConfig().getString("metadata-keys." + path, fallback);
    }

    public static NamespacedKey customIdKey() {
        return new NamespacedKey(Hammer.getInstance(), getMetadataKeyName("custom-id", "custom_id"));
    }
}