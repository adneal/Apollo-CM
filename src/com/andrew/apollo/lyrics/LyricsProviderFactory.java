
package com.andrew.apollo.lyrics;

public final class LyricsProviderFactory {

    /* This class is never initiated. */
    public LyricsProviderFactory() {
    }

    /**
     * @param filePath The path to save the lyrics.
     * @return A new instance of {@link OfflineLyricsProvider}.
     */
    public static final LyricsProvider getOfflineProvider(String filePath) {
        return new OfflineLyricsProvider(filePath);
    }

    /**
     * @return The current lyrics provider.
     */
    public static final LyricsProvider getMainOnlineProvider() {
        return new LyricsWikiProvider();
    }

    // TODO Implement more providers, and also a system to iterate over them

}
