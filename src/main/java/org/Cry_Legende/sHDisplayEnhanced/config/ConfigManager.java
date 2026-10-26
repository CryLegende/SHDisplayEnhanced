package org.Cry_Legende.sHDisplayEnhanced.config;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ConfigManager {

    private final SHDisplayEnhanced plugin;

    // Messages
    private String noPermissionMessage;
    private String playerOnlyMessage;
    private String reloadMessage;

    // GUI Items & Settings
    private Sound clickSound;
    private String mainMenuTitle;
    private ItemStack fillerItem;
    private ItemStack noCollectionsItem;
    private ItemStack backButton;
    private ItemStack lockedItem;
    private String lockedItemName;
    private List<String> lockedItemLore;
    private List<String> unlockedLore;
    private ItemStack rewardInfoItem; // NOUVEAU
    private ItemStack shopInfoItem;   // NOUVEAU

    public ConfigManager(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        noPermissionMessage = loadString(config, "messages.no_permission", "&cVous n'avez pas la permission.");
        playerOnlyMessage = loadString(config, "messages.player_only", "&cCette commande ne peut être exécutée que par un joueur.");
        reloadMessage = loadString(config, "messages.reload", "&aConfiguration rechargée.");

        clickSound = sound(config.getString("gui.click_sound", "UI_BUTTON_CLICK"));
        if (clickSound == null) {
            plugin.getLogger().warning("Le son '" + config.getString("gui.click_sound") + "' n'est pas valide. Utilisation de UI_BUTTON_CLICK par défaut.");
            clickSound = Sound.UI_BUTTON_CLICK;
        }

        mainMenuTitle = loadString(config, "gui.main_menu.title", "&8Décorations");
        fillerItem = createItemFromConfig(config.getConfigurationSection("gui.filler_item"));
        noCollectionsItem = createItemFromConfig(config.getConfigurationSection("gui.no_collections_item"));
        backButton = createItemFromConfig(config.getConfigurationSection("gui.navigation.back_button"));
        lockedItem = createItemFromConfig(config.getConfigurationSection("gui.collection.locked_item"));
        rewardInfoItem = createItemFromConfig(config.getConfigurationSection("gui.collection.reward_info_item")); // NOUVEAU
        shopInfoItem = createItemFromConfig(config.getConfigurationSection("gui.shop_info_item"));                 // NOUVEAU

        lockedItemName = config.getString("gui.collection.locked_item.name", "&cDécoration Verrouillée");
        lockedItemLore = config.getStringList("gui.collection.locked_item.lore");
        unlockedLore = config.getStringList("gui.collection.unlocked_lore");
    }

    private ItemStack createItemFromConfig(ConfigurationSection section) {
        if (section == null) {
            ItemStack errorItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = errorItem.getItemMeta();
            if (meta != null) meta.setDisplayName("§cErreur de Config");
            errorItem.setItemMeta(meta);
            return errorItem;
        }
        try {
            Material material = Material.matchMaterial(section.getString("material", "STONE"));
            ItemStack item = new ItemStack(material != null ? material : Material.STONE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (section.isSet("name")) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
                }
                if (section.isSet("lore")) {
                    List<String> lore = section.getStringList("lore").stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la création d'un item depuis la config: " + section.getCurrentPath());
            e.printStackTrace();
            return new ItemStack(Material.BARRIER, 1);
        }
    }

    /** Son lu en config : ancien nom (ENTITY_PLAYER_LEVELUP) ou clé (minecraft:entity.player.levelup). */
    static Sound sound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.indexOf(':') >= 0 || s.indexOf('.') >= 0) {
            NamespacedKey key = NamespacedKey.fromString(s.toLowerCase(Locale.ROOT));
            return key == null ? null : Registry.SOUND_EVENT.get(key);
        }
        try {
            return (Sound) Sound.class.getField(s.toUpperCase(Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    private String loadString(FileConfiguration config, String path, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', config.getString(path, defaultValue));
    }

    // --- Getters ---
    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public String getPlayerOnlyMessage() {
        return playerOnlyMessage;
    }

    public String getReloadMessage() {
        return reloadMessage;
    }

    public Sound getClickSound() {
        return clickSound;
    }

    public String getMainMenuTitle() {
        return mainMenuTitle;
    }

    public ItemStack getFillerItem() {
        return fillerItem.clone();
    }

    public ItemStack getNoCollectionsItem() {
        return noCollectionsItem.clone();
    }

    public ItemStack getBackButton() {
        return backButton.clone();
    }

    public ItemStack getLockedItem() {
        return lockedItem.clone();
    }

    public ItemStack getRewardInfoItem() {
        return rewardInfoItem.clone();
    } // NOUVEAU

    public ItemStack getShopInfoItem() {
        return shopInfoItem.clone();
    }     // NOUVEAU

    public String getLockedItemName() {
        return lockedItemName;
    }

    public List<String> getLockedItemLore() {
        return Collections.unmodifiableList(lockedItemLore);
    }

    public List<String> getUnlockedLore() {
        return Collections.unmodifiableList(unlockedLore);
    }

    public ItemStack getMenuItem(String key) {
        return createItemFromConfig(plugin.getConfig().getConfigurationSection("gui.main_menu.items." + key));
    }

    public int getMenuItemSlot(String key) {
        return plugin.getConfig().getInt("gui.main_menu.items." + key + ".slot", 0);
    }
}
