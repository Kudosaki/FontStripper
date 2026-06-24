package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.retrooper.packetevents.util.SpigotConversionUtil; // FIX: Updated to 'io.github...'
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// FIX: 'implements PacketListener' instead of extending the old abstract class
public class ItemPacketEventsInterceptor implements PacketListener {

    private static final Pattern UNICODE_FONT_PATTERN = Pattern.compile("[\uE000-\uF8FF]");

    public static void register(JavaPlugin plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(
                new ItemPacketEventsInterceptor(), 
                PacketListenerPriority.HIGHEST
        );
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        if (player == null) return;

        if (InventoryStateHandler.openInventories.contains(player.getUniqueId())) return;

        // FIX: Directly compare event.getPacketType() to prevent Java type-casting errors
        
        // 1. Single Slot Updates
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            int slot = wrapper.getSlot();

            if (slot < 36 || slot > 45) return;

            com.github.retrooper.packetevents.protocol.item.ItemStack peItem = wrapper.getItem();
            if (peItem == null || peItem.isEmpty()) return;

            ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
            ItemStack strippedItem = stripCustomFontIfPresent(bukkitItem);

            if (strippedItem != null) {
                wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(strippedItem));
            }
        } 
        // 2. Full Inventory Syncs
        else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> peItems = wrapper.getItems();
            
            if (peItems == null || peItems.isEmpty()) return;

            boolean modified = false;
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> processedList = new ArrayList<>(peItems.size());

            for (int i = 0; i < peItems.size(); i++) {
                com.github.retrooper.packetevents.protocol.item.ItemStack peItem = peItems.get(i);

                if (i < 36 || i > 45 || peItem == null || peItem.isEmpty()) {
                    processedList.add(peItem);
                    continue;
                }

                ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
                ItemStack strippedItem = stripCustomFontIfPresent(bukkitItem);

                if (strippedItem != null) {
                    processedList.add(SpigotConversionUtil.fromBukkitItemStack(strippedItem));
                    modified = true;
                } else {
                    processedList.add(peItem);
                }
            }

            if (modified) {
                wrapper.setItems(processedList);
            }
        }
    }

    private static ItemStack stripCustomFontIfPresent(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return null;
        
        Component displayName = meta.displayName();
        if (displayName == null) return null;

        String plainText = displayName.toString(); 
        boolean containsCustomUnicode = false;
        
        for (int i = 0; i < plainText.length(); i++) {
            char c = plainText.charAt(i);
            if (c >= '\uE000' && c <= '\uF8FF') {
                containsCustomUnicode = true;
                break;
            }
        }

        if (!containsCustomUnicode) return null;

        ItemStack clonedItem = item.clone();
        ItemMeta clonedMeta = clonedItem.getItemMeta();

        Component strippedName = displayName.replaceText(builder -> builder
                .match(UNICODE_FONT_PATTERN)
                .replacement("")
        );

        clonedMeta.displayName(strippedName);
        clonedItem.setItemMeta(clonedMeta);
        
        return clonedItem;
    }
}
