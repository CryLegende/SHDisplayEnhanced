package org.Cry_Legende.sHDisplayEnhanced.model;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Collection {

    private final String id;
    private final String guiTitle;
    private final int guiSize;
    private final ItemStack menuItem;
    private final List<String> rewardCommands;
    private final List<String> rewardDisplayLore; // NOUVEAU
    private final Map<String, CollectionItem> collectionItems = new HashMap<>();
    private final Map<Integer, ItemStack> decorativeItems = new HashMap<>();

    public Collection(FileConfiguration config) {
        this.id = config.getString("collection_id", "id_invalide");
        this.guiTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "&cTitre Manquant"));
        this.guiSize = config.getInt("gui.size", 54);
        this.rewardCommands = config.getStringList("rewards.commands");
        this.rewardDisplayLore = config.getStringList("rewards.reward_display"); // NOUVEAU

        this.menuItem = createMenuItem(config.getConfigurationSection("gui.menu_item"));
        loadItems(config.getConfigurationSection("items"));
        loadDecorativeItems(config.getConfigurationSection("decorative_items"));
    }

    private void loadItems(ConfigurationSection itemsSection) {
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null) {
                    collectionItems.put(key, new CollectionItem(key, itemConfig));
                }
            }
        }
    }

    private ItemStack createMenuItem(ConfigurationSection menuConfig) {
        if (menuConfig == null) return new ItemStack(Material.BARRIER);
        Material material = Material.matchMaterial(menuConfig.getString("material", "BOOK"));
        ItemStack item = new ItemStack(material != null ? material : Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', menuConfig.getString("name", "Série")));
            if (menuConfig.isList("lore")) {
                meta.setLore(menuConfig.getStringList("lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void loadDecorativeItems(ConfigurationSection decorativeSection) {
        if (decorativeSection != null) {
            for (String key : decorativeSection.getKeys(false)) {
                ConfigurationSection decorConfig = decorativeSection.getConfigurationSection(key);
                if (decorConfig != null && decorConfig.isSet("slot") && decorConfig.isConfigurationSection("item")) {
                    int slot = decorConfig.getInt("slot");
                    ItemStack item = createMenuItem(decorConfig.getConfigurationSection("item"));
                    decorativeItems.put(slot, item);
                }
            }
        }
    }

    public void executeRewardCommands(Player player) {
        if (rewardCommands == null || rewardCommands.isEmpty()) return;
        rewardCommands.forEach(command -> {
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public int getGuiSize() {
        return guiSize;
    }

    public ItemStack getMenuItem() {
        return menuItem;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public List<String> getRewardDisplayLore() {
        return rewardDisplayLore;
    } // NOUVEAU

    public Map<String, CollectionItem> getCollectionItems() {
        return collectionItems;
    }

    public Map<Integer, ItemStack> getDecorativeItems() {
        return decorativeItems;
    }
}
