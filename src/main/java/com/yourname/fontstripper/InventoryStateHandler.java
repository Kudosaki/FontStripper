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
        
        // Add player to the open set
        openInventories.add(player.getUniqueId());
        
        // Force the server to resend items so the font reappears in the GUI
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Remove player from the open set
        openInventories.remove(player.getUniqueId());
        
        // Force the server to resend items so the font is stripped in the hotbar
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
