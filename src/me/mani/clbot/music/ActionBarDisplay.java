package me.mani.clbot.music;

import me.mani.clapi.http.music.MusicConnection;
import me.mani.clapi.http.music.data.Track;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Overload
 * @version 1.0
 */
public class ActionBarDisplay extends BukkitRunnable {

    private MusicConnection musicConnection;

    private String broadcastMessage;
    private Track nowPlaying;
    private int currentTime;

    public ActionBarDisplay(MusicConnection musicConnection) {
        this.musicConnection = musicConnection;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(broadcastMessage).create());
        }

        update(nowPlaying, currentTime + 1000);
    }

    public void update(Track nowPlaying, int currentTime) {
        this.nowPlaying = nowPlaying;
        this.currentTime = currentTime;
        if (nowPlaying == null) {
            broadcastMessage = null;
        }
        else {
            broadcastMessage = "\u266a " + nowPlaying.getArtist() + " - " + nowPlaying.getTitle() + " \u266a";
            StringBuilder sb = new StringBuilder(broadcastMessage);
            try {
                sb.insert((int) (((double) currentTime / (double) nowPlaying.getDuration()) * broadcastMessage.length()), "§7");
            }
            catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            broadcastMessage = "§e" + sb.toString();
        }
    }

}
