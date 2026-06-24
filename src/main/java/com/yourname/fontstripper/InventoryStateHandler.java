package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        plugin.getLogger().info("[Debug] Inventory opened for: " + player.getName() + ". Adding to openInventories.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getLogger().info("[Debug] Running Clear-then-Set refresh for: " + player.getName());

            for (int slot = 36; slot <= 44; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null) {
                    // 1. CLEAR: Air
                    WrapperPlayServerSetSlot clearPacket = new WrapperPlayServerSetSlot(
                        0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.AIR))
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, clearPacket);

                    // 2. SET: Item
                    WrapperPlayServerSetSlot setPacket = new WrapperPlayServerSetSlot(
                        0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(item)
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, setPacket);
                    
                    plugin.getLogger().info("[Debug] Refreshed slot " + slot + " for " + player.getName());
                }
            }
        }, 5L); 
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
        plugin.getLogger().info("[Debug] Inventory closed for: " + player.getName() + ". Removing from openInventories.");
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }
}
