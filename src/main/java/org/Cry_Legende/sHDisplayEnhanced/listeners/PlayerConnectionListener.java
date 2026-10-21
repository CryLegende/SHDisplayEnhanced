package org.Cry_Legende.sHDisplayEnhanced.listeners;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final SHDisplayEnhanced plugin;

    public PlayerConnectionListener(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().savePlayerData(event.getPlayer().getUniqueId());
    }
}
