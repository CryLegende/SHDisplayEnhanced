package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.config.ConfigManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.PlayerManager;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectionsListGUI extends AbstractGUI {

    private final SHDisplayEnhanced plugin;
    private final PlayerManager playerManager;
    private final ConfigManager configManager;

    public CollectionsListGUI(Player player, SHDisplayEnhanced plugin) {
        super(player, 54, "Séries de Décorations"); // Titre mis à jour
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void build() {
        ItemStack filler = configManager.getFillerItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > inventory.getSize() - 10 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inventory.setItem(i, filler);
            }
        }

        Map<String, Collection> collections = plugin.getCollectionManager().getAllCollections();
        if (collections.isEmpty()) {
            inventory.setItem(22, configManager.getNoCollectionsItem());
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slotIndex = 0;

        for (Collection collection : collections.values()) {
            if (slotIndex >= slots.length) break;

            ItemStack displayItem = collection.getMenuItem().clone();
            ItemMeta meta = displayItem.getItemMeta();

            int totalItems = collection.getCollectionItems().size();
            long completedItems = playerManager.getCompletedCount(player.getUniqueId(), collection.getId());
            boolean isCompleted = playerManager.hasCompletedCollection(player.getUniqueId(), collection);
            String progress = completedItems + " / " + totalItems;
            String completedText = isCompleted ? "§aComplété" : "§cIncomplet";

            if (meta != null && meta.hasLore()) {
                List<String> newLore = meta.getLore().stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line
                        .replace("%shde_progress_this_collection%", progress)
                        .replace("%shde_completed_this_collection%", completedText)))
                    .collect(Collectors.toList());
                meta.setLore(newLore);
            }

            displayItem.setItemMeta(meta);
            int currentSlot = slots[slotIndex];
            inventory.setItem(currentSlot, displayItem);

            actions.put(currentSlot, (event) -> {
                player.playSound(player.getLocation(), configManager.getClickSound(), 1f, 1f);
                new SingleCollectionGUI(player, collection, plugin).open();
            });

            slotIndex++;
        }

        inventory.setItem(49, configManager.getBackButton());
        actions.put(49, (event) -> {
            player.playSound(player.getLocation(), configManager.getClickSound(), 1f, 1f);
            new MainMenuGUI(player, plugin).open();
        });
    }
}
