package me.mani.clbot.music;

import me.mani.clapi.http.music.MusicConnection;
import me.mani.clapi.http.music.data.Playlist;
import me.mani.clapi.http.music.data.Track;
import me.mani.clapi.various.AudioStreamConnection;
import me.mani.clapi.various.StreamDataParser;
import me.mani.clbot.Bot;
import me.mani.clcore.util.AnvilInventory;
import me.mani.clcore.util.ItemBuilders;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitTask;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Created by Schuckmann on 12.05.2016.
 */
public class MusicMenu {

    public static final String FAVORITES_PLAYLIST = "214e10dc-3ffb-49f0-84b7-7a5d0cfd5870";

    private static MusicMenu instance;

    private MusicConnection musicConnection;
    private AudioStreamConnection audioStreamConnection;

    private List<MusicSubMenu> menus;
    private PlaylistSelectMenu playlistSelectMenu;
    private SearchMenu searchMenu;

    private List<Track> currentTracklist;
    private Playlist currentPlaylist;

    private Runnable cancelRadioListener;

    public MusicMenu(MusicConnection musicConnection) {
        instance = this;
        this.musicConnection = musicConnection;
        setup();
    }

    private void setup() {
        playlistSelectMenu = new PlaylistSelectMenu();
        searchMenu = new SearchMenu();

        remake(null, () -> {});
    }

    /**
     * Redraws the inventories.
     *
     * @param playlist If this is null, all tracks are displayed.
     */
    public void remake(Playlist playlist, Runnable callback) {
        menus = new ArrayList<>();
        currentPlaylist = playlist != null && playlist.getName().equals("temp") ? null : playlist;
        if (playlist == null) {
            musicConnection.fetchTrackList((trackList) -> {
                makeMenus(trackList);
                callback.run();
            });
        }
        else {
            if (!playlist.isSynced())
                musicConnection.fetchPlaylistTrackList(playlist, (tracks) -> {
                    playlist.setTracks(tracks);
                    makeMenus(playlist.getTracks());
                    callback.run();
                });
            else {
                makeMenus(playlist.getTracks());
                callback.run();
            }
        }
    }

    private void makeMenus(List<Track> tracks) {
        currentTracklist = tracks;
        int pageIndex = 0;
        int index = 0;
        MusicSubMenu menu = new MusicSubMenu(0);
        for (Track track : tracks) {
            if (index == 45) {
                pageIndex++;
                index = 0;
                menus.add(menu);
                menu = new MusicSubMenu(pageIndex);
            }
            menu.addMusicTrack(index++, track);
        }
        // Adding menu with remaining tracks
        menus.add(menu);
    }

    public void open(Player player) {
        openPage(player, 0);
    }

    private void openPage(Player player, int pageIndex) {
        if (pageIndex > menus.size() - 1)
            openPage(player, 0);
        else if (pageIndex < 0)
            openPage(player, menus.size() - 1);
        else
            menus.get(pageIndex).open(player);
    }

    public void broadcastTrackChange(Track newTrack) {
        if (cancelRadioListener != null)
            cancelRadioListener.run();
        if (newTrack.getType().equals(Track.TYPE_URL)) {
            Bukkit.broadcastMessage("§7Now Playing Radio: §b" + newTrack.getTitle());
            if (SupportedRadioStream.isSupported(newTrack.getTitle())) {
                SupportedRadioStream supportedRadioStream = SupportedRadioStream.getSupportedRadioStream(newTrack.getTitle());
                if (supportedRadioStream != null) {
                    audioStreamConnection = new AudioStreamConnection(supportedRadioStream.getUrl());
                    audioStreamConnection.listenForMetadata(supportedRadioStream.getStream(), (metadata) -> {
                        try {
                            String[] information = supportedRadioStream.formatMetadata(new StreamDataParser(metadata).getString("StreamTitle"));
                            Bukkit.broadcastMessage("§7Now Playing: §e" + information[1] + " §8by §7" + information[0]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                            cancelRadioListener.run();
                        }
                    }, (runnable) -> cancelRadioListener = runnable);
                }
            }
        }
        else
            Bukkit.broadcastMessage("§7Now Playing: §e" + newTrack.getTitle() + " §8by §7" + newTrack.getArtist());
    }

    private void getMaterial(Track track, BiConsumer<Material, ChatColor> consumer) {
        if (track.getType().equals(Track.TYPE_URL))
            consumer.accept(Material.RECORD_12, ChatColor.AQUA);
        else {
            musicConnection.fetchPlaylistTrackList(FAVORITES_PLAYLIST, (tracks) -> {
                if (tracks.contains(track))
                    consumer.accept(Material.GOLD_RECORD, ChatColor.YELLOW);
                else if (track.getDuration() > 900000) // If the track is longer than 15 minutes
                    consumer.accept(Material.RECORD_3, ChatColor.RED);
                else
                    consumer.accept(Material.RECORD_9, ChatColor.WHITE);
            });
        }
    }

    public MusicConnection getMusicConnection() {
        return musicConnection;
    }

    public class MusicSubMenu extends Menu {

        public MusicSubMenu(int pageIndex) {
            inventory = Bukkit.createInventory(null, 54, "Seite " + (pageIndex + 1));

            // Previous page item
            inventory.setItem(45, ItemBuilders.normal(Material.ARROW).name("§7Previous page").build());
            clickListeners[45] = (clickEvent) -> openPage((Player) clickEvent.getWhoClicked(), pageIndex - 1);

            // Playlist select item
            inventory.setItem(47, ItemBuilders.normal(Material.PAPER).name("§7Select playlist").build());
            clickListeners[47] = (clickEvent) -> playlistSelectMenu.open((Player) clickEvent.getWhoClicked());

            // Refresh item
            inventory.setItem(48, ItemBuilders.normal(Material.FIREWORK).name("§7Refresh").build());
            clickListeners[48] = (clickEvent) -> {
                musicConnection.clearCaches();
                remake(null, () -> {
                    clickEvent.getWhoClicked().closeInventory();
                    instance.open((Player) clickEvent.getWhoClicked());
                });
            };

            // Volume control item
            musicConnection.fetchInstanceStatus((instance) ->
                    inventory.setItem(49, ItemBuilders.normal(Material.STICK).name("§7Volume control (" + instance.getVolume() + " %)").build()));
            clickListeners[49] = (clickEvent) -> {
                musicConnection.fetchInstanceStatus((instance) -> {
                    if (instance != null) {
                        int volume = instance.getVolume();
                        if (clickEvent.isLeftClick())
                            volume += 1;
                        else if (clickEvent.isRightClick())
                            volume -= 1;
                        if (volume >= 0 && volume <= 100) {
                            musicConnection.pushSetVolume(volume);
                            inventory.setItem(49, ItemBuilders.normal(Material.STICK).name("§7Volume control (" + volume + " %)").build());
                        }
                    }
                });
            };

            // Search item
            inventory.setItem(51, ItemBuilders.normal(Material.GLASS).name("§7Search tracks").build());
//            clickListeners[51] = (clickEvent) -> searchMenu.open((Player) clickEvent.getWhoClicked());
            clickListeners[51] = (clickEvent) -> {
                AnvilInventory anvilInventory = new AnvilInventory((Player) clickEvent.getWhoClicked(), (anvilClickEvent) -> {
                    String searchQuery = anvilClickEvent.getName();
                    List<Track> foundTracks = new ArrayList<>();
                    for (Track track : currentTracklist)
                        if (StringUtils.containsIgnoreCase(track.getTitle(), searchQuery) || StringUtils.containsIgnoreCase(track.getArtist(), searchQuery))
                            foundTracks.add(track);
                    remake(Playlist.tempPlaylist(foundTracks), () -> {
                        clickEvent.getWhoClicked().closeInventory();
                        instance.open((Player) clickEvent.getWhoClicked());
                    });
                });
                anvilInventory.setSlot(AnvilInventory.AnvilSlot.INPUT_LEFT, ItemBuilders.normal(Material.STAINED_GLASS_PANE).name("Search query...").build());
                anvilInventory.open();
            };

            // Next page item
            inventory.setItem(53, ItemBuilders.normal(Material.ARROW).name("§7Next page").build());
            clickListeners[53] = (clickEvent) -> openPage((Player) clickEvent.getWhoClicked(), pageIndex + 1);
        }

        public void addMusicTrack(int index, Track track) {
            getMaterial(track, (material, color) ->
                    inventory.setItem(index, ItemBuilders.normal(material).name(color + track.getTitle()).lore(Arrays.asList("§8by §7" + track.getArtist())).hide(ItemFlag.values()).build())
            );
            clickListeners[index] = (clickEvent) -> {
                if (currentPlaylist == null) {
                    musicConnection.pushTrackPlay(track.getUuid());
                    Bot.getInstance().getActionBarDisplay().update(track, 0);
                }
                else
                    musicConnection.pushPlaylistTrackPlay(currentPlaylist.getUuid(), index);
                broadcastTrackChange(track);
            };
        }

    }

    /**
     * Not supporting multi paging here, because there will never be more than 54 playlists...
     */
    public class PlaylistSelectMenu extends Menu {

        public PlaylistSelectMenu() {
            inventory = Bukkit.createInventory(null, 54, "Select a playlist");

            musicConnection.fetchPlaylistList((playlists) -> {
                int index = 0;
                for (Playlist playlist : playlists) {
                    inventory.addItem(ItemBuilders.normal(Material.PAPER).name("§e" + playlist.getName()).build());
                    clickListeners[index++] = (clickEvent) -> {
                        if (clickEvent.isLeftClick()) {
                            remake(playlist, () -> {
                                clickEvent.getWhoClicked().closeInventory();
                                instance.open((Player) clickEvent.getWhoClicked());
                            });
                        }
                        else if (clickEvent.isRightClick()) {
                            musicConnection.pushPlaylistTrackPlay(playlist.getUuid(), 0);
                            musicConnection.fetchTrack(playlist.getEntries().get(0), MusicMenu.this::broadcastTrackChange);
                        }
                    };
                }
            });

            inventory.setItem(53, ItemBuilders.normal(Material.MAP).name("§eAll tracks").build());
            clickListeners[53] = (clickEvent) -> {
                remake(null, () -> {
                    clickEvent.getWhoClicked().closeInventory();
                    instance.open((Player) clickEvent.getWhoClicked());
                });
            };
        }

    }

    public class SearchMenu extends Menu {

        private BukkitTask task;

        public SearchMenu() {
            inventory = Bukkit.createInventory(null, InventoryType.ANVIL, "Search for a track");

            inventory.setItem(0, ItemBuilders.normal(Material.STAINED_GLASS_PANE).name(" ").build());
        }

        @Override
        public void open(Player player) {
            super.open(player);
            task = Bukkit.getScheduler().runTaskTimer(Bot.getInstance(), () -> {
                if (inventory.getItem(2) != null) {
                    String searchQuery = inventory.getItem(2).getItemMeta().getDisplayName();
                    for (Track track : currentTracklist)
                        if (StringUtils.startsWithIgnoreCase(track.getTitle(), searchQuery)) {
                            getMaterial(track, (material, color) ->
                                    inventory.setItem(1, ItemBuilders.normal(material).name(color + track.getTitle()).lore(Arrays.asList("§8by §7" + track.getArtist())).hide(ItemFlag.values()).build())
                            );
                            break;
                        }
                }
            }, 0L, 2L);
        }

        @Override
        public void close() {
            task.cancel();
        }

    }

}
