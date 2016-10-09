package me.mani.clbot.music;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.mani.clapi.http.music.MusicConnection;
import me.mani.clapi.http.music.data.Track;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Overload
 * @version 1.0
 */
public class ActionBarDisplay extends BukkitRunnable {

    private static final ProtocolManager PROTOCOL_MANAGER = ProtocolLibrary.getProtocolManager();

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
            PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.CHAT);
            packetContainer.getBytes().write(0, (byte) 2);
            packetContainer.getChatComponents().write(0, WrappedChatComponent.fromText(broadcastMessage));
            try {
                PROTOCOL_MANAGER.sendServerPacket(player, packetContainer);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
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
