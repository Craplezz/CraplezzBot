package me.mani.clbot.music;

/**
 * Created by Schuckmann on 20.05.2016.
 */
public enum SupportedRadioStream {
    I_LOVE_RADIO ("iLoveRadio", "http://stream01.iloveradio.de/", "iloveradio1.mp3", " - "),
    I_LOVE_2_DANCE ("iLove2Dance", "http://stream01.iloveradio.de/", "iloveradio2.mp3", " - "),
    TOP_100_STATION ("top100station", "http://87.230.103.85:80", "", " - ");

    private String name;
    private String url;
    private String stream;
    private String splitter;

    private SupportedRadioStream(String name, String url, String stream, String splitter) {
        this.name = name;
        this.url = url;
        this.stream = stream;
        this.splitter = splitter;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getStream() {
        return stream;
    }

    public String[] formatMetadata(String metadata) {
        String[] information = metadata.split(splitter);
        return information;
    }

    public static boolean isSupported(String name) {
        for (SupportedRadioStream supportedRadioStream : values())
            if (supportedRadioStream.name.equals(name))
                return true;
        return false;
    }

    public static SupportedRadioStream getSupportedRadioStream(String name) {
        for (SupportedRadioStream supportedRadioStream : values())
            if (supportedRadioStream.name.equals(name))
                return supportedRadioStream;
        return null;
    }

}
