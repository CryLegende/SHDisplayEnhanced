package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.Cry_Legende.sHDisplayEnhanced.SHDisplayEnhanced;
import org.Cry_Legende.sHDisplayEnhanced.config.ConfigManager;
import org.Cry_Legende.sHDisplayEnhanced.managers.PlayerManager;
import org.Cry_Legende.sHDisplayEnhanced.model.Collection;
import org.Cry_Legende.sHDisplayEnhanced.model.CollectionItem;
import org.Cry_Legende.sHDisplayEnhanced.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SingleCollectionGUI extends AbstractGUI {

    private final Collection collection;
    private final SHDisplayEnhanced plugin;
    private final PlayerManager playerManager;
    private final ConfigManager configManager;

    public SingleCollectionGUI(Player player, Collection collection, SHDisplayEnhanced plugin) {
        super(player, collection.getGuiSize(), collection.getGuiTitle());
        this.collection = collection;
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void build() {
        actions.clear();

        ItemStack filler = configManager.getFillerItem();
        for (int i = 0; i < collection.getGuiSize(); i++) {
            if (i < 9 || i > collection.getGuiSize() - 10 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inventory.setItem(i, filler);
            }
        }

        for (Map.Entry<Integer, ItemStack> entry : collection.getDecorativeItems().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }

        for (CollectionItem collectionItem : collection.getCollectionItems().values()) {
            if (playerManager.hasCompleted(player.getUniqueId(), collection.getId(), collectionItem.getId())) {
                addUnlockedItem(collectionItem);
            } else {
                addLockedItem(collectionItem);
            }
        }

        if (collection.getRewardDisplayLore() != null && !collection.getRewardDisplayLore().isEmpty()) {
            ItemStack rewardInfo = configManager.getRewardInfoItem();
            ItemMeta meta = rewardInfo.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                collection.getRewardDisplayLore().forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', "&7- " + line)));
                meta.setLore(lore);
                rewardInfo.setItemMeta(meta);
            }
            inventory.setItem(4, rewardInfo);
        }

        inventory.setItem(49, configManager.getBackButton());
        actions.put(49, (event) -> {
            player.playSound(player.getLocation(), configManager.getClickSound(), 1f, 1f);
            new CollectionsListGUI(player, plugin).open();
        });
    }

    private void addUnlockedItem(CollectionItem collectionItem) {
        ItemStack unlockedDisplay = collectionItem.getItemStack().clone();
        ItemMeta meta = unlockedDisplay.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        long completionDate = playerManager.getCompletionDate(player.getUniqueId(), collection.getId(), collectionItem.getId());
        String formattedDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date(completionDate));
        for (String line : configManager.getUnlockedLore()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%date%", formattedDate)));
        }
        meta.setLore(lore);
        unlockedDisplay.setItemMeta(meta);
        inventory.setItem(collectionItem.getSlot(), unlockedDisplay);
    }

    private void addLockedItem(CollectionItem collectionItem) {
        ItemStack lockedDisplay = configManager.getLockedItem().clone();
        ItemMeta meta = lockedDisplay.getItemMeta();
        String requiredItemName = collectionItem.getItemStack().hasItemMeta() && collectionItem.getItemStack().getItemMeta().hasDisplayName()
            ? collectionItem.getItemStack().getItemMeta().getDisplayName()
            : collectionItem.getItemStack().getType().name();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', configManager.getLockedItemName().replace("%item_name%", requiredItemName)));
        List<String> lore = configManager.getLockedItemLore().stream()
            .map(line -> ChatColor.translateAlternateColorCodes('&', line.replace("%item_name%", requiredItemName)))
            .collect(Collectors.toList());
        meta.setLore(lore);
        lockedDisplay.setItemMeta(meta);
        inventory.setItem(collectionItem.getSlot(), lockedDisplay);

        actions.put(collectionItem.getSlot(), (event) -> {
            // --- LOGIQUE AMÉLIORÉE ---
            // On s'assure que le joueur a bien un item sur son curseur.
            ItemStack cursorItem = event.getCursor();

            if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§cPrenez l'objet requis dans votre inventaire et placez-le ici.");
                return;
            }

            if (ItemUtils.isSimilar(cursorItem, collectionItem.getItemStack())) {
                completeItem(collectionItem, cursorItem);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§cVous n'avez pas la bonne décoration sur votre curseur. Requis : " + requiredItemName);
            }
        });
    }

    private void completeItem(CollectionItem collectionItem, ItemStack itemToConsume) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        playerManager.setCompleted(player.getUniqueId(), collection.getId(), collectionItem.getId());

        // Réduit la quantité de l'item sur le curseur.
        itemToConsume.setAmount(itemToConsume.getAmount() - 1);

        refresh();

        if (playerManager.hasCompletedCollection(player.getUniqueId(), collection)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage("§aVous avez terminé la série '" + collection.getId() + "' !");
            collection.executeRewardCommands(player);
        }
    }

    // Cette méthode n'est plus nécessaire car on force l'utilisation du curseur.
    // private ItemStack findItemInInventory(ItemStack requiredItem) { ... }

    private void refresh() {
        // Sauvegarde la position du curseur car inventory.clear() peut l'affecter.
        ItemStack cursor = player.getItemOnCursor();
        inventory.clear();
        build();
        player.setItemOnCursor(cursor); // Restaure l'item sur le curseur
    }
}
