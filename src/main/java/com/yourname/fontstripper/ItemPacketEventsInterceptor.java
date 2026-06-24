package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ItemPacketEventsInterceptor implements PacketListener {

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(
            new ItemPacketEventsInterceptor(),
            PacketListenerPriority.HIGHEST
        );
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        Bukkit.getLogger().info("[FontStripper] SET_SLOT for slot: " + wrapper.getSlot());

        ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
        if (item == null || item.getType().isAir()) return;

        var meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            Bukkit.getLogger().info("[FontStripper] No meta/display name.");
            return;
        }

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return;

        // Serialize to JSON to detect Adventure font styling
        String nameJson = GsonComponentSerializer.gson().serialize(nameComponent);
        Bukkit.getLogger().info("[FontStripper] Name JSON: " + nameJson);

        boolean hasCustomFont = nameJson.contains("\"font\"");

        if (!hasCustomFont) {
            Bukkit.getLogger().info("[FontStripper] No custom font detected.");
            return;
        }

        Bukkit.getLogger().info("[FontStripper] Custom font found. Checking inventory state...");

        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) {
            // Inventory is open — send the item as-is with font intact
            Bukkit.getLogger().info("[FontStripper] Inventory OPEN — keeping font.");
        } else {
            // Hotbar / closed inventory — strip the font
            Bukkit.getLogger().info("[FontStripper] Inventory CLOSED — stripping font.");
            ItemStack stripped = stripFont(item);
            wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
        }
    }

    /**
     * Clones the item and recursively removes all font styling from its display name.
     */
    private static ItemStack stripFont(ItemStack item) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        if (meta == null) return clone;

        Component original = meta.displayName();
        if (original == null) return clone;

        Component stripped = removeFontRecursive(original);
        meta.displayName(stripped);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Recursively walks the Component tree and nulls out any font key,
     * preserving all other styling (color, bold, italic, etc.) and text content.
     */
    private static Component removeFontRecursive(Component component) {
        // Remove only the font from this component's style, keep everything else
        Component result = component.style(
            component.style().toBuilder().font(null).build()
        );

        // Recurse into children
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            List<Component> strippedChildren = children.stream()
                .map(ItemPacketEventsInterceptor::removeFontRecursive)
                .toList();
            result = result.children(strippedChildren);
        }

        return result;
    }
}
