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
import android.provider.MediaStore.Audio.ArtistColumns;

import com.andrew.apollo.Constants;
import com.andrew.apollo.utils.ApolloUtils;

/**
 * @author Andrew Neal
 * @returns A String[] of all the artists on a device in default artist order
 *          that are then fed into the Last.fm API
 */
public class FetchArtistImages extends AsyncTask<Void, Integer, String[]> implements Constants {

    private final WeakReference<Context> contextReference;

    private final int choice;

    public FetchArtistImages(Context context, int opt) {
        contextReference = new WeakReference<Context>(context);
        choice = opt;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        String[] projection = new String[] {
                BaseColumns._ID, ArtistColumns.ARTIST
        };
        String sortOrder = Audio.Artists.DEFAULT_SORT_ORDER;
        Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor c = contextReference.get().getContentResolver()
                .query(uri, projection, null, null, sortOrder);
        ArrayList<String> artistIds = new ArrayList<String>();
        if (c != null) {
            int count = c.getCount();
            if (count > 0) {
                final int ARTIST_IDX = c.getColumnIndex(ArtistColumns.ARTIST);
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
                if (ApolloUtils.getImageURL(result[i], ARTIST_IMAGE, contextReference.get()) == null) {
                    new LastfmGetArtistImages(contextReference.get()).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, result[i]);
                }
            } else if (choice == 1 && result != null) {
                // Unless the user wants to grab new images
                new LastfmGetArtistImages(contextReference.get()).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, result[i]);
            }
        }
        super.onPostExecute(result);
    }

    /**
     * Fetch artist images
     */
    public void runTask() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    }
}
