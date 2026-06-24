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

        // We delay the force-update to ensure the client window is fully initialized
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Manual "SetSlot" refresh for the hotbar slots (36-44)
            // This forces the client to redraw these slots, bypassing the cache.
            for (int slot = 36; slot <= 44; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null) {
                    WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(
                        0, // Window ID 0 is the player inventory
                        slot, 
                        SpigotConversionUtil.fromBukkitItemStack(item)
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                }
            }
        }, 3L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());

        // When closing, a standard update is usually sufficient to re-apply the filter
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
