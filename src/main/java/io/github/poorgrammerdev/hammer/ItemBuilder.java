package io.github.poorgrammerdev.hammer;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

/**
 * Utility class for easily creating custom items.
 * @author Thomas Tran
 */
public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final PersistentDataContainer container;

    public ItemBuilder (ItemStack item) throws IllegalArgumentException {
        this.item = item;
        if (item.getItemMeta() == null) throw new IllegalArgumentException("ItemMeta cannot be a null value.");

        meta = item.getItemMeta();
        container = meta.getPersistentDataContainer();
    }

    public ItemBuilder(Material material, int count) throws IllegalArgumentException {
        this(new ItemStack(material, count));
    }

    public ItemBuilder(Material material) throws IllegalArgumentException {
        this(material, 1);
    }

    public ItemBuilder setName(final Component name) {
        meta.displayName(name);
        return this;
    }

    public void setLore(final Component... lore) {
        meta.lore(Arrays.asList(lore));
    }

    public ItemBuilder setItemModel(final NamespacedKey key) {
        meta.setItemModel(key);
        return this;
    }

    public <T,Z> ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<T, Z> type, Z data) throws IllegalArgumentException {
        if (container != null) {
            container.set(key, type, data);
            return this;
        }
        throw new IllegalArgumentException("Item does not have PersistentDataContainer");
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
