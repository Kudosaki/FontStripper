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
        
        getServer().getPluginManager().registerEvents(new InventoryStateHandler(this), this);
        ItemPacketEventsInterceptor.register(this);
        
        getLogger().info("FontStripper successfully armed for 1.21.1!");
    }
    
    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}
