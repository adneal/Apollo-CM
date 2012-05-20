
package com.andrew.apollo.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Constants;
import com.andrew.apollo.R;
import com.andrew.apollo.list.fragments.RecentlyAddedFragment;
import com.andrew.apollo.tasks.LastfmGetAlbumImages;
import com.andrew.apollo.tasks.ViewHolderTask;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.views.ViewHolderList;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal
 */
public class RecentlyAddedAdapter extends SimpleCursorAdapter implements Constants {

    private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;

    private WeakReference<ViewHolderList> holderReference;

    public RecentlyAddedAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        // ViewHolderList
        ViewHolderList viewholder;

        if (view != null) {

            viewholder = new ViewHolderList(view);
            holderReference = new WeakReference<ViewHolderList>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderList)convertView.getTag();
        }
        // AQuery
        final AQuery aq = new AQuery(convertView);

        // Track name
        String trackName = mCursor.getString(RecentlyAddedFragment.mTitleIndex);
        holderReference.get().mViewHolderLineOne.setText(trackName);

        // Artist name
        String artistName = mCursor.getString(RecentlyAddedFragment.mArtistIndex);
        holderReference.get().mViewHolderLineTwo.setText(artistName);

        // Album name
        String albumName = mCursor.getString(RecentlyAddedFragment.mAlbumIndex);

        // Match positions
        holderReference.get().position = position;
        if (aq.shouldDelay(position, view, parent, "")) {
            holderReference.get().mViewHolderImage.setImageDrawable(null);
        } else {
            // Check for missing artwork and cache then cache it
            if (ApolloUtils.getImageURL(albumName, ALBUM_IMAGE, mContext) == null) {
                new LastfmGetAlbumImages(mContext, null, 0).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, artistName, albumName);
            } else {
                new ViewHolderTask(holderReference.get(), null, position, mContext, 1, 0,
                        holderReference.get().mViewHolderImage).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, albumName);
            }
        }

        holderReference.get().mQuickContext.setVisibility(View.GONE);

        // Now playing indicator
        long currentaudioid = MusicUtils.getCurrentAudioId();
        long audioid = mCursor.getLong(RecentlyAddedFragment.mMediaIdIndex);
        if (currentaudioid == audioid) {
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
