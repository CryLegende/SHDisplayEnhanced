package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractGUI implements InventoryHolder {

    protected final Inventory inventory;
    protected final Player player;
    protected final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public AbstractGUI(Player player, int size, String title) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    protected abstract void build();

    public void open() {
        build();
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (actions.containsKey(event.getSlot())) {
            actions.get(event.getSlot()).accept(event);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}