package org.Cry_Legende.sHDisplayEnhanced;

import org.Cry_Legende.sHDisplayEnhanced.commands.AdminCommand;
import org.Cry_Legende.sHDisplayEnhanced.commands.CollectionsCommand;
import org.Cry_Legende.sHDisplayEnhanced.config.ConfigManager;
import org.Cry_Legende.sHDisplayEnhanced.gui.GUIListener;
import org.Cry_Legende.sHDisplayEnhanced.listeners.PlayerConnectionListener;
import org.Cry_Legende.sHDisplayEnhanced.managers.CollectionManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.PlayerManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.ShopManager;
import org.Cry_Legende.sHDisplayEnhanced.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SHDisplayEnhanced extends JavaPlugin {

    private static SHDisplayEnhanced instance;

    private ConfigManager configManager;
    private CollectionManager collectionManager;
    private ShopManager shopManager;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("SHDisplayEnhanced v" + getDescription().getVersion() + " s'active...");

        loadConfigurations();
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();
        this.collectionManager = new CollectionManager(this);
        this.shopManager = new ShopManager(this);
        this.playerManager = new PlayerManager(this);
        this.collectionManager.loadCollections();
        this.shopManager.loadShopItems();
        registerCommands();
        registerListeners();

        // --- CORRECTION MAJEURE : Recharger les données des joueurs déjà en ligne ---
        getLogger().info("Rechargement des données pour les joueurs en ligne...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.loadPlayerData(player.getUniqueId());
        }
        // --- FIN DE LA CORRECTION ---

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Hooked into PlaceholderAPI.");
        }
        getLogger().info("SHDisplayEnhanced a été activé avec succès !");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sauvegarde des données des joueurs en ligne...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.savePlayerDataSync(player.getUniqueId());
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("SHDisplayEnhanced a été désactivé.");
    }

    private void loadConfigurations() {
        saveDefaultConfig();
        File collectionsDir = new File(getDataFolder(), "collections");
        if (!collectionsDir.exists()) {
            collectionsDir.mkdirs();
        }
        File shopFile = new File(getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            saveResource("shop.yml", false);
        }
        this.configManager = new ConfigManager(this);
        this.configManager.load();
    }

    private void registerCommands() {
        getCommand("deco").setExecutor(new CollectionsCommand(this));
        getCommand("shdeco").setExecutor(new AdminCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    public void reloadPlugin() {
        // Sauvegarde des données avant de recharger pour ne rien perdre
        getLogger().info("Sauvegarde des données des joueurs avant rechargement...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.savePlayerDataSync(player.getUniqueId());
        }

        reloadConfig();
        configManager.load();
        collectionManager.loadCollections();
        shopManager.loadShopItems();

        // Recharge les données pour les joueurs en ligne après avoir tout rechargé
        getLogger().info("Rechargement des données pour les joueurs en ligne après rechargement...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.loadPlayerData(player.getUniqueId());
        }
    }

    // --- Getters ---
    public static SHDisplayEnhanced getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}
