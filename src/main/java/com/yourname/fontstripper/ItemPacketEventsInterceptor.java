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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ItemPacketEventsInterceptor implements PacketListener {

    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 44;

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(
            new ItemPacketEventsInterceptor(),
            PacketListenerPriority.HIGHEST
        );
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Log every single packet — no conditions
        Bukkit.getLogger().info("[FontStripper] ANY PACKET: " + event.getPacketType());

        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        Bukkit.getLogger().info("[FontStripper] SET_SLOT hit — windowId=" + wrapper.getWindowId() + " slot=" + wrapper.getSlot());

        if (!(event.getPlayer() instanceof Player)) return;

        if (wrapper.getWindowId() != 0) return;
        int slot = wrapper.getSlot();
        if (slot < HOTBAR_START || slot > HOTBAR_END) return;

        ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
        if (item == null || item.getType().isAir()) return;

        var meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return;

        String plainName = PLAIN.serialize(nameComponent);
        String jsonName = GSON.serialize(nameComponent);

        Bukkit.getLogger().info("[FontStripper] PLAIN: " + plainName);
        Bukkit.getLogger().info("[FontStripper] JSON: " + jsonName);

        StringBuilder codepoints = new StringBuilder();
        for (char c : plainName.toCharArray()) {
            codepoints.append(String.format("U+%04X ", (int) c));
        }
        Bukkit.getLogger().info("[FontStripper] CODEPOINTS: " + codepoints);
    }
}
