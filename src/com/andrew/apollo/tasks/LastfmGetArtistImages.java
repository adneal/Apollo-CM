/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import android.content.Context;
import android.os.AsyncTask;

import com.andrew.apollo.Constants;
import com.andrew.apollo.lastfm.api.Artist;
import com.andrew.apollo.lastfm.api.Image;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.lastfm.api.PaginatedResult;
import com.andrew.apollo.utils.ApolloUtils;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 * @returns A convenient image size that's perfect for a GridView.
 */
public class LastfmGetArtistImages extends AsyncTask<String, Integer, String> implements Constants {

    // URL to cache
    private String url = null;

    private PaginatedResult<Image> artist;

    // AQuery
    private final AQuery aq;

    private final WeakReference<Context> contextReference;

    public LastfmGetArtistImages(Context context) {
        contextReference = new WeakReference<Context>(context);

        // Initiate AQuery
        aq = new AQuery(contextReference.get());
    }

    @Override
    protected String doInBackground(String... artistname) {
        if (ApolloUtils.isOnline(contextReference.get()) && artistname[0] != null) {
            try {
                artist = Artist.getImages(artistname[0], 1, 1, LASTFM_API_KEY);
                Iterator<Image> iterator = artist.getPageResults().iterator();
                while (iterator.hasNext()) {
                    Image mTemp = iterator.next();
                    url = mTemp.getImageURL(ImageSize.LARGESQUARE);
                }
                aq.cache(url, 0);
                ApolloUtils.setImageURL(artistname[0], url, ARTIST_IMAGE, contextReference.get());
                return url;
            } catch (Exception e) {
                return null;
            }
        } else {
            url = ApolloUtils.getImageURL(artistname[0], ARTIST_IMAGE, contextReference.get());
        }
        return url;
    }
}
