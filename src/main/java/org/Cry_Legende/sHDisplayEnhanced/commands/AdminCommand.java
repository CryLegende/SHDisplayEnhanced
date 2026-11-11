package org.Cry_Legende.sHDisplayEnhanced.commands;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * /shdisplayenhanced reload | import | info &lt;joueur&gt;. La console peut tout utiliser.
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_RELOAD = "shdisplayenhanced.admin.reload";
    private static final String PERMISSION_IMPORT = "shdisplayenhanced.admin.import";
    private static final String PERMISSION_INFO = "shdisplayenhanced.admin.info";

    private final SHDisplayEnhanced plugin;

    public AdminCommand(SHDisplayEnhanced plugin) {
        this.plugin = plugin;
    }

    /** Sous-commandes que l'expéditeur a le droit d'utiliser ; import n'existe qu'avec SHNetwork. */
    private List<String> allowed(CommandSender sender) {
        List<String> subs = new ArrayList<>();
        if (sender.hasPermission(PERMISSION_RELOAD)) subs.add("reload");
        if (plugin.getNetwork() != null && sender.hasPermission(PERMISSION_IMPORT)) subs.add("import");
        if (sender.hasPermission(PERMISSION_INFO)) subs.add("info");
        return subs;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> subs = allowed(sender);
        if (subs.isEmpty()) {
            sender.sendMessage(plugin.message("no_permission"));
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("reload") && args.length == 1 && subs.contains("reload")) {
            reload(sender);
        } else if (sub.equals("import") && args.length == 1 && subs.contains("import")) {
            importLocal(sender);
        } else if (sub.equals("info") && args.length == 2 && subs.contains("info")) {
            info(sender, args[1]);
        } else {
            sender.sendMessage(plugin.message("usage_list").replace("%subcommands%", String.join("|", subs)));
        }
        return true;
    }

    private void reload(CommandSender sender) {
        String error = plugin.reloadAll();
        if (error == null) {
            sender.sendMessage(plugin.getConfigManager().getReloadMessage());
            if (plugin.networkSettingsChanged()) {
                sender.sendMessage(plugin.message("reload_network"));
            }
        } else {
            String[] parts = error.split(" : ", 2);
            sender.sendMessage(plugin.message("reload_error").replace("%file%", parts[0]).replace("%reason%", parts.length > 1 ? parts[1] : ""));
        }
    }

    private void importLocal(CommandSender sender) {
        sender.sendMessage(plugin.message("import_started"));
        plugin.getNetwork().importLocal(
            result -> sender.sendMessage(plugin.message("import_done")
                .replace("%players%", String.valueOf(result.players()))
                .replace("%added%", String.valueOf(result.added()))
                .replace("%present%", String.valueOf(result.present()))),
            reason -> sender.sendMessage(plugin.message("import_failed").replace("%reason%", String.valueOf(reason))));
    }

    /**
     * Progression d'un joueur par série. Joueur en ligne : sa mémoire ; sinon lecture de la base hors du fil principal
     * (nom déjà connu du serveur ou UUID, jamais de requête Mojang).
     */
    private void info(CommandSender sender, String name) {
        UUID uuid;
        String display;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            uuid = online.getUniqueId();
            display = online.getName();
        } else {
            UUID parsed = parseUuid(name);
            OfflinePlayer known = parsed == null ? Bukkit.getOfflinePlayerIfCached(name) : null;
            if (parsed != null) {
                uuid = parsed;
                display = name;
            } else if (known != null) {
                uuid = known.getUniqueId();
                display = known.getName() != null ? known.getName() : name;
            } else {
                sender.sendMessage(plugin.message("player_unknown").replace("%player%", name));
                return;
            }
        }
        if (online != null) {
            Map<String, Long> items = plugin.getPlayerManager().snapshot(uuid);
            if (items != null) {
                report(sender, display, items, null);
                return;
            }
        }
        final UUID target = uuid;
        final String shown = display;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Long> items;
            Set<String> rewards = null;
            try {
                if (plugin.getNetwork() != null) {
                    items = plugin.getNetwork().readItems(target);
                    rewards = plugin.getNetwork().readRewards(target);
                } else {
                    items = plugin.getDatabaseManager().getCompletedItems(target);
                }
            } catch (java.sql.SQLException e) {
                items = null;
            }
            Map<String, Long> finalItems = items;
            Set<String> finalRewards = rewards;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalItems == null) {
                    sender.sendMessage(plugin.message("info_unreadable").replace("%player%", shown));
                } else {
                    report(sender, shown, finalItems, finalRewards);
                }
            });
        });
    }

    private static UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Affiche la progression : décorations enregistrées par série, séries terminées, récompense payée (réseau). */
    private void report(CommandSender sender, String player, Map<String, Long> items, Set<String> rewards) {
        Map<String, Collection> collections = new TreeMap<>(plugin.getCollectionManager().getAllCollections());
        sender.sendMessage(plugin.message("info_title").replace("%player%", player));
        int completed = 0;
        int counted = 0;
        for (Collection collection : collections.values()) {
            String prefix = collection.getId() + ".";
            long done = items.keySet().stream().filter(key -> key.startsWith(prefix)).count();
            counted += done;
            int total = collection.getCollectionItems().size();
            boolean finished = total > 0 && done >= total;
            if (finished) completed++;
            String state = "";
            if (finished) {
                state = plugin.message("info_series_done");
                if (rewards != null) {
                    state += plugin.message(rewards.contains(collection.getId()) ? "info_reward_paid" : "info_reward_unpaid");
                }
            }
            sender.sendMessage(plugin.message("info_series")
                .replace("%series%", collection.getId()).replace("%done%", String.valueOf(done))
                .replace("%total%", String.valueOf(total)).replace("%state%", state));
        }
        long other = items.size() - counted;
        if (other > 0) {
            sender.sendMessage(plugin.message("info_other").replace("%count%", String.valueOf(other)));
        }
        sender.sendMessage(plugin.message("info_total")
            .replace("%completed%", String.valueOf(completed)).replace("%series%", String.valueOf(collections.size()))
            .replace("%items%", String.valueOf(items.size())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            for (String sub : allowed(sender)) {
                if (sub.startsWith(start)) result.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info") && sender.hasPermission(PERMISSION_INFO)) {
            String start = args[1].toLowerCase(Locale.ROOT);
            Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(start)).sorted(Comparator.naturalOrder()).forEach(result::add);
        }
        return result;
    }
}
