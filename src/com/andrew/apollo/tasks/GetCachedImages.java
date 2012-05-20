/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.andrew.apollo.Constants;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal Returns a cached image for @TracksBrowser
 */
public class GetCachedImages extends AsyncTask<String, Integer, Bitmap> implements Constants {

    private final Context mContext;

    private final int choice;

    private final WeakReference<ImageView> imageViewReference;

    private final AQuery aquery;

    private final ImageView mImageView;

    private String url;

    private WeakReference<Bitmap> bitmapReference;

    private final WeakReference<Context> contextReference;

    public GetCachedImages(Context c, int opt, ImageView iv) {
        contextReference = new WeakReference<Context>(c);
        mContext = contextReference.get();
        choice = opt;
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();

        // AQuery
        aquery = new AQuery(mContext);
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        if (choice == 0)
            url = ApolloUtils.getImageURL(args[0], ARTIST_IMAGE_ORIGINAL, mContext);
        if (choice == 1)
            url = ApolloUtils.getImageURL(args[0], ALBUM_IMAGE, mContext);
        bitmapReference = new WeakReference<Bitmap>(aquery.getCachedImage(url, 300));
        return bitmapReference.get();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (imageViewReference != null && result != null) {
            ApolloUtils.runnableBackground(mImageView, result);
        } else {
            result = aquery.getCachedImage(R.drawable.promo);
            ApolloUtils.runnableBackground(mImageView, result);
        }
        super.onPostExecute(result);
    }
}
