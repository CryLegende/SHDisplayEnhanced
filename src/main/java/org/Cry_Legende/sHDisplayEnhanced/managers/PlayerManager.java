package org.Cry_Legende.sHDisplayEnhanced.managers;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final SHDisplayEnhanced plugin;
    // Lu et écrit depuis le thread principal et depuis les tâches asynchrones de chargement/sauvegarde
    private final Map<UUID, Map<String, Long>> playerDataCache = new ConcurrentHashMap<>();

    public PlayerManager(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerData(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            Map<String, Long> completedItems = plugin.getDatabaseManager().getCompletedItems(uuid);
            if (completedItems == null) return; // lecture impossible (déjà journalisé) : rien en cache
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Le joueur a pu quitter pendant la lecture : on ne garde pas ses données en mémoire
                if (Bukkit.getPlayer(uuid) == null) return;
                Map<String, Long> loaded = new ConcurrentHashMap<>(completedItems);
                // Une série validée pendant le chargement ne doit pas être écrasée
                Map<String, Long> already = playerDataCache.get(uuid);
                if (already != null) already.forEach(loaded::putIfAbsent);
                playerDataCache.put(uuid, loaded);
                plugin.getLogger().info("Données de série chargées pour le joueur " + uuid);
            });
        });
    }

    // Version asynchrone pour la déconnexion normale d'un joueur.
    public void savePlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            Map<String, Long> dataToSave = playerDataCache.remove(uuid);
            CompletableFuture.runAsync(() -> {
                plugin.getDatabaseManager().saveCompletedItems(uuid, dataToSave);
                plugin.getLogger().info("Données de série sauvegardées (async) pour le joueur " + uuid);
            });
        }
    }

    // NOUVELLE MÉTHODE : Version synchrone pour l'arrêt du serveur.
    public void savePlayerDataSync(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            Map<String, Long> dataToSave = playerDataCache.get(uuid); // On ne retire pas du cache, car le serveur s'éteint
            plugin.getDatabaseManager().saveCompletedItems(uuid, dataToSave);
            plugin.getLogger().info("Données de série sauvegardées (sync) pour le joueur " + uuid);
        }
    }

    public boolean hasCompleted(UUID uuid, String collectionId, String itemId) {
        return playerDataCache.getOrDefault(uuid, Map.of()).containsKey(collectionId + "." + itemId);
    }

    public void setCompleted(UUID uuid, String collectionId, String itemId) {
        playerDataCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(collectionId + "." + itemId, System.currentTimeMillis());
    }

    public long getCompletionDate(UUID uuid, String collectionId, String itemId) {
        return playerDataCache.getOrDefault(uuid, Map.of()).getOrDefault(collectionId + "." + itemId, 0L);
    }

    public long getCompletedCount(UUID uuid, String collectionId) {
        Map<String, Long> data = playerDataCache.get(uuid);
        if (data == null) return 0;
        return data.keySet().stream().filter(key -> key.startsWith(collectionId + ".")).count();
    }

    public boolean hasCompletedCollection(UUID uuid, Collection collection) {
        int totalItemsRequired = collection.getCollectionItems().size();
        if (totalItemsRequired == 0) return true;
        return getCompletedCount(uuid, collection.getId()) >= totalItemsRequired;
    }

    public int getGlobalProgress(UUID uuid) {
        Map<String, Collection> allCollections = plugin.getCollectionManager().getAllCollections();
        if (allCollections.isEmpty()) return 0;
        int totalPossibleItems = 0;
        long totalPlayerItems = 0;
        for (Collection collection : allCollections.values()) {
            totalPossibleItems += collection.getCollectionItems().size();
            totalPlayerItems += getCompletedCount(uuid, collection.getId());
        }
        if (totalPossibleItems == 0) return 100;
        return (int) ((double) totalPlayerItems / totalPossibleItems * 100.0);
    }

    public long getTotalCollectionsCompleted(UUID uuid) {
        return plugin.getCollectionManager().getAllCollections().values().stream()
            .filter(collection -> hasCompletedCollection(uuid, collection))
            .count();
    }
}
