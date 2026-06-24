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
        
        // This is the ONLY thing required to start filtering.
        ItemPacketEventsInterceptor.register(this);
        
        getLogger().info("[FontStripper] Diagnostic mode enabled. Every SET_SLOT packet will be logged.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}
