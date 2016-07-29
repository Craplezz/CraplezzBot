package me.mani.clbot.music;

import me.mani.clcore.ClickManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Created by Schuckmann on 15.05.2016.
 */
public class Menu {

    protected Inventory inventory;
    protected ClickManager.ClickListener[] clickListeners = new ClickManager.ClickListener[256];

    public void open(Player player) {
        player.openInventory(inventory);
        ClickManager clickManager = ClickManager.getClickManager(player);
        clickManager.setClickListeners(clickListeners);
        clickManager.setInventory(inventory);
        clickManager.addInventoryCloseListener((closeEvent) -> close());
    }

    public void close() {}

}
