/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.andrew.apollo.Constants;
import com.andrew.apollo.lastfm.api.Artist;
import com.andrew.apollo.lastfm.api.Image;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.lastfm.api.PaginatedResult;
import com.andrew.apollo.utils.ApolloUtils;
import com.androidquery.AQuery;


/**
 * @author Andrew Neal
 * @Note This is used to display artist images in @TracksBrowser
 */
public class LastfmGetArtistImagesOriginal extends AsyncTask<String, Integer, String> implements
        Constants {

    // URL to cache
    private String url = null;

    private final ImageView mImageView;

    private final WeakReference<ImageView> imageviewReference;

    // AQuery
    private final AQuery aq;

    // Context
    private final Context mContext;

    private final WeakReference<Context> contextReference;

    public LastfmGetArtistImagesOriginal(Context context, ImageView iv) {
        contextReference = new WeakReference<Context>(context);
        mContext = contextReference.get();
        imageviewReference = new WeakReference<ImageView>(iv);
        mImageView = imageviewReference.get();

        // Initiate AQuery
        aq = new AQuery(mContext);
    }

    @Override
    protected String doInBackground(String... artistname) {
        if (ApolloUtils.isOnline(mContext)) {
            PaginatedResult<Image> artist = Artist.getImages(artistname[0], 1, 1, LASTFM_API_KEY);
            Iterator<Image> iterator = artist.getPageResults().iterator();
            while (iterator.hasNext()) {
                Image mTemp = iterator.next();
                url = mTemp.getImageURL(ImageSize.ORIGINAL);
            }
            aq.cache(url, 0);
            ApolloUtils.setImageURL(artistname[0], url, ARTIST_IMAGE_ORIGINAL, mContext);
            return url;
        } else {
            url = ApolloUtils.getImageURL(artistname[0], ARTIST_IMAGE_ORIGINAL, mContext);
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null && mImageView != null) {
            new BitmapFromURL(mImageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result);
        }
        super.onPostExecute(result);
    }
}
