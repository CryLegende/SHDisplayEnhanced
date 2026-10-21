package org.Cry_Legende.sHDisplayEnhanced.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

/**
 * Représente un item spécifique requis pour une collection.
 */
public class CollectionItem {

    private final String id;
    private final int slot;
    private final ItemStack item;

    public CollectionItem(String id, ConfigurationSection config) {
        this.id = id;
        this.slot = config.getInt("slot");
        this.item = createItemFromConfig(config.getConfigurationSection("item"));
    }

    private ItemStack createItemFromConfig(ConfigurationSection itemConfig) {
        if (itemConfig == null) {
            // Log une erreur ou retourne un item par défaut pour éviter les crashes
            return new ItemStack(Material.STONE, 1);
        }

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§r" + itemConfig.getString("name", "").replace('&', '§'));

            if (itemConfig.isList("lore")) {
                meta.setLore(itemConfig.getStringList("lore").stream()
                    .map(line -> "§r" + line.replace('&', '§'))
                    .collect(Collectors.toList()));
            }

            if (itemConfig.isConfigurationSection("enchantments")) {
                for (String enchantKey : itemConfig.getConfigurationSection("enchantments").getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, itemConfig.getInt("enchantments." + enchantKey), true);
                    }
                }
            }

            if (itemConfig.isList("item-flags")) {
                for (String flagStr : itemConfig.getStringList("item-flags")) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flagStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        // Ignore les flags invalides
                    }
                }
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    // Getters
    public String getId() {
        return id;
    }

    public int getSlot() {
        return slot;
    }


    public ItemStack getItemStack() {
        return item.clone(); // Retourne un clone pour éviter les modifications accidentelles
    }
}
