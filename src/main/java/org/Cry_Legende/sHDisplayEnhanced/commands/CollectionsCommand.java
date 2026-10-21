package org.Cry_Legende.sHDisplayEnhanced.commands;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.gui.MainMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CollectionsCommand implements CommandExecutor {

    private final SHDisplayEnhanced plugin;

    public CollectionsCommand(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getPlayerOnlyMessage());
            return true;
        }
        Player player = (Player) sender;

        // Ouvre le nouveau menu principal au lieu de l'ancienne liste de collections
        new MainMenuGUI(player, plugin).open();
        return true;
    }
}