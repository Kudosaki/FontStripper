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
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // --- DEBUG LOGGING ---
        String invType = event.getInventory().getType().name();
        Bukkit.getLogger().info("[FontStripper Debug] " + player.getName() + " opened inventory of type: " + invType);
        // ---------------------

        // Track that the player has an active GUI screen open
        openInventories.add(player.getUniqueId());
        
        // Force the server to re-sync item values to the client 1 tick later.
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // --- DEBUG LOGGING ---
        Bukkit.getLogger().info("[FontStripper Debug] " + player.getName() + " closed inventory.");
        // ---------------------

        openInventories.remove(player.getUniqueId());
        
        // Force a re-sync now that the GUI is shut to clean out the hotbar cache immediately
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
