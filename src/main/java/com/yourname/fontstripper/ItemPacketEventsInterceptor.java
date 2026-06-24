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

    // In the player inventory container (window ID 0):
    // Slots 36-44 are the hotbar slots 0-8
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 44;

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

        int windowId = wrapper.getWindowId();
        int slot = wrapper.getSlot();

        Bukkit.getLogger().info("[FontStripper] SET_SLOT windowId=" + windowId + " slot=" + slot);

        // Only strip for player inventory (windowId 0), hotbar slots 36-44
        // Any other container (chest, furnace, etc.) = inventory is open, keep font
        if (windowId != 0 || slot < HOTBAR_START || slot > HOTBAR_END) {
            Bukkit.getLogger().info("[FontStripper] Not a hotbar slot — keeping font.");
            return;
        }

        ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
        if (item == null || item.getType().isAir()) return;

        var meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return;

        String nameJson = GsonComponentSerializer.gson().serialize(nameComponent);
        Bukkit.getLogger().info("[FontStripper] Name JSON: " + nameJson);

        if (!nameJson.contains("\"font\"")) {
            Bukkit.getLogger().info("[FontStripper] No custom font detected.");
            return;
        }

        Bukkit.getLogger().info("[FontStripper] Hotbar slot with custom font — stripping.");
        ItemStack stripped = stripFont(item);
        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
    }

    private static ItemStack stripFont(ItemStack item) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        if (meta == null) return clone;

        Component original = meta.displayName();
        if (original == null) return clone;

        meta.displayName(removeFontRecursive(original));
        clone.setItemMeta(meta);
        return clone;
    }

    private static Component removeFontRecursive(Component component) {
        Component result = component.style(
            component.style().toBuilder().font(null).build()
        );

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
