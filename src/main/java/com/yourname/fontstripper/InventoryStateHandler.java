@Override
public void onPacketSend(PacketSendEvent event) {
    PacketType packetType = event.getPacketType();

    // 1. If it's a WINDOW_ITEMS packet (the full inventory sync), 
    // we NEVER strip. This ensures when you open your inventory, 
    // you see the beautiful custom icons exactly as you designed them.
    if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
        return; 
    }

    // 2. If it's a SET_SLOT packet, we only process it if it's the 
    // Hotbar slots (36-44) AND we are NOT looking at a container GUI.
    if (packetType == PacketType.Play.Server.SET_SLOT) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        int slot = wrapper.getSlot();

        // Only touch hotbar (36-44)
        if (slot < 36 || slot > 44) return;

        // NEW LOGIC: If the player IS looking at an inventory, 
        // we skip the strip entirely so the tooltip remains pristine.
        if (InventoryStateHandler.openInventories.contains(event.getPlayer().getUniqueId())) {
            return;
        }

        // Only strip if they are NOT looking at an inventory
        com.github.retrooper.packetevents.protocol.item.ItemStack peItem = wrapper.getItem();
        if (peItem == null || peItem.isEmpty()) return;

        ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
        ItemStack strippedItem = stripCustomFontIfPresent(bukkitItem);

        if (strippedItem != null) {
            wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(strippedItem));
        }
    }
}
