package com.yourname.fontstripper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryStateHandler implements Listener {
    public static final Set<UUID> openInventories = new HashSet<>();
    private final JavaPlugin plugin;

    public InventoryStateHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.add(player.getUniqueId());

        // We use a 3-tick delay to ensure the inventory packet has been processed 
        // by the client's internal UI thread before we force a refresh.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // This triggers a full re-sync of the inventory data
                player.updateInventory();
                
                // FORCE REFRESH: By accessing the inventory explicitly, 
                // we tell the client to re-render the slots.
                player.openInventory(player.getOpenInventory().getTopInventory());
            }
        }, 3L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());

        // Standard refresh on close to re-apply the filter immediately
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        }, 1L);
    }
}
