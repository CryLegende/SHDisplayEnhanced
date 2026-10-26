package org.Cry_Legende.sHDisplayEnhanced.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public class ItemUtils {

    /**
     * Compare deux ItemStacks sur la base des critères définis dans les collections.
     * Ignore la quantité et la durabilité.
     *
     * @param itemInHand   L'item que le joueur tient.
     * @param requiredItem L'item défini dans la configuration.
     * @return true si les items sont considérés comme identiques pour la collection.
     */
    public static boolean isSimilar(ItemStack itemInHand, ItemStack requiredItem) {
        if (itemInHand == null || requiredItem == null) {
            return false;
        }

        if (itemInHand.getType() != requiredItem.getType()) {
            return false;
        }

        ItemMeta handMeta = itemInHand.getItemMeta();
        ItemMeta requiredMeta = requiredItem.getItemMeta();

        if (handMeta == null || requiredMeta == null) {
            return handMeta == requiredMeta;
        }

        // Compare le nom, le lore, les enchantements et les flags.
        return handMeta.getDisplayName().equals(requiredMeta.getDisplayName()) &&
            Objects.equals(handMeta.getLore(), requiredMeta.getLore()) &&
            handMeta.getEnchants().equals(requiredMeta.getEnchants()) &&
            handMeta.getItemFlags().equals(requiredMeta.getItemFlags());
    }
}
