package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
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

        // Check if inventory is open
        boolean isOpen = InventoryStateHandler.openInventories.contains(player.getUniqueId());

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (wrapper.getSlot() < 36 || wrapper.getSlot() > 44) return;

            ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
            boolean hasFont = item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                             && item.getItemMeta().displayName().toString().matches(".*[\uE000-\uF8FF].*");

            if (isOpen) {
                // RESTORE LOGIC: If it's open, ensure we are sending the original item.
                // We force the packet to contain the original (unstripped) item.
                if (hasFont) {
                    Bukkit.getLogger().info("[FontStripper Debug] RESTORING original item for " + player.getName() + " in slot " + wrapper.getSlot());
                }
                // We don't strip. We send the original packet through (which is the original item).
            } else {
                // FILTER LOGIC: Inventory is closed, strip it.
                if (hasFont) {
                    Bukkit.getLogger().info("[FontStripper Debug] FILTERING (Stripping) item for " + player.getName() + " in slot " + wrapper.getSlot());
                    ItemStack stripped = strip(item);
                    if (stripped != null) {
                        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
                    }
                }
            }
        }
    }

    private static ItemStack strip(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        meta.displayName(meta.displayName().replaceText(b -> b.match(UNICODE_FONT_PATTERN).replacement("")));
        clone.setItemMeta(meta);
        return clone;
    }
}
