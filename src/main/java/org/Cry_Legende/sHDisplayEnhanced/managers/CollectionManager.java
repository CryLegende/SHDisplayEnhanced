package org.Cry_Legende.sHDisplayEnhanced.managers;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.Cry_Legende.sHDisplayEnhanced.model.CollectionItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gère le chargement et l'accès à toutes les collections du plugin.
 */
public class CollectionManager {

    private final SHDisplayEnhanced plugin;
    private final Map<String, Collection> collections = new HashMap<>();

    public CollectionManager(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    public void loadCollections() {
        collections.clear(); // Vider les anciennes collections avant de recharger
        File collectionsDir = new File(plugin.getDataFolder(), "collections");

        if (!collectionsDir.exists() || !collectionsDir.isDirectory() || collectionsDir.listFiles() == null) {
            plugin.getLogger().warning("Le dossier des collections est vide ou n'existe pas. Aucune collection ne sera chargée.");
            return;
        }

        for (File file : collectionsDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                FileConfiguration collectionConfig = YamlConfiguration.loadConfiguration(file);
                String collectionId = collectionConfig.getString("collection_id");

                if (collectionId == null || collectionId.isEmpty()) {
                    plugin.getLogger().warning("Le fichier '" + file.getName() + "' n'a pas de 'collection_id' et sera ignoré.");
                    continue;
                }

                if (collections.containsKey(collectionId)) {
                    plugin.getLogger().warning("ID de collection dupliqué : '" + collectionId + "'. Le fichier '" + file.getName() + "' sera ignoré.");
                    continue;
                }

                Collection collection = new Collection(collectionConfig);
                collections.put(collection.getId(), collection);
            }
        }
        plugin.getLogger().info(collections.size() + " collection(s) chargée(s).");
    }

    public Collection getCollection(String id) {
        return collections.get(id);
    }

    public Map<String, Collection> getAllCollections() {
        return new HashMap<>(collections); // Retourne une copie pour éviter les modifications externes
    }

    /**
     * Trouve un CollectionItem spécifique à travers toutes les collections.
     *
     * @param fullId L'ID complet sous la forme "collection_id.item_id".
     * @return Le CollectionItem correspondant, ou null s'il n'est pas trouvé.
     */
    public CollectionItem findCollectionItem(String fullId) {
        String[] parts = fullId.split("\\.", 2);
        if (parts.length != 2) {
            return null;
        }
        String collectionId = parts[0];
        String itemId = parts[1];

        Collection collection = getCollection(collectionId);
        if (collection != null) {
            return collection.getCollectionItems().get(itemId);
        }
        return null;
    }
}
