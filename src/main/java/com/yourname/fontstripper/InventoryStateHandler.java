package com.yourname.fontstripper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryStateHandler implements Listener {

    // Players whose inventories are currently open
    public static final Set<UUID> openInventories = new HashSet<>();

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.add(player.getUniqueId());
            System.out.println("[FontStripper] Inventory Opened: " + player.getName());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.remove(player.getUniqueId());
            System.out.println("[FontStripper] Inventory Closed: " + player.getName());
        }
    }
}
