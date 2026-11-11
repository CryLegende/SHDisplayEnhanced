package org.Cry_Legende.sHDisplayEnhanced.network;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Décorations enregistrées et séries récompensées en MariaDB, partagées par tous les serveurs. Chargée seulement
 * avec SHNetwork (HikariCP et le pilote MariaDB viennent de {@code libraries:} dans plugin.yml).
 *
 * <p>Toutes les méthodes bloquent : à appeler hors du fil principal.</p>
 */
public final class DisplayDatabase {

    static final String ITEMS = "displayenhanced_items";
    static final String REWARDS = "displayenhanced_rewards";
    private static final int IMPORT_CHUNK = 500;

    /** Bilan d'une importation : joueurs lus, lignes ajoutées, lignes déjà présentes. */
    public record ImportResult(int players, int added, int present) {
    }

    private final HikariDataSource pool;

    public DisplayDatabase(String url, String user, String password, int timeoutMs) {
        this(open(url, user, password, timeoutMs));
    }

    DisplayDatabase(HikariDataSource pool) {
        this.pool = pool;
    }

    private static HikariDataSource open(String url, String user, String password, int timeoutMs) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("SHDisplayEnhanced");
        hikari.setJdbcUrl(url);
        hikari.setUsername(user);
        hikari.setPassword(password);
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.addDataSourceProperty("connectTimeout", String.valueOf(timeoutMs));
        hikari.addDataSourceProperty("socketTimeout", String.valueOf(timeoutMs));
        hikari.setMaximumPoolSize(3);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(timeoutMs);
        // le serveur démarre même si la base est absente : les lectures et écritures échouent alors avec un message
        hikari.setInitializationFailTimeout(-1);
        hikari.setKeepaliveTime(60_000);
        return new HikariDataSource(hikari);
    }

    /** Crée les tables si elles manquent. Des tables déjà là ne demandent aucun droit CREATE à l'utilisateur. */
    public void createTables() throws SQLException {
        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            String options = ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + ITEMS + " ("
                + "uuid CHAR(36) NOT NULL, "
                + "completed_item VARCHAR(128) NOT NULL, "
                + "completion_date BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, completed_item)" + options);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + REWARDS + " ("
                + "uuid CHAR(36) NOT NULL, "
                + "collection_id VARCHAR(64) NOT NULL, "
                + "claimed_at BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, collection_id)" + options);
        }
    }

    /** Décorations enregistrées par le joueur : « série.décoration » -> date (ms). */
    public Map<String, Long> load(UUID uuid) throws SQLException {
        Map<String, Long> items = new HashMap<>();
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT completed_item, completion_date FROM " + ITEMS + " WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return items;
    }

    /** Séries dont la récompense a déjà été réservée par le joueur. */
    public java.util.Set<String> loadRewards(UUID uuid) throws SQLException {
        java.util.Set<String> collections = new java.util.HashSet<>();
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT collection_id FROM " + REWARDS + " WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    collections.add(rs.getString(1));
                }
            }
        }
        return collections;
    }

    /** Enregistre une décoration. Vrai si la ligne est nouvelle ; une décoration déjà enregistrée garde sa première date. */
    public boolean insertItem(UUID uuid, String item, long date) throws SQLException {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + ITEMS + " (uuid, completed_item, completion_date) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, item);
            ps.setLong(3, date);
            return ps.executeUpdate() == 1;
        }
    }

    /** Réserve la récompense de la série : vrai pour le seul serveur qui crée la ligne, faux si elle est déjà donnée. */
    public boolean claimReward(UUID uuid, String collection, long now) throws SQLException {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + REWARDS + " (uuid, collection_id, claimed_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, collection);
            ps.setLong(3, now);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Reprend les décorations de l'ancienne base SQLite (uuid -> décoration -> date). Une ligne déjà en base n'est pas
     * touchée, donc l'importation peut être relancée sans rien écraser.
     */
    public ImportResult importItems(Map<UUID, Map<String, Long>> data) throws SQLException {
        int added = 0;
        int present = 0;
        try (Connection c = pool.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + ITEMS + " (uuid, completed_item, completion_date) VALUES (?, ?, ?)")) {
                int inChunk = 0;
                for (Map.Entry<UUID, Map<String, Long>> player : data.entrySet()) {
                    for (Map.Entry<String, Long> item : player.getValue().entrySet()) {
                        ps.setString(1, player.getKey().toString());
                        ps.setString(2, item.getKey());
                        ps.setLong(3, item.getValue());
                        if (ps.executeUpdate() == 1) {
                            added++;
                        } else {
                            present++;
                        }
                        if (++inChunk >= IMPORT_CHUNK) {
                            c.commit();
                            inChunk = 0;
                        }
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return new ImportResult(data.size(), added, present);
    }

    public void close() {
        pool.close();
    }
}
