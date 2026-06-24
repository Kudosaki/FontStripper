package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientCommand;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryStateHandler implements Listener, PacketListener {
    public static final Set<UUID> openInventories = new HashSet<>();
    private final JavaPlugin plugin;

    public InventoryStateHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void registerPacketListener(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new InventoryStateHandler(plugin));
    }

    // 1. Detect Standard Inventory ('E' Key)
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_COMMAND) {
            WrapperPlayClientClientCommand wrapper = new WrapperPlayClientClientCommand(event);
            if (wrapper.getAction() == ClientCommand.OPEN_INVENTORY) {
                Player player = (Player) event.getPlayer();
                openInventories.add(player.getUniqueId());
                plugin.getLogger().info("[Debug] Inventory opened (Packet) for: " + player.getName());
                triggerRefresh(player);
            }
        }
    }

    // 2. Detect Container Inventories
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.add(player.getUniqueId());
        plugin.getLogger().info("[Debug] Inventory opened (Event) for: " + player.getName());
        triggerRefresh(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
        plugin.getLogger().info("[Debug] Inventory closed for: " + player.getName());
    }

    private void triggerRefresh(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (int slot = 36; slot <= 44; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null) {
                    // Send AIR to clear cache
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSetSlot(0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.AIR))));
                    // Send Item to set state
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSetSlot(0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(item)));
                }
            }
        }, 5L);
    }
}
