package me.mani.clbot;

import me.mani.clapi.http.HttpConnection;
import me.mani.clapi.http.music.MusicConnection;
import me.mani.clbot.music.ActionBarDisplay;
import me.mani.clbot.music.MusicMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Schuckmann on 18.05.2016.
 */
public class Bot extends JavaPlugin {

    private static final Material MUSIC_MENU_MATERIAL = Material.NETHER_STAR;

    private static Bot instance;

    private MusicMenu musicMenu;
    private ActionBarDisplay actionBarDisplay;

    @Override
    public void onEnable() {
        instance = this;

        MusicConnection.connect("craplezz.de", 8087, "admin", "manuelsch", "887edd77-8871-40da-a3c1-2bee8729d877", (musicConnection) -> {
            musicMenu = new MusicMenu(musicConnection);
            actionBarDisplay = new ActionBarDisplay(musicConnection);
            actionBarDisplay.runTaskTimer(this, 0L, 20L);

            HttpConnection.getService().execute(() -> {
                do {
                    musicConnection.fetchInstanceStatus((instance) -> {
                        actionBarDisplay.update(instance.getCurrentTrack(), instance.getPosition());

                        musicMenu.broadcastTrackChange(instance.getCurrentTrack());
                    });

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (isEnabled());
            });
        });

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onPlayerInteract(PlayerInteractEvent event) {
                if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && event.getItem().getType() == MUSIC_MENU_MATERIAL)
                    musicMenu.open(event.getPlayer());
            }

        }, this);
    }

    public MusicMenu getMusicMenu() {
        return musicMenu;
    }

    public ActionBarDisplay getActionBarDisplay() {
        return actionBarDisplay;
    }

    public static Bot getInstance() {
        return instance;
    }

}
