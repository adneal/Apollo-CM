
package com.andrew.apollo.lyrics;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LyricsWikiProvider implements LyricsProvider {

    // URL used to fetch the lyrics
    private static final String LYRICS_URL = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";

    // Currently, the only lyrics provider
    public static final String PROVIDER_NAME = "LyricsWiki";

    // Timeout duration
    private static final int DEFAULT_HTTP_TIME = 15 * 1000;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLyrics(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String ret = null;
        artist = artist.replace(" ", "%20");
        song = song.replace(" ", "%20");
        try {
            // Get the lyrics URL
            URL url = new URL(String.format(LYRICS_URL, artist, song));
            final String urlString = getUrlAsString(url);
            final String songURL = new JSONObject(urlString.replace("song = ", ""))
                    .getString("url");
            if (songURL.endsWith("action=edit")) {
                return null;
            }

            // And now get the full lyrics
            url = new URL(songURL);
            String html = getUrlAsString(url);
            // TODO Clean this up
            html = html.substring(html.indexOf("<div class='lyricbox'>"));
            html = html.substring(html.indexOf("</div>") + 6);
            html = html.substring(0, html.indexOf("<!--"));
            // Replace new line html with characters
            html = html.replace("<br />", "\n;");
            // Now parse the html entities
            final String[] htmlChars = html.split(";");
            final StringBuilder builder = new StringBuilder();
            String code = null;
            char caracter;
            for (final String s : htmlChars) {
                if (s.equals("\n")) {
                    builder.append(s);
                } else {
                    code = s.replaceAll("&#", "");
                    caracter = (char)Integer.valueOf(code).intValue();
                    builder.append(caracter);
                }
            }
            // And that's it
            ret = builder.toString();
        } catch (final MalformedURLException e) {
            Log.e("Apollo", "Lyrics not found in " + getProviderName(), e);
        } catch (final IOException e) {
            Log.e("Apollo", "Lyrics not found in " + getProviderName(), e);
        } catch (final JSONException e) {
            Log.e("Apollo", "Lyrics not found in " + getProviderName(), e);
        } catch (final NumberFormatException e) {
            Log.e("Apollo", "Lyrics not found in " + getProviderName(), e);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * @param url The {@link URL} to fecth the lyrics from
     * @return The {@link URL} used to fetch the lyrics as a {@link String}
     * @throws IOException
     */
    public static final String getUrlAsString(final URL url) throws IOException {
        // Perform a GET request for the lyrics
        final HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setReadTimeout(DEFAULT_HTTP_TIME);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.connect();
        final InputStreamReader input = new InputStreamReader(httpURLConnection.getInputStream());
        // Read the server output
        final BufferedReader reader = new BufferedReader(input);
        // Build the URL
        final StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line + "\n");
        }
        return builder.toString();
    }
}
