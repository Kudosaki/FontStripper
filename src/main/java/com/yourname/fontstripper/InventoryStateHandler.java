package com.yourname.fontstripper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryStateHandler implements Listener {
    public static final Set<UUID> openInventories = new HashSet<>();

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            openInventories.remove(event.getPlayer().getUniqueId());
             System.out.println("[Debug] Inventory Closed: " + event.getPlayer().getName());
        }
    }
}
