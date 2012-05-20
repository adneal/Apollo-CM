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
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.views.ViewHolderQueue;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 */
public class ViewHolderQueueTask extends AsyncTask<String, Integer, Bitmap> implements Constants {

    private final ViewHolderQueue mViewHolderQueue;

    private final WeakReference<ImageView> imageViewReference;

    private final Context mContext;

    private final int mPosition;

    private final int choice;

    private final int holderChoice;

    private final AQuery aquery;

    private final ImageView mImageView;

    private String url;

    private WeakReference<Bitmap> bitmapReference;

    private final WeakReference<Context> contextReference;

    public ViewHolderQueueTask(ViewHolderQueue vh, int position, Context c, int opt, int holderOpt,
            ImageView iv) {
        mViewHolderQueue = vh;
        mPosition = position;
        contextReference = new WeakReference<Context>(c);
        mContext = contextReference.get();
        choice = opt;
        holderChoice = holderOpt;
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();

        // AQuery
        aquery = new AQuery(mContext);
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        if (choice == 0)
            url = ApolloUtils.getImageURL(args[0], ARTIST_IMAGE, mContext);
        if (choice == 1)
            url = ApolloUtils.getImageURL(args[0], ALBUM_IMAGE, mContext);
        bitmapReference = new WeakReference<Bitmap>(aquery.getCachedImage(url));
        return bitmapReference.get();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (imageViewReference != null && holderChoice == 0
                && mViewHolderQueue.position == mPosition && mViewHolderQueue != null)
            aquery.id(mImageView).image(result);
        if (imageViewReference != null && holderChoice == 1
                && mViewHolderQueue.position == mPosition && mViewHolderQueue != null)
            aquery.id(mImageView).image(result);
        super.onPostExecute(result);
    }
}
