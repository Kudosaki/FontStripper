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
        plugin.getLogger().info("[FontStripper] Interceptor registered successfully.");
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // DIAGNOSTIC: Log every SET_SLOT packet so we can see what slot ID it is
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            
            // Log the slot ID! This will tell us if your hotbar is actually 36-44
            Bukkit.getLogger().info("[FontStripper Diagnostic] Received SET_SLOT packet for slot: " + wrapper.getSlot());

            ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
            
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                Component name = item.getItemMeta().displayName();
                if (name != null && name.toString().matches(".*[\uE000-\uF8FF].*")) {
                    Bukkit.getLogger().info("[FontStripper Diagnostic] Found item with custom font! Stripping...");
                    
                    // Force strip
                    ItemStack stripped = strip(item);
                    if (stripped != null) {
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
