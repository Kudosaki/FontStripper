package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public class ItemPacketEventsInterceptor implements PacketListener {
    private static final Pattern UNICODE_FONT_PATTERN = Pattern.compile("[\uE000-\uF8FF]");

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new ItemPacketEventsInterceptor(), PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // 1. DETECTION: If we receive WINDOW_ITEMS, the inventory is OPEN.
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            InventoryStateHandler.openInventories.add(player.getUniqueId());
            return;
        }

        // 2. FILTERING: If the inventory is open, do nothing (restore original items)
        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) {
            return;
        }

        // 3. STRIPPING: If closed, strip the font
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            
            // Only target hotbar slots (36-44)
            if (wrapper.getSlot() >= 36 && wrapper.getSlot() <= 44) {
                ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
                
                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    Component name = item.getItemMeta().displayName();
                    if (name != null && name.toString().matches(".*[\uE000-\uF8FF].*")) {
                        // Debugging: confirm stripping
                        // System.out.println("[Debug] Stripping item in slot: " + wrapper.getSlot());
                        ItemStack stripped = strip(item);
                        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
                    }
                }
            }
        }
    }

    private static ItemStack strip(ItemStack item) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        meta.displayName(meta.displayName().replaceText(b -> b.match(UNICODE_FONT_PATTERN).replacement("")));
        clone.setItemMeta(meta);
        return clone;
    }
}
