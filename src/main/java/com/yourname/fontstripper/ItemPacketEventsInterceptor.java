package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import net.kyori.adventure.text.Component;
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
        // If inventory is open, we do nothing; let raw data pass through
        if (InventoryStateHandler.openInventories.contains(event.getPlayer().getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (wrapper.getSlot() < 36 || wrapper.getSlot() > 44) return;
            
            ItemStack stripped = strip(SpigotConversionUtil.toBukkitItemStack(wrapper.getItem()));
            if (stripped != null) wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
            
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> processed = new ArrayList<>();
            boolean changed = false;

            for (int i = 0; i < items.size(); i++) {
                if (i >= 36 && i <= 44) {
                    ItemStack stripped = strip(SpigotConversionUtil.toBukkitItemStack(items.get(i)));
                    if (stripped != null) { processed.add(SpigotConversionUtil.fromBukkitItemStack(stripped)); changed = true; }
                    else processed.add(items.get(i));
                } else processed.add(items.get(i));
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
