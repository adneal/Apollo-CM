/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.andrew.apollo.utils.ApolloUtils;

/**
 * @author Andrew Neal
 */
public class BitmapFromURL extends AsyncTask<String, Integer, Bitmap> {

    private final WeakReference<ImageView> imageViewReference;

    private final ImageView mImageView;

    private WeakReference<Bitmap> bitmapReference;

    public BitmapFromURL(ImageView iv) {
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        bitmapReference = new WeakReference<Bitmap>(ApolloUtils.getBitmapFromURL(params[0]));
        return bitmapReference.get();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && mImageView != null)
            ApolloUtils.runnableBackground(mImageView, result);
        super.onPostExecute(result);
    }
}
