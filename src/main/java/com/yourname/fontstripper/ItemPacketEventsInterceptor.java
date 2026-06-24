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

    private static final int HOTBAR_START = 36;
    private static final int OFFHAND_SLOT = 45; // Covers 36-44 (Hotbar) and 45 (Offhand)

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

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            handleWindowItems(event, player);
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            handleSetSlot(event, player);
        }
    }

    private void handleWindowItems(PacketSendEvent event, Player player) {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
        if (wrapper.getWindowId() != 0) return;
        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) return;

        List<ItemStack> items = wrapper.getItems();
        List<ItemStack> modified = new ArrayList<>(items);
        boolean changed = false;

        // Strip only the hotbar and offhand slots when the inventory is closed
        for (int i = HOTBAR_START; i <= OFFHAND_SLOT && i < modified.size(); i++) {
            ItemStack original = modified.get(i);
            ItemStack processed = processItem(original);
            
            if (original != processed) {
                modified.set(i, processed);
                changed = true;
            }
        }

        if (changed) wrapper.setItems(modified);
    }

    private void handleSetSlot(PacketSendEvent event, Player player) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) return;

        int windowId = wrapper.getWindowId();
        int slot = wrapper.getSlot();

        // Target normal hotbar/offhand updates (0) OR floating cursor items (-1)
        if (windowId == 0 && (slot < HOTBAR_START || slot > OFFHAND_SLOT)) return;
        if (windowId != 0 && windowId != -1) return;

        ItemStack original = wrapper.getItem();
        ItemStack processed = processItem(original);

        if (original != processed) {
            wrapper.setItem(processed);
        }
    }

    // --- Helper Methods ---

    /**
     * Checks if the item has the custom font. If it does, strips it and returns the new item.
     * If not, returns the exact original item untouched.
     */
    private ItemStack processItem(ItemStack peItem) {
        if (peItem == null || peItem.isEmpty()) return peItem;

        org.bukkit.inventory.ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(peItem);
        if (bukkit == null || bukkit.getType().isAir()) return peItem;

        var meta = bukkit.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return peItem;

        Component nameComponent = meta.displayName();
        if (nameComponent == null) return peItem;

        String plainName = PLAIN.serialize(nameComponent);
        if (!PUA_PATTERN.matcher(plainName).find()) return peItem;

        org.bukkit.inventory.ItemStack stripped = stripFont(bukkit, nameComponent);
        return SpigotConversionUtil.fromBukkitItemStack(stripped);
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

        if (component instanceof TextComponent tc) {
            String cleaned = PUA_PATTERN.matcher(tc.content()).replaceAll("");
            result = tc.content(cleaned);
        }

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
