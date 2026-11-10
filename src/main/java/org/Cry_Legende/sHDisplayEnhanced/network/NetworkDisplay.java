package org.Cry_Legende.sHDisplayEnhanced.network;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.Cry_Legende.sHDisplayEnhanced.network.DisplayDatabase.ImportResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Séries de décorations partagées entre les serveurs Opale. Les décorations enregistrées par un joueur vivent en mémoire
 * (PlayerManager) ; ici se trouvent leur lecture en base à l'arrivée du joueur, leur écriture au moment même où il les
 * enregistre, et la réservation en base de la récompense de chaque série : le serveur qui crée la ligne est le seul
 * à payer. Chargée seulement avec SHNetwork.
 *
 * <p>Aucune méthode ne touche la base sur le fil principal.</p>
 */
public final class NetworkDisplay {

    private static final int ATTEMPTS = 3;
    private static final long RETRY_MILLIS = 300L;

    private final SHDisplayEnhanced plugin;
    private final NetBus bus;
    private final DisplayDatabase db;
    /** Dernière écriture en cours de chaque joueur : les écritures d'un même joueur partent dans l'ordre. */
    private final Map<UUID, CompletableFuture<Void>> pending = new ConcurrentHashMap<>();
    /** Joueurs dont les décorations sont lues en base : avant cela, rien ne peut être enregistré. */
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public NetworkDisplay(SHDisplayEnhanced plugin, NetBus bus, DisplayDatabase db) {
        this.plugin = plugin;
        this.bus = bus;
        this.db = db;
    }

    /** Crée les tables et inscrit l'attente des écritures au départ de chaque joueur. */
    public void start() {
        bus.registerSaveTask(player -> {
            UUID uuid = player.getUniqueId();
            loaded.remove(uuid);
            CompletableFuture<Void> last = pending.get(uuid);
            return last == null ? CompletableFuture.completedFuture(null) : last;
        });
        async(() -> {
            try {
                db.createTables();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Tables des séries non vérifiées (base injoignable ou droits manquants) : " + e.getMessage());
            }
        });
    }

    /** Attend les écritures en cours (arrêt du plugin) puis ferme la base. */
    public void close() {
        for (CompletableFuture<Void> last : pending.values()) {
            try {
                last.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("Des décorations n'ont pas fini d'être écrites en base à l'arrêt : " + e);
            }
        }
        db.close();
    }

    /** Vrai quand les décorations du joueur sont chargées depuis la base. */
    public boolean isLoaded(UUID uuid) {
        return loaded.contains(uuid);
    }

    // ------------------------------------------------------------------ lecture à l'arrivée

    /** Charge les décorations du joueur une fois que le serveur de départ a fini de les écrire. */
    public void load(UUID uuid) {
        int wait = Math.max(1, plugin.getConfig().getInt("reseau.attente-depart-secondes", 30));
        bus.awaitSaved(uuid, Duration.ofSeconds(wait)).whenComplete((saved, error) -> async(() -> read(uuid, 1)));
    }

    private void read(UUID uuid, int attempt) {
        Map<String, Long> items;
        try {
            items = db.load(uuid);
        } catch (SQLException e) {
            if (attempt < ATTEMPTS && plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> read(uuid, attempt + 1), 100L);
            } else {
                plugin.getLogger().log(Level.SEVERE, "Décorations de " + uuid + " illisibles, base injoignable : " + e.getMessage());
            }
            return;
        }
        sync(() -> {
            // Le joueur a pu quitter pendant la lecture : on ne garde pas ses données en mémoire
            if (Bukkit.getPlayer(uuid) == null) {
                return;
            }
            plugin.getPlayerManager().applyLoaded(uuid, items);
            loaded.add(uuid);
            plugin.getLogger().info("Données de série chargées pour le joueur " + uuid);
        });
    }

    // ------------------------------------------------------------------ écritures

    /** Écrit tout de suite la décoration que le joueur vient d'enregistrer (fil principal). */
    public void recordItem(UUID uuid, String item, long date) {
        enqueue(uuid, () -> {
            if (!retry(() -> db.insertItem(uuid, item, date))) {
                plugin.getLogger().severe("Décoration " + item + " de " + uuid + " non enregistrée en base (date " + date + ")");
            }
        });
    }

    /**
     * Série terminée par ce joueur (fil principal) : la récompense n'est donnée que si ce serveur est le premier à la
     * réserver en base. Sans réponse de la base après plusieurs essais, rien n'est donné et une ligne est écrite dans la console.
     */
    public void completeSeries(Player player, Collection collection) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        enqueue(uuid, () -> {
            boolean[] first = new boolean[1];
            boolean done = retry(() -> {
                first[0] = db.claimReward(uuid, collection.getId(), System.currentTimeMillis());
                return true;
            });
            if (!done) {
                plugin.getLogger().severe("Récompense de la série " + collection.getId() + " non donnée à " + name + " : base injoignable");
            } else if (first[0]) {
                sync(() -> plugin.grantSeries(player, collection));
            }
        });
    }

    private interface SqlAction {
        boolean run() throws SQLException;
    }

    /** Rejoue l'écriture quelques fois si la base ne répond pas. Rend faux si elle n'a jamais abouti. */
    private boolean retry(SqlAction action) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                action.run();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Écriture des séries impossible (essai " + attempt + "/" + ATTEMPTS + ") : " + e.getMessage());
                try {
                    Thread.sleep(RETRY_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void enqueue(UUID uuid, Runnable task) {
        CompletableFuture<Void> next = pending.compute(uuid, (k, previous) -> {
            CompletableFuture<Void> base = previous == null ? CompletableFuture.completedFuture(null) : previous;
            return base.handle((v, e) -> (Void) null).thenRunAsync(task, this::async);
        });
        next.whenComplete((v, e) -> pending.remove(uuid, next));
    }

    // ------------------------------------------------------------------ importation de l'ancienne base SQLite

    /** Copie playerdata.db dans la base partagée (hors fil principal), puis rend le bilan sur le fil principal. */
    public void importLocal(Consumer<ImportResult> done, Consumer<String> failed) {
        File file = new File(plugin.getDataFolder(), "playerdata.db");
        async(() -> {
            if (!file.isFile()) {
                sync(() -> failed.accept("playerdata.db introuvable"));
                return;
            }
            Map<UUID, Map<String, Long>> data = new HashMap<>();
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT uuid, completed_item, completion_date FROM player_collections")) {
                while (rs.next()) {
                    try {
                        data.computeIfAbsent(UUID.fromString(rs.getString(1)), k -> new HashMap<>()).put(rs.getString(2), rs.getLong(3));
                    } catch (IllegalArgumentException invalid) {
                        plugin.getLogger().warning("Uuid invalide ignoré dans playerdata.db : " + rs.getString(1));
                    }
                }
                ImportResult result = db.importItems(data);
                sync(() -> done.accept(result));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Importation de playerdata.db impossible", e);
                sync(() -> failed.accept(e.getMessage()));
            }
        });
    }

    private void async(Runnable task) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }

    private void sync(Runnable task) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
