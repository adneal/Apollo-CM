
package com.andrew.apollo.lyrics;

public interface LyricsProvider {

    /**
     * Gives the lyrics of the song, or null if they werent found
     * 
     * @param artist Artist name
     * @param song Song name
     * @return Full lyrics as a {@link String}
     */
    public String getLyrics(String artist, String song);

    /**
     * Gives the name of the provider implementation
     * 
     * @return The name of the lyrics provider
     */
    public String getProviderName();

}
