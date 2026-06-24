package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public class ItemPacketEventsInterceptor implements PacketListener {
    // Regex for Private Use Area characters
    private static final Pattern UNICODE_FONT_PATTERN = Pattern.compile("[\uE000-\uF8FF]");

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new ItemPacketEventsInterceptor(), PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            
            // Log every SET_SLOT packet
            Bukkit.getLogger().info("[FilterDebug] SET_SLOT packet received for Slot: " + wrapper.getSlot());

            ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
            
            if (item == null || item.getType().isAir()) return;

            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                Component nameComponent = item.getItemMeta().displayName();
                // Use plain text for reliable regex matching
                String name = PlainTextComponentSerializer.plainText().serialize(nameComponent);
                
                Bukkit.getLogger().info("[FilterDebug] Checking item: " + item.getType() + " | Name: " + name);

                if (UNICODE_FONT_PATTERN.matcher(name).find()) {
                    Bukkit.getLogger().info("[FilterDebug] MATCH FOUND! Checking inventory state...");

                    if (!InventoryStateHandler.openInventories.contains(player.getUniqueId())) {
                        Bukkit.getLogger().info("[FilterDebug] Inventory CLOSED - STRIPPING FONT.");
                        ItemStack stripped = strip(item);
                        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
                    } else {
                        Bukkit.getLogger().info("[FilterDebug] Inventory OPEN - Skipping strip.");
                    }
                } else {
                    Bukkit.getLogger().info("[FilterDebug] No font pattern detected in name.");
                }
            } else {
                Bukkit.getLogger().info("[FilterDebug] No meta/display name on item.");
            }
        }
    }

    private static ItemStack strip(ItemStack item) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        // Get name, strip pattern, set back
        String name = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String cleanName = name.replaceAll("[\uE000-\uF8FF]", "");
        meta.displayName(net.kyori.adventure.text.Component.text(cleanName));
        clone.setItemMeta(meta);
        return clone;
    }
}
