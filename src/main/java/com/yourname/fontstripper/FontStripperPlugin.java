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

        // Register the open/close inventory listener
        getServer().getPluginManager().registerEvents(new InventoryStateHandler(), this);

        // Register the packet interceptor
        ItemPacketEventsInterceptor.register(this);

        getLogger().info("[FontStripper] Successfully initialized.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}
