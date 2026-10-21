package org.Cry_Legende.sHDisplayEnhanced.managers;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.CollectionItem;
import org.Cry_Legende.sHDisplayEnhanced.model.SellableItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final SHDisplayEnhanced plugin;
    private final Map<String, SellableItem> sellableItems = new HashMap<>();

    public ShopManager(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    public void loadShopItems() {
        sellableItems.clear();
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        ConfigurationSection mainSection = shopConfig.getConfigurationSection("sellable_items");
        if (mainSection == null) {
            plugin.getLogger().info("Aucune section 'sellable_items' trouvée dans shop.yml. La boutique de vente sera vide.");
            return;
        }

        for (String catalogueId : mainSection.getKeys(false)) {
            ConfigurationSection catalogueSection = mainSection.getConfigurationSection(catalogueId);
            if (catalogueSection == null) continue;

            for (String itemId : catalogueSection.getKeys(false)) {
                String fullId = catalogueId + "." + itemId;
                ConfigurationSection itemSection = catalogueSection.getConfigurationSection(itemId);
                if (itemSection == null) continue;

                CollectionItem linkedItem = plugin.getCollectionManager().findCollectionItem(fullId);
                if (linkedItem == null) {
                    plugin.getLogger().warning("L'objet vendable '" + fullId + "' dans shop.yml ne correspond à aucune décoration existante ! Il sera ignoré.");
                    continue;
                }

                // --- MODIFICATION ICI ---
                List<String> commands = itemSection.getStringList("commands");
                List<String> sellPriceLore = itemSection.getStringList("sell_price_lore");
                sellableItems.put(fullId, new SellableItem(fullId, linkedItem, commands, sellPriceLore));
                // --- FIN DE LA MODIFICATION ---
            }
        }
        plugin.getLogger().info(sellableItems.size() + " décoration(s) vendable(s) chargée(s) depuis shop.yml.");
    }

    public Map<String, SellableItem> getSellableItems() {
        return Collections.unmodifiableMap(sellableItems);
    }
}