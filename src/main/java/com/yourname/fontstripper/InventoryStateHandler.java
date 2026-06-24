package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
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

        // 5-tick delay: Ensures the client UI thread has finished the 
        // initial packet handling before we override the cache.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // 1. Manual "SetSlot" refresh (Hotbar 36-44)
            // We force-send the 'Pretty' item to override the cached slot state.
            for (int slot = 36; slot <= 44; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null) {
                    WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(
                        0, // Window ID 0 (Player Inventory)
                        0, // State ID
                        slot, 
                        SpigotConversionUtil.fromBukkitItemStack(item)
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                }
            }

            // 2. Final "Flush": Forces the client to re-render the entire GUI view
            player.updateInventory();
            
        }, 5L); 
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());

        // Refresh on close to immediately re-apply the filter
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
