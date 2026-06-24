package com.yourname.fontstripper;

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

    public InventoryStateHandler(JavaPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.add(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
        // Force refresh to strip font immediately after closing
        player.updateInventory();
    }
}
