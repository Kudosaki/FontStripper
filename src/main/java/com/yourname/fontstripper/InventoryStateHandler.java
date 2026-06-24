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
        
        // Force an item packet refresh 1 tick later so the GUI renders the unstripped items
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
        
        // Force a packet refresh so the hotbar switches back to stripped mode
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
