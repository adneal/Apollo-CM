/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.ui.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.andrew.apollo.R;
import com.andrew.apollo.lyrics.LyricsProvider;
import com.andrew.apollo.lyrics.LyricsProviderFactory;
import com.andrew.apollo.lyrics.OfflineLyricsProvider;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

/**
 * This {@link SherlockFragment} is used to display lyrics for the currently
 * playing song.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressLint("NewApi")
public class LyricsFragment extends SherlockFragment {

    // Lyrics
    private TextView mLyrics;

    // Progess
    private ProgressBar mProgressBar;

    private boolean mTryOnline = false;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public LyricsFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.lyrics_base, null);
        // Initialize the lyrics text view
        mLyrics = (TextView)rootView.findViewById(R.id.audio_player_lyrics);
        // Enable text selection
        if (ApolloUtils.hasHoneycomb()) {
            mLyrics.setTextIsSelectable(true);
        }
        // Initialze the progess bar
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.audio_player_lyrics_progess);
        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    /**
     * Called to set the lyrics.
     */
    public void fetchLyrics(final boolean force) {
        if (isAdded()) {
            ApolloUtils.execute(false, new FetchLyrics(), force);
        }
    }

    /**
     * Save current lyrics in file metadata for offline use
     */
    private void saveLyrics(final String lyrics) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                final String path = MusicUtils.getFilePath();
                if (path != null) {
                    OfflineLyricsProvider.saveLyrics(lyrics, path);
                }
                return null;
            }
        }, (Void[])null);
    }

    /**
     * Used to fetch the lyrics for the currently playing song.
     */
    private final class FetchLyrics extends AsyncTask<Boolean, Void, String> {

        private final String mArtist;

        private final String mSong;

        /**
         * Constructor of <code>FetchLyrics</code>
         */
        public FetchLyrics() {
            mArtist = MusicUtils.getArtistName();
            mSong = MusicUtils.getTrackName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPreExecute() {
            mTryOnline = false;
            // Release the lyrics on track changes
            mLyrics.setText(null);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String doInBackground(final Boolean... force) {
            LyricsProvider provider = null;
            String lyrics = null;

            // First try offline, unless the user wants to fetch new lyrics
            if (!force[0]) {
                provider = LyricsProviderFactory.getOfflineProvider(MusicUtils.getFilePath());
                lyrics = provider.getLyrics(null, null);
            }

            // Now try to fetch for them
            if (lyrics == null && ApolloUtils.isOnline(getSherlockActivity())) {
                mTryOnline = true;
                provider = LyricsProviderFactory.getMainOnlineProvider();
                lyrics = provider.getLyrics(mArtist, mSong);
            }
            return lyrics;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final String result) {
            if (!TextUtils.isEmpty(result) && isAdded()) {
                // Set the lyrics
                mLyrics.setText(result);
                // Save the lyrics
                saveLyrics(result);
            } else {
                if (mTryOnline) {
                    mLyrics.setText(getString(R.string.no_lyrics, mSong));
                } else {
                    mLyrics.setText(getString(R.string.try_fetch_lyrics, mSong));
                }
            }
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
