package org.Cry_Legende.sHDisplayEnhanced;

import org.Cry_Legende.sHDisplayEnhanced.commands.AdminCommand;
import org.Cry_Legende.sHDisplayEnhanced.commands.CollectionsCommand;
import org.Cry_Legende.sHDisplayEnhanced.config.ConfigManager;
import org.Cry_Legende.sHDisplayEnhanced.gui.GUIListener;
import org.Cry_Legende.sHDisplayEnhanced.listeners.PlayerConnectionListener;
import org.Cry_Legende.sHDisplayEnhanced.managers.CollectionManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.PlayerManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.ShopManager;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.Cry_Legende.sHDisplayEnhanced.network.DisplayDatabase;
import org.Cry_Legende.sHDisplayEnhanced.network.NetBus;
import org.Cry_Legende.sHDisplayEnhanced.network.NetworkDisplay;
import org.Cry_Legende.sHDisplayEnhanced.network.ShNetworkBus;
import org.Cry_Legende.sHDisplayEnhanced.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public final class SHDisplayEnhanced extends JavaPlugin {

    private static SHDisplayEnhanced instance;

    private ConfigManager configManager;
    private CollectionManager collectionManager;
    private ShopManager shopManager;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private volatile FileConfiguration messages;
    private String networkSettingsAtStart = "";
    // Non nul seulement avec SHNetwork : décorations et récompenses de séries dans la base partagée
    private NetworkDisplay network;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("SHDisplayEnhanced v" + getDescription().getVersion() + " s'active...");

        loadConfigurations();
        networkSettingsAtStart = networkSettings();
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();
        this.collectionManager = new CollectionManager(this);
        this.shopManager = new ShopManager(this);
        this.playerManager = new PlayerManager(this);
        this.collectionManager.loadCollections();
        this.shopManager.loadShopItems();
        enableNetwork();
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
        if (playerManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerManager.savePlayerDataSync(player.getUniqueId());
            }
        }

        if (network != null) {
            network.close();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("SHDisplayEnhanced a été désactivé.");
    }

    /**
     * Passe en mode réseau si SHNetwork est installé et 'reseau.actif' est vrai. Les classes qui touchent à SHNetwork
     * ne sont résolues qu'ici : sans lui, rien n'est chargé et le plugin garde son fonctionnement d'origine.
     */
    private void enableNetwork() {
        if (!getConfig().getBoolean("reseau.actif", true)) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("SHNetwork")) return;
        DisplayDatabase db = null;
        try {
            NetBus bus = ShNetworkBus.open();
            db = new DisplayDatabase(
                getConfig().getString("reseau.base-de-donnees.url"),
                getConfig().getString("reseau.base-de-donnees.utilisateur"),
                getConfig().getString("reseau.base-de-donnees.mot-de-passe"),
                Math.max(250, getConfig().getInt("reseau.base-de-donnees.delai-ms", 3000)));
            NetworkDisplay display = new NetworkDisplay(this, bus, db);
            network = display;
            playerManager.enableNetwork(display);
            display.start();
            getLogger().info("SHNetwork détecté : décorations et récompenses de séries dans la base partagée entre les serveurs.");
        } catch (RuntimeException | LinkageError e) {
            network = null;
            playerManager.enableNetwork(null);
            if (db != null) db.close();
            getLogger().log(Level.SEVERE, "SHNetwork présent mais inutilisable, décorations dans la base de ce serveur : " + e);
        }
    }

    /** Message de messages.yml avec ses codes couleur. */
    public String message(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(key, ""));
    }

    private void loadMessages() {
        try {
            messages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().log(Level.SEVERE, "messages.yml illisible, textes du jar utilisés : " + describe(e));
            YamlConfiguration fallback = new YamlConfiguration();
            YamlConfiguration defaults = jarMessages();
            if (defaults != null) {
                fallback.setDefaults(defaults);
            }
            messages = fallback;
        }
    }

    /** Lit messages.yml en mode strict (une erreur de syntaxe est levée) ; un texte absent vient du fichier du jar. */
    private YamlConfiguration readMessages() throws IOException, InvalidConfigurationException {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.load(file);
        // Un messages.yml plus ancien n'a pas les textes ajoutés depuis : ils viennent du fichier du jar
        YamlConfiguration defaults = jarMessages();
        if (defaults != null) {
            loaded.setDefaults(defaults);
        }
        return loaded;
    }

    private YamlConfiguration jarMessages() {
        try (InputStream in = getResource("messages.yml")) {
            return in == null ? null : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            getLogger().warning("messages.yml par défaut illisible : " + e.getMessage());
            return null;
        }
    }

    /** Cause d'une erreur de lecture sur une seule ligne. */
    private static String describe(Exception e) {
        String text = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        text = text.replaceAll("\\s*\\R\\s*", " ").trim();
        return text.length() > 240 ? text.substring(0, 240) + "..." : text;
    }

    /** Série terminée par un joueur : son et message, puis commandes de récompense de la série. */
    public void grantSeries(Player player, Collection collection) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendMessage(message("series_completed").replace("%collection%", collection.getId()));
        collection.executeRewardCommands(player);
    }

    public NetworkDisplay getNetwork() {
        return network;
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
        loadMessages();
        this.configManager = new ConfigManager(this);
        this.configManager.load();
    }

    private void registerCommands() {
        getCommand("deco").setExecutor(new CollectionsCommand(this));
        PluginCommand adminCommand = getCommand("shdisplayenhanced");
        if (adminCommand != null) {
            AdminCommand admin = new AdminCommand(this);
            adminCommand.setExecutor(admin);
            adminCommand.setTabCompleter(admin);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    /**
     * Relit config.yml, messages.yml, shop.yml et chaque série de collections/. Renvoie null si tout est lu, sinon
     * « fichier : erreur » : rien n'est alors rechargé et l'ancienne configuration reste en mémoire. Les décorations des
     * joueurs (playerdata.db ou base partagée) ne sont jamais touchées. La section 'reseau' n'est pas relue à chaud.
     */
    public String reloadAll() {
        List<File> files = new ArrayList<>();
        files.add(new File(getDataFolder(), "config.yml"));
        files.add(new File(getDataFolder(), "shop.yml"));
        File[] series = new File(getDataFolder(), "collections").listFiles();
        if (series != null) {
            Arrays.sort(series);
            for (File file : series) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    files.add(file);
                }
            }
        }
        for (File file : files) {
            if (!file.exists()) {
                continue;
            }
            try {
                new YamlConfiguration().load(file);
            } catch (IOException | InvalidConfigurationException e) {
                String name = file.getParentFile().getName().equals("collections") ? "collections/" + file.getName() : file.getName();
                return name + " : " + describe(e);
            }
        }
        YamlConfiguration loadedMessages;
        try {
            loadedMessages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            return "messages.yml : " + describe(e);
        }

        // Sauvegarde des données avant de recharger pour ne rien perdre
        getLogger().info("Sauvegarde des données des joueurs avant rechargement...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.savePlayerDataSync(player.getUniqueId());
        }

        reloadConfig();
        messages = loadedMessages;
        configManager.load();
        collectionManager.loadCollections();
        shopManager.loadShopItems();

        // Recharge les données pour les joueurs en ligne après avoir tout rechargé
        getLogger().info("Rechargement des données pour les joueurs en ligne après rechargement...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.loadPlayerData(player.getUniqueId());
        }
        return null;
    }

    /** Contenu de la section 'reseau' du config.yml, pour savoir si elle a changé depuis le démarrage. */
    private String networkSettings() {
        ConfigurationSection section = getConfig().getConfigurationSection("reseau");
        return section == null ? "" : section.getValues(true).toString();
    }

    /** Vrai quand la section 'reseau' n'est plus celle du démarrage : elle ne compte qu'après un redémarrage. */
    public boolean networkSettingsChanged() {
        return !networkSettings().equals(networkSettingsAtStart);
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
