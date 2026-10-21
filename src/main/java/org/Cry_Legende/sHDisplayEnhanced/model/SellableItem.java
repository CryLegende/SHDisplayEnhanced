package org.Cry_Legende.sHDisplayEnhanced.model;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellableItem {

    private final String id;
    private final CollectionItem linkedCollectionItem;
    private final List<String> commands;
    private final List<String> sellPriceLore;

    public SellableItem(String id, CollectionItem linkedCollectionItem, List<String> commands, List<String> sellPriceLore) {
        this.id = id;
        this.linkedCollectionItem = linkedCollectionItem;
        this.commands = commands;
        this.sellPriceLore = sellPriceLore;
    }

    /**
     * NOUVEAU : Retourne l'item de base, sans le lore de prix.
     * C'est cet item qu'on cherchera et retirera de l'inventaire du joueur.
     *
     * @return L'ItemStack de base de la décoration.
     */
    public ItemStack getRequiredItem() {
        return linkedCollectionItem.getItemStack();
    }

    /**
     * MODIFIÉ : Retourne un NOUVEL item avec le lore du prix ajouté pour l'affichage.
     *
     * @return L'ItemStack à afficher dans le menu de vente.
     */
    public ItemStack getDisplayItem() {
        ItemStack item = linkedCollectionItem.getItemStack(); // On part de l'item de base
        if (sellPriceLore == null || sellPriceLore.isEmpty()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> newLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            sellPriceLore.forEach(line -> newLore.add(ChatColor.translateAlternateColorCodes('&', line)));
            meta.setLore(newLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void executeSellCommands(Player player) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        commands.forEach(command -> {
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }
}
