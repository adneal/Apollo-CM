
package com.andrew.apollo.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Constants;
import com.andrew.apollo.R;
import com.andrew.apollo.grid.fragments.ArtistsFragment;
import com.andrew.apollo.tasks.LastfmGetArtistImages;
import com.andrew.apollo.tasks.ViewHolderTask;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.views.ViewHolderGrid;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 */
public class ArtistAdapter extends SimpleCursorAdapter implements Constants {

    private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;

    private WeakReference<ViewHolderGrid> holderReference;

    public ArtistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        // ViewHolderGrid
        final ViewHolderGrid viewholder;

        if (view != null) {

            viewholder = new ViewHolderGrid(view);
            holderReference = new WeakReference<ViewHolderGrid>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderGrid)convertView.getTag();
        }

        // AQuery
        final AQuery aq = new AQuery(view);

        // Artist Name
        String artistName = mCursor.getString(ArtistsFragment.mArtistNameIndex);
        holderReference.get().mViewHolderLineOne.setText(artistName);

        // Number of albums
        int albums_plural = mCursor.getInt(ArtistsFragment.mArtistNumAlbumsIndex);
        boolean unknown = artistName == null || artistName.equals(MediaStore.UNKNOWN_STRING);
        String numAlbums = MusicUtils.makeAlbumsLabel(mContext, albums_plural, 0, unknown);
        holderReference.get().mViewHolderLineTwo.setText(numAlbums);

        holderReference.get().position = position;
        if (aq.shouldDelay(position, view, parent, "")) {
            holderReference.get().mViewHolderImage.setImageDrawable(null);
        } else {
            // Check for missing artist images and cache them
            if (ApolloUtils.getImageURL(artistName, ARTIST_IMAGE, mContext) == null) {
                new LastfmGetArtistImages(mContext).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName);
            } else {
                new ViewHolderTask(null, holderReference.get(), position, mContext, 0, 1,
                        holderReference.get().mViewHolderImage).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName);
            }
        }
        // Now playing indicator
        long currentartistid = MusicUtils.getCurrentArtistId();
        long artistid = mCursor.getLong(ArtistsFragment.mArtistIdIndex);
        if (currentartistid == artistid) {
            holderReference.get().mPeakOne.setImageResource(R.anim.peak_meter_1);
            holderReference.get().mPeakTwo.setImageResource(R.anim.peak_meter_2);
            mPeakOneAnimation = (AnimationDrawable)holderReference.get().mPeakOne.getDrawable();
            mPeakTwoAnimation = (AnimationDrawable)holderReference.get().mPeakTwo.getDrawable();
            try {
                if (MusicUtils.mService.isPlaying()) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                } else {
                    mPeakOneAnimation.stop();
                    mPeakTwoAnimation.stop();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            holderReference.get().mPeakOne.setImageResource(0);
            holderReference.get().mPeakTwo.setImageResource(0);
        }
        return view;
    }
}
