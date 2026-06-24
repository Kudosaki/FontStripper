package com.yourname.fontstripper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public class FontStripperPlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        // Register standard listeners
        getServer().getPluginManager().registerEvents(new InventoryStateHandler(this), this);
        // Register packet listeners
        ItemPacketEventsInterceptor.register(this);
        InventoryStateHandler.registerPacketListener(this);
        
        getLogger().info("[FontStripper] Fully initialized.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}
