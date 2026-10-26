package org.Cry_Legende.sHDisplayEnhanced.storage;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gère la connexion et les interactions avec la base de données SQLite
 * pour stocker les données de progression des joueurs.
 */
public class DatabaseManager {

    private Connection connection;
    private final SHDisplayEnhanced plugin;

    public DatabaseManager(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    /**
     * Établit la connexion à la base de données SQLite.
     * Si le fichier de base de données n'existe pas, il est créé.
     */
    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Connexion à la base de données SQLite établie.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de se connecter à la base de données SQLite !", e);
        }
    }

    /**
     * Crée la table nécessaire pour stocker les données si elle n'existe pas déjà.
     */
    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_collections (" +
            "uuid TEXT NOT NULL," +
            "completed_item TEXT NOT NULL," +
            "completion_date BIGINT NOT NULL," +
            "PRIMARY KEY(uuid, completed_item));";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de créer la table 'player_collections'.", e);
        }
    }

    /**
     * Ferme la connexion à la base de données.
     */
    public synchronized void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connexion à la base de données SQLite fermée.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la fermeture de la connexion à la base de données.", e);
        }
    }

    /**
     * Récupère tous les items complétés et leur date de complétion pour un joueur donné.
     *
     * @param uuid L'UUID du joueur.
     * @return Une Map contenant l'ID de l'item et le timestamp de complétion.
     */
    public synchronized Map<String, Long> getCompletedItems(UUID uuid) {
        // Renvoie null si la lecture échoue : une map vide serait prise pour "aucune progression" et écrasée à la sauvegarde
        if (connection == null) return null;
        String sql = "SELECT completed_item, completion_date FROM player_collections WHERE uuid = ?";
        Map<String, Long> items = new HashMap<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.put(rs.getString("completed_item"), rs.getLong("completion_date"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de récupérer les données pour le joueur " + uuid, e);
            return null;
        }
        return items;
    }

    /**
     * Sauvegarde la progression d'un joueur.
     * La progression ne fait qu'augmenter : on écrit sans rien supprimer, dans une transaction, pour qu'un échec
     * en cours de route ne puisse pas effacer les données déjà enregistrées.
     *
     * @param uuid  L'UUID du joueur.
     * @param items La Map complète de la progression du joueur à sauvegarder.
     */
    public synchronized void saveCompletedItems(UUID uuid, Map<String, Long> items) {
        if (connection == null) return;
        String sql = "INSERT OR REPLACE INTO player_collections(uuid, completed_item, completion_date) VALUES(?, ?, ?)";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (Map.Entry<String, Long> entry : items.entrySet()) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, entry.getKey());
                    pstmt.setLong(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde des données pour le joueur " + uuid, e);
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}
