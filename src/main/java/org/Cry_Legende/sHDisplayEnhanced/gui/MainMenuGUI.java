package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.config.ConfigManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.PlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.stream.Collectors;

public class MainMenuGUI extends AbstractGUI {

    private final SHDisplayEnhanced plugin;

    public MainMenuGUI(Player player, SHDisplayEnhanced plugin) {
        super(player, 27, plugin.getConfigManager().getMainMenuTitle());
        this.plugin = plugin;
    }

    @Override
    protected void build() {
        ConfigManager cfg = plugin.getConfigManager();
        PlayerManager pm = plugin.getPlayerManager();

        // Appliquer le filler sur les slots vides
        ItemStack filler = cfg.getFillerItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Item des Séries
        ItemStack collectionsItem = cfg.getMenuItem("collections");
        inventory.setItem(cfg.getMenuItemSlot("collections"), collectionsItem);
        actions.put(cfg.getMenuItemSlot("collections"), e -> new CollectionsListGUI(player, plugin).open());

        // Item de la Boutique
        ItemStack shopItem = cfg.getMenuItem("shop");
        inventory.setItem(cfg.getMenuItemSlot("shop"), shopItem);
        actions.put(cfg.getMenuItemSlot("shop"), e -> new ShopGUI(player, plugin).open());

        // Item des Statistiques avec le skin du joueur
        ItemStack statsItem = cfg.getMenuItem("stats");
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) statsMeta;
            skullMeta.setOwningPlayer(player); // Applique le skin du joueur
            statsItem.setItemMeta(skullMeta);
        }

        if (statsMeta != null && statsMeta.hasLore()) {
            int globalProgress = pm.getGlobalProgress(player.getUniqueId());
            long completed = pm.getTotalCollectionsCompleted(player.getUniqueId());
            int total = plugin.getCollectionManager().getAllCollections().size();
            List<String> newLore = statsMeta.getLore().stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line
                    .replace("%shde_global_progress%", String.valueOf(globalProgress))
                    .replace("%shde_collections_completed%", String.valueOf(completed))
                    .replace("%shde_collections_total%", String.valueOf(total))
                )).collect(Collectors.toList());
            statsMeta.setLore(newLore);
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(cfg.getMenuItemSlot("stats"), statsItem);
    }
}
