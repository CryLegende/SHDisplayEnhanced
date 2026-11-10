package org.Cry_Legende.sHDisplayEnhanced.network;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Ce dont SHDisplayEnhanced a besoin de SHNetwork. Seule {@link ShNetworkBus} le connaît : sans lui,
 * aucune classe de ce paquet n'est chargée.
 */
public interface NetBus {

    /** Inscrit la sauvegarde exécutée au départ de chaque joueur, avant qu'il arrive sur un autre serveur. */
    void registerSaveTask(Function<Player, CompletableFuture<Void>> task);

    /**
     * Attend que le serveur de départ ait fini de sauvegarder le joueur. Le futur se termine hors du fil principal,
     * sur un fil à ne jamais bloquer.
     */
    CompletableFuture<Boolean> awaitSaved(UUID uuid, Duration timeout);
}
