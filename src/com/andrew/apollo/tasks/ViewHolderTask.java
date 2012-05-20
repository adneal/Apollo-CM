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
import com.andrew.apollo.views.ViewHolderGrid;
import com.andrew.apollo.views.ViewHolderList;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 */
public class ViewHolderTask extends AsyncTask<String, Integer, Bitmap> implements Constants {

    private final ViewHolderList mViewHolderList;

    private final ViewHolderGrid mViewHolderGrid;

    private final WeakReference<ImageView> imageViewReference;

    private final Context mContext;

    private final int mPosition;

    private final int choice;

    private final int holderChoice;

    private final AQuery aquery;

    private final ImageView mImageView;

    private final int albumart;

    private final WeakReference<Context> contextReference;

    private String url;

    private WeakReference<Bitmap> bitmapReference;

    public ViewHolderTask(ViewHolderList vh, ViewHolderGrid vhg, int position, Context c, int opt,
            int holderOpt, ImageView iv) {
        mViewHolderList = vh;
        mViewHolderGrid = vhg;
        mPosition = position;
        contextReference = new WeakReference<Context>(c);
        mContext = contextReference.get();
        choice = opt;
        holderChoice = holderOpt;
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();
        aquery = new AQuery(mContext);

        albumart = mContext.getResources().getInteger(R.integer.listview_album_art);
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        if (choice == 0)
            url = ApolloUtils.getImageURL(args[0], ARTIST_IMAGE, mContext);
        if (choice == 1)
            url = ApolloUtils.getImageURL(args[0], ALBUM_IMAGE, mContext);
        bitmapReference = new WeakReference<Bitmap>(aquery.getCachedImage(url));
        if (holderChoice == 0) {
            return ApolloUtils.getResizedBitmap(bitmapReference.get(), albumart, albumart);
        } else if (holderChoice == 1) {
            return bitmapReference.get();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && imageViewReference != null && holderChoice == 0
                && mViewHolderList.position == mPosition && mViewHolderList != null)
            mImageView.setImageBitmap(result);
        if (result != null && imageViewReference != null && holderChoice == 1
                && mViewHolderGrid.position == mPosition && mViewHolderGrid != null)
            mImageView.setImageBitmap(result);
        super.onPostExecute(result);
    }
}
