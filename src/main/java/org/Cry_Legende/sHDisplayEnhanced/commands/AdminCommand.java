package org.Cry_Legende.sHDisplayEnhanced.commands;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {

    private final SHDisplayEnhanced plugin;

    public AdminCommand(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("shdisplayenhanced.admin")) {
            sender.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getConfigManager().getReloadMessage());
        } else if (args.length > 0 && args[0].equalsIgnoreCase("import") && plugin.getNetwork() != null) {
            sender.sendMessage(plugin.message("import_started"));
            plugin.getNetwork().importLocal(
                result -> sender.sendMessage(plugin.message("import_done")
                    .replace("%players%", String.valueOf(result.players()))
                    .replace("%added%", String.valueOf(result.added()))
                    .replace("%present%", String.valueOf(result.present()))),
                reason -> sender.sendMessage(plugin.message("import_failed").replace("%reason%", String.valueOf(reason))));
        } else {
            sender.sendMessage(plugin.message(plugin.getNetwork() != null ? "usage_network" : "usage"));
        }
        return true;
    }
}