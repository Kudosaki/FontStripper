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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
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

        // Debug: See if the packet is being let through
        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) {
            // We do NOT log here to prevent console spam
            return; 
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (wrapper.getSlot() < 36 || wrapper.getSlot() > 44) return;
            
            ItemStack stripped = strip(SpigotConversionUtil.toBukkitItemStack(wrapper.getItem()));
            if (stripped != null) {
                // plugin.getLogger().info("[Debug] Stripping slot " + wrapper.getSlot()); // Uncomment if you need to see stripping
                wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
            }
            
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> processed = new ArrayList<>();
            boolean changed = false;

            for (int i = 0; i < items.size(); i++) {
                if (i >= 36 && i <= 44) {
                    ItemStack stripped = strip(SpigotConversionUtil.toBukkitItemStack(items.get(i)));
                    if (stripped != null) { 
                        processed.add(SpigotConversionUtil.fromBukkitItemStack(stripped)); 
                        changed = true; 
                    } else { 
                        processed.add(items.get(i)); 
                    }
                } else { 
                    processed.add(items.get(i)); 
                }
            }
            if (changed) wrapper.setItems(processed);
        }
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
