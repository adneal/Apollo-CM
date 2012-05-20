
package com.andrew.apollo.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Constants;
import com.andrew.apollo.grid.fragments.QuickQueueFragment;
import com.andrew.apollo.tasks.LastfmGetAlbumImages;
import com.andrew.apollo.tasks.LastfmGetArtistImages;
import com.andrew.apollo.tasks.ViewHolderQueueTask;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.views.ViewHolderQueue;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 */
public class QuickQueueAdapter extends SimpleCursorAdapter implements Constants {

    private WeakReference<ViewHolderQueue> holderReference;

    public QuickQueueAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        // ViewHolderQueue
        final ViewHolderQueue viewholder;

        if (view != null) {

            viewholder = new ViewHolderQueue(view);
            holderReference = new WeakReference<ViewHolderQueue>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderQueue)convertView.getTag();
        }

        // AQuery
        final AQuery aq = new AQuery(view);

        // Artist Name
        String artistName = mCursor.getString(QuickQueueFragment.mArtistIndex);

        // Album name
        String albumName = mCursor.getString(QuickQueueFragment.mAlbumIndex);

        // Track name
        String trackName = mCursor.getString(QuickQueueFragment.mTitleIndex);
        holderReference.get().mTrackName.setText(trackName);

        holderReference.get().position = position;
        // Artist Image
        if (aq.shouldDelay(position, view, parent, "")) {
            holderReference.get().mArtistImage.setImageDrawable(null);
        } else {
            // Check for missing artist images and cache them
            if (ApolloUtils.getImageURL(artistName, ARTIST_IMAGE, mContext) == null) {
                new LastfmGetArtistImages(mContext).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName);
            } else {
                new ViewHolderQueueTask(holderReference.get(), position, mContext, 0, 0,
                        holderReference.get().mArtistImage).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName);
            }
        }

        // Album Image
        if (aq.shouldDelay(position, view, parent, "")) {
            holderReference.get().mAlbumArt.setImageDrawable(null);
        } else {
            // Check for missing album images and cache them
            if (ApolloUtils.getImageURL(albumName, ALBUM_IMAGE, mContext) == null) {
                new LastfmGetAlbumImages(mContext, null, 0).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName, albumName);
            } else {
                new ViewHolderQueueTask(holderReference.get(), position, mContext, 1, 1,
                        holderReference.get().mAlbumArt).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, albumName);
            }
        }
        return view;
    }
}
