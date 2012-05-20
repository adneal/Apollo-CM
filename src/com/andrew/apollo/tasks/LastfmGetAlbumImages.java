/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.andrew.apollo.Constants;
import com.andrew.apollo.lastfm.api.Album;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.utils.ApolloUtils;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 * @returns A convenient image size that's perfect for a GridView.
 */
public class LastfmGetAlbumImages extends AsyncTask<String, Integer, String> implements Constants {

    // URL to cache
    private String url = null;

    // AQuery
    private final AQuery aq;

    private final WeakReference<Context> contextReference;

    private final WeakReference<ImageView> imageviewReference;

    private final ImageView mImageView;

    private final int choice;

    private Album album;

    public LastfmGetAlbumImages(Context context, ImageView iv, int opt) {
        contextReference = new WeakReference<Context>(context);
        imageviewReference = new WeakReference<ImageView>(iv);
        mImageView = imageviewReference.get();
        choice = opt;

        // Initiate AQuery
        aq = new AQuery((Activity)contextReference.get(), iv);
    }

    @Override
    protected String doInBackground(String... name) {
        if (ApolloUtils.isOnline(contextReference.get()) && name[0] != null && name[1] != null) {
            try {
                album = Album.getInfo(name[0], name[1], LASTFM_API_KEY);
                url = album.getImageURL(ImageSize.LARGE);
                aq.cache(url, 0);
                ApolloUtils.setImageURL(name[1], url, ALBUM_IMAGE, contextReference.get());
                return url;
            } catch (Exception e) {
                return null;
            }
        } else {
            url = ApolloUtils.getImageURL(name[1], ALBUM_IMAGE, contextReference.get());
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null && mImageView != null && choice == 1)
            new BitmapFromURL(mImageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result);
        super.onPostExecute(result);
    }
}
