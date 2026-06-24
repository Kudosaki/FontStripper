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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.regex.Pattern;

public class ItemPacketEventsInterceptor implements PacketListener {

    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 44;

    // Matches PUA Unicode characters — your custom font glyphs live here
    private static final Pattern FONT_PATTERN = Pattern.compile("[\uE000-\uF8FF]");

    // Cached serializer
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(
            new ItemPacketEventsInterceptor(),
            PacketListenerPriority.HIGHEST
        );
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);

        // Only care about player inventory hotbar slots
        if (wrapper.getWindowId() != 0) return;
        int slot = wrapper.getSlot();
        if (slot < HOTBAR_START || slot > HOTBAR_END) return;

        ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
        if (item == null || item.getType().isAir()) return;

        var meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return;

        // PUA characters survive plain text serialization just fine
        String plainName = PLAIN.serialize(nameComponent);
        if (!FONT_PATTERN.matcher(plainName).find()) return;

        // Strip PUA characters from the component tree and update packet
        ItemStack stripped = stripFont(item, nameComponent);
        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
    }

    private static ItemStack stripFont(ItemStack item, Component nameComponent) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        if (meta == null) return clone;
        meta.displayName(removePUARecursive(nameComponent));
        clone.setItemMeta(meta);
        return clone;
    }

    private static Component removePUARecursive(Component component) {
        // Strip PUA chars from this component's text content
        Component result = component;
        if (component instanceof net.kyori.adventure.text.TextComponent tc) {
            String cleaned = FONT_PATTERN.matcher(tc.content()).replaceAll("");
            result = tc.content(cleaned);
        }

        // Recurse into children
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            result = result.children(
                children.stream()
                    .map(ItemPacketEventsInterceptor::removePUARecursive)
                    .toList()
            );
        }

        return result;
    }
}
