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
    public void disconnect() {
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
    public Map<String, Long> getCompletedItems(UUID uuid) {
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
        }
        return items;
    }

    /**
     * Sauvegarde l'ensemble de la progression d'un joueur.
     * Pour garantir l'intégrité, les anciennes données sont supprimées avant d'insérer les nouvelles.
     *
     * @param uuid  L'UUID du joueur.
     * @param items La Map complète de la progression du joueur à sauvegarder.
     */
    public void saveCompletedItems(UUID uuid, Map<String, Long> items) {
        String deleteSql = "DELETE FROM player_collections WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression des anciennes données pour le joueur " + uuid, e);
            return; // Arrêter si la suppression échoue
        }

        String insertSql = "INSERT INTO player_collections(uuid, completed_item, completion_date) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Map.Entry<String, Long> entry : items.entrySet()) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, entry.getKey());
                pstmt.setLong(3, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde des nouvelles données pour le joueur " + uuid, e);
        }
    }
}
