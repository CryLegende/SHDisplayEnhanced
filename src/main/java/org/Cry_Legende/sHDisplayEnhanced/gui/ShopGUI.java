package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.SellableItem;
import org.Cry_Legende.sHDisplayEnhanced.utils.ItemUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ShopGUI extends AbstractGUI {

    private final SHDisplayEnhanced plugin;

    public ShopGUI(Player player, SHDisplayEnhanced plugin) {
        super(player, 45, "Vendre des Décorations en Double");
        this.plugin = plugin;
    }

    @Override
    protected void build() {
        actions.clear();

        ItemStack filler = plugin.getConfigManager().getFillerItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > inventory.getSize() - 10 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inventory.setItem(i, filler);
            }
        }

        inventory.setItem(4, plugin.getConfigManager().getShopInfoItem());

        int slot = 10;
        Map<String, SellableItem> sellableItems = plugin.getShopManager().getSellableItems();

        for (Map.Entry<String, SellableItem> entry : sellableItems.entrySet()) {
            String key = entry.getKey();
            SellableItem sellableItem = entry.getValue();

            String[] ids = key.split("\\.", 2);
            if (ids.length != 2) continue;
            String collectionId = ids[0];
            String itemId = ids[1];

            // --- CORRECTION MAJEURE ---
            // On utilise getRequiredItem() (sans le lore du prix) pour la vérification.
            if (plugin.getPlayerManager().hasCompleted(player.getUniqueId(), collectionId, itemId) &&
                inventoryContains(player.getInventory(), sellableItem.getRequiredItem())) {

                // On utilise getDisplayItem() (avec le lore du prix) pour l'affichage.
                inventory.setItem(slot, sellableItem.getDisplayItem());
                actions.put(slot, e -> {
                    // On utilise getRequiredItem() (sans le lore du prix) pour la suppression.
                    if (removeItem(player.getInventory(), sellableItem.getRequiredItem())) {
                        sellableItem.executeSellCommands(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
                        refresh();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        player.sendMessage("§cErreur: L'objet n'a pas pu être trouvé dans votre inventaire.");
                    }
                });

                slot++;
                if ((slot + 1) % 9 == 0) slot += 2;
                if (slot >= 35) break;
            }
        }

        inventory.setItem(40, plugin.getConfigManager().getBackButton());
        actions.put(40, e -> {
            player.playSound(player.getLocation(), plugin.getConfigManager().getClickSound(), 1f, 1f);
            new MainMenuGUI(player, plugin).open();
        });
    }

    private boolean inventoryContains(org.bukkit.inventory.Inventory inventory, ItemStack requiredItem) {
        for (ItemStack item : inventory.getContents()) {
            if (ItemUtils.isSimilar(item, requiredItem)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeItem(org.bukkit.inventory.Inventory inventory, ItemStack requiredItem) {
        for (ItemStack item : inventory.getContents()) {
            if (ItemUtils.isSimilar(item, requiredItem)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void refresh() {
        inventory.clear();
        build();
    }
}
