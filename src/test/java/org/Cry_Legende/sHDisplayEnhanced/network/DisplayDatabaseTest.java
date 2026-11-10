package org.Cry_Legende.sHDisplayEnhanced.network;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.Cry_Legende.sHDisplayEnhanced.network.DisplayDatabase.ImportResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requêtes des séries sur un vrai MariaDB : récompense d'une série donnée par un seul serveur, décoration enregistrée
 * sans écraser la première date, importation relançable. Ignoré sans SHDE_TEST_URL (une base d'essai dont le nom contient
 * « test », par exemple jdbc:mariadb://127.0.0.1:3399/zz_shde_test), SHDE_TEST_USER et SHDE_TEST_PASSWORD.
 */
class DisplayDatabaseTest {

    private HikariDataSource pool;
    private DisplayDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("SHDE_TEST_URL");
        Assumptions.assumeTrue(url != null && url.contains("test"), "SHDE_TEST_URL absent ou sans « test » : test MariaDB ignoré");
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(url);
        hikari.setUsername(System.getenv().getOrDefault("SHDE_TEST_USER", "shdisplayenhanced"));
        hikari.setPassword(System.getenv().getOrDefault("SHDE_TEST_PASSWORD", "shdisplayenhanced"));
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setMaximumPoolSize(16);
        pool = new HikariDataSource(hikari);
        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS " + DisplayDatabase.ITEMS);
            st.executeUpdate("DROP TABLE IF EXISTS " + DisplayDatabase.REWARDS);
        }
        db = new DisplayDatabase(pool);
        db.createTables();
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.close();
        }
    }

    private static void parallel(int threads, ThrowingRunnable task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                task.run();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    void seriesRewardIsClaimedByOneServerOnly() throws Exception {
        UUID player = UUID.randomUUID();
        List<Boolean> results = new CopyOnWriteArrayList<>();
        // deux serveurs terminent la même série au même moment : un seul paie
        parallel(8, () -> results.add(db.claimReward(player, "creatures", System.currentTimeMillis())));
        assertEquals(8, results.size());
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        // une autre série du même joueur reste réservable
        assertTrue(db.claimReward(player, "nourriture", System.currentTimeMillis()));
        assertFalse(db.claimReward(player, "nourriture", System.currentTimeMillis()));
    }

    @Test
    void itemKeepsItsFirstDateAndLoadSeesEveryServer() throws Exception {
        UUID player = UUID.randomUUID();
        assertTrue(db.insertItem(player, "creatures.Hedgehog", 100L));
        assertFalse(db.insertItem(player, "creatures.Hedgehog", 200L));
        assertTrue(db.insertItem(player, "creatures.Frog", 300L));
        Map<String, Long> loaded = db.load(player);
        assertEquals(2, loaded.size());
        assertEquals(100L, loaded.get("creatures.Hedgehog"));
        assertTrue(db.load(UUID.randomUUID()).isEmpty());
    }

    @Test
    void importCanBeRerunWithoutOverwriting() throws Exception {
        UUID player = UUID.randomUUID();
        Map<UUID, Map<String, Long>> data = Map.of(player, Map.of("a.x", 10L, "a.y", 20L));
        ImportResult first = db.importItems(data);
        assertEquals(1, first.players());
        assertEquals(2, first.added());
        assertEquals(0, first.present());
        // une décoration enregistrée depuis, avec une autre date, n'est pas touchée par la seconde importation
        assertTrue(db.insertItem(player, "a.z", 30L));
        ImportResult second = db.importItems(data);
        assertEquals(0, second.added());
        assertEquals(2, second.present());
        assertEquals(3, db.load(player).size());
        assertEquals(10L, db.load(player).get("a.x"));
    }
}
