/**
 * 
 */

package com.andrew.apollo.tasks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AlbumColumns;

import com.andrew.apollo.Constants;
import com.andrew.apollo.utils.ApolloUtils;

/**
 * @author Andrew Neal
 * @returns A String[] of all the artists and albums on a device in default
 *          album order that are then fed into the Last.fm API
 */
public class FetchAlbumImages {

    private final Context mContext;

    private final WeakReference<Context> contextReference;

    private final int choice;

    public FetchAlbumImages(Context context, int opt) {
        contextReference = new WeakReference<Context>(context);
        mContext = contextReference.get();
        choice = opt;
    }

    /**
     * @return album names in default album sort order
     */
    public String[] getAlbumArtists() {
        String[] projection = new String[] {
                BaseColumns._ID, AlbumColumns.ARTIST
        };
        String sortOrder = Audio.Albums.DEFAULT_SORT_ORDER;
        Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor c = mContext.getContentResolver().query(uri, projection, null, null, sortOrder);
        ArrayList<String> artistIds = new ArrayList<String>();
        if (c != null) {
            int count = c.getCount();
            if (count > 0) {
                final int ARTIST_IDX = c.getColumnIndex(AlbumColumns.ARTIST);
                for (int i = 0; i < count; i++) {
                    c.moveToPosition(i);
                    artistIds.add(c.getString(ARTIST_IDX));
                }
            }
            c.close();
            c = null;
        }
        return artistIds.toArray(new String[artistIds.size()]);
    }

    /**
     * @author Andrew Neal
     * @returns artist names in default album sort order that are then fed into
     *          the Last.fm API along with @getAlbumArtists()
     */
    public class getAlbums extends AsyncTask<Void, Integer, String[]> implements Constants {

        @Override
        protected String[] doInBackground(Void... params) {
            String[] projection = new String[] {
                    BaseColumns._ID, AlbumColumns.ALBUM
            };
            String sortOrder = Audio.Albums.DEFAULT_SORT_ORDER;
            Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
            Cursor c = mContext.getContentResolver().query(uri, projection, null, null, sortOrder);
            ArrayList<String> artistIds = new ArrayList<String>();
            if (c != null) {
                int count = c.getCount();
                if (count > 0) {
                    final int ARTIST_IDX = c.getColumnIndex(AlbumColumns.ALBUM);
                    for (int i = 0; i < count; i++) {
                        c.moveToPosition(i);
                        artistIds.add(c.getString(ARTIST_IDX));
                    }
                }
                c.close();
                c = null;
            }
            return artistIds.toArray(new String[artistIds.size()]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            for (int i = 0; i < result.length; i++) {
                // Only download images we don't already have
                if (choice == 0 && result != null) {
                    if (ApolloUtils.getImageURL(result[i], ALBUM_IMAGE, mContext) == null) {
                        new LastfmGetAlbumImages(mContext, null, 0).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, getAlbumArtists()[i], result[i]);
                    }
                } else if (choice == 1 && result != null) {
                    // Unless the user wants to grab new images
                    new LastfmGetAlbumImages(mContext, null, 0).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, getAlbumArtists()[i], result[i]);
                }
            }
            super.onPostExecute(result);
        }
    }

    /**
     * Fetch album art
     */
    public void runTask() {
        new getAlbums().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    }
}
