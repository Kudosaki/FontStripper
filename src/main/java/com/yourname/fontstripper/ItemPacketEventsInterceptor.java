package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ItemPacketEventsInterceptor implements PacketListener {

    // Hotbar slots in the full player inventory array (slots 36-44)
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 44;

    // PUA Unicode range — where custom font glyphs live
    private static final Pattern PUA_PATTERN = Pattern.compile("[\uE000-\uF8FF]");

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(
            new ItemPacketEventsInterceptor(),
            PacketListenerPriority.HIGHEST
        );
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Handle WINDOW_ITEMS — full inventory sync (fires on login, respawn, inventory open/close)
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            handleWindowItems(event, player);
            return;
        }

        // Handle SET_SLOT — single slot update (offhand, picked up items, etc.)
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            handleSetSlot(event, player);
        }
    }

    private void handleWindowItems(PacketSendEvent event, Player player) {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);

        // Only player inventory (windowId 0)
        if (wrapper.getWindowId() != 0) return;

        boolean inventoryOpen = InventoryStateHandler.openInventories.contains(player.getUniqueId());
        List<ItemStack> items = wrapper.getItems();
        List<ItemStack> modified = new ArrayList<>(items);
        boolean changed = false;

        for (int i = HOTBAR_START; i <= HOTBAR_END && i < modified.size(); i++) {
            ItemStack peItem = modified.get(i);
            if (peItem == null || peItem.isEmpty()) continue;

            org.bukkit.inventory.ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(peItem);
            if (bukkit == null || bukkit.getType().isAir()) continue;

            var meta = bukkit.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;

            Component nameComponent = meta.displayName();
            if (nameComponent == null) continue;

            String plainName = PLAIN.serialize(nameComponent);

            if (!PUA_PATTERN.matcher(plainName).find()) continue;

            if (inventoryOpen) {
                // Inventory is open — restore font (send as-is, no stripping)
                // Nothing to do, item already has font
            } else {
                // Hotbar visible — strip the font
                org.bukkit.inventory.ItemStack stripped = stripFont(bukkit, nameComponent);
                modified.set(i, SpigotConversionUtil.fromBukkitItemStack(stripped));
                changed = true;
            }
        }

        if (changed) {
            wrapper.setItems(modified);
        }
    }

    private void handleSetSlot(PacketSendEvent event, Player player) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);

        if (wrapper.getWindowId() != 0) return;
        int slot = wrapper.getSlot();
        if (slot < HOTBAR_START || slot > HOTBAR_END) return;

        boolean inventoryOpen = InventoryStateHandler.openInventories.contains(player.getUniqueId());
        if (inventoryOpen) return;

        ItemStack peItem = wrapper.getItem();
        if (peItem == null || peItem.isEmpty()) return;

        org.bukkit.inventory.ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(peItem);
        if (bukkit == null || bukkit.getType().isAir()) return;

        var meta = bukkit.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return;

        String plainName = PLAIN.serialize(nameComponent);
        if (!PUA_PATTERN.matcher(plainName).find()) return;

        org.bukkit.inventory.ItemStack stripped = stripFont(bukkit, nameComponent);
        wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(stripped));
    }

    private static org.bukkit.inventory.ItemStack stripFont(
        org.bukkit.inventory.ItemStack item,
        Component nameComponent
    ) {
        org.bukkit.inventory.ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        if (meta == null) return clone;
        meta.displayName(removePUARecursive(nameComponent));
        clone.setItemMeta(meta);
        return clone;
    }

    private static Component removePUARecursive(Component component) {
        Component result = component;

        // Strip PUA characters from text content
        if (component instanceof TextComponent tc) {
            String cleaned = PUA_PATTERN.matcher(tc.content()).replaceAll("");
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
