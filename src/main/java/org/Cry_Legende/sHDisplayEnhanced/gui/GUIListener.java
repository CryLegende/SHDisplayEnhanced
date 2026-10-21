package org.Cry_Legende.sHDisplayEnhanced.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ignorer si aucun inventaire n'est ouvert
        if (event.getInventory() == null) {
            return;
        }

        // On vérifie si l'inventaire supérieur (le GUI) est bien une de nos interfaces.
        InventoryHolder topHolder = event.getInventory().getHolder();
        if (!(topHolder instanceof AbstractGUI)) {
            return;
        }

        AbstractGUI gui = (AbstractGUI) topHolder;

        // --- CORRECTION DU BUG SHIFT+CLIC ---
        // Si le joueur clique dans son propre inventaire avec SHIFT,
        // l'action est "MOVE_TO_OTHER_INVENTORY".
        // On annule systématiquement cette action pour éviter que l'item ne disparaisse dans notre GUI.
        if (event.getClickedInventory() != null && !(event.getClickedInventory().getHolder() instanceof AbstractGUI) && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        // On vérifie si le clic a eu lieu dans notre GUI.
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof AbstractGUI) {
            // On annule l'événement pour empêcher le joueur de prendre ou de placer
            // des objets librement dans le GUI. Seules nos actions définies seront exécutées.
            event.setCancelled(true);

            // On passe l'événement à notre gestionnaire de clics personnalisé.
            gui.handleClick(event);
        }

        // Si le clic a lieu en dehors du GUI (dans l'inventaire du joueur, sans shift),
        // on ne fait rien et on laisse le comportement par défaut (ex: prendre un item sur le curseur).
    }
}