package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Pattern;

public class ItemPacketEventsInterceptor implements PacketListener {
    private static final Pattern UNICODE_FONT_PATTERN = Pattern.compile("[\uE000-\uF8FF]");
    private static final Set<UUID> pendingCacheBust = new HashSet<>();

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new ItemPacketEventsInterceptor(), PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            // DEBUG: Inventory Open signal via packet
            Bukkit.getLogger().info("[FontStripper Debug] WindowItems packet detected for " + player.getName() + " - Triggering Refresh");
            
            if (!pendingCacheBust.contains(player.getUniqueId())) {
                pendingCacheBust.add(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugins()[0], () -> pendingCacheBust.remove(player.getUniqueId()), 20L);
                triggerRefresh(player);
            }
            return;
        }

        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (wrapper.getSlot() >= 36 && wrapper.getSlot() <= 44) {
                ItemStack original = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
                ItemStack stripped = strip(original);
                
                if (stripped != null) {
                    // DEBUG: Filtering active
                    Bukkit.getLogger().info("[FontStripper Debug] Filtering font on item: " + original.getType());
                    wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
                }
            }
        }
    }

    private void triggerRefresh(Player player) {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugins()[0], () -> {
            for (int slot = 36; slot <= 44; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSetSlot(0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.AIR))));
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSetSlot(0, 0, slot, 
                        SpigotConversionUtil.fromBukkitItemStack(item)));
                }
            }
        }, 5L);
    }

    private static ItemStack strip(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;
        Component name = item.getItemMeta().displayName();
        if (name == null || !name.toString().matches(".*[\uE000-\uF8FF].*")) return null;
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        meta.displayName(name.replaceText(b -> b.match(UNICODE_FONT_PATTERN).replacement("")));
        clone.setItemMeta(meta);
        return clone;
    }
}
