package org.Cry_Legende.sHDisplayEnhanced.model;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
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
        if (material == null) {
            // Un matériau invalide ferait échouer le chargement de toutes les séries
            SHDisplayEnhanced.getInstance().getLogger().warning("Matériau invalide '" + itemConfig.getString("material") + "' pour la décoration '" + id + "' (" + itemConfig.getCurrentPath() + ").");
            material = Material.BARRIER;
        }
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
                    Enchantment enchantment = enchantment(enchantKey);
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

    /** Enchantement lu en config : clé (minecraft:lure, lure) ou ancien nom (LURE, DAMAGE_ALL). */
    @SuppressWarnings("deprecation")
    private static Enchantment enchantment(String raw) {
        NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
        Enchantment found = key == null ? null : Registry.ENCHANTMENT.get(key);
        return found != null ? found : Enchantment.getByName(raw.trim().toUpperCase(Locale.ROOT));
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
