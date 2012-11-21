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

package com.andrew.apollo.ui.activities;

import static com.andrew.apollo.utils.MusicUtils.mService;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.ui.fragments.LyricsFragment;
import com.andrew.apollo.ui.fragments.QueueFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.PlayPauseButton;
import com.andrew.apollo.widgets.RepeatButton;
import com.andrew.apollo.widgets.RepeatingImageButton;
import com.andrew.apollo.widgets.ShuffleButton;
import com.nineoldandroids.animation.ObjectAnimator;

import java.lang.ref.WeakReference;

/**
 * Apollo's "now playing" interface.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AudioPlayerActivity extends SherlockFragmentActivity implements ServiceConnection,
        OnSeekBarChangeListener {

    // Message to refresh the time
    private static final int REFRESH_TIME = 1;

    // The service token
    private ServiceToken mToken;

    // Play and pause button
    private PlayPauseButton mPlayPauseButton;

    // Repeat button
    private RepeatButton mRepeatButton;

    // Shuffle button
    private ShuffleButton mShuffleButton;

    // Previous button
    private RepeatingImageButton mPreviousButton;

    // Next button
    private RepeatingImageButton mNextButton;

    // Track name
    private TextView mTrackName;

    // Artist name
    private TextView mArtistName;

    // Album art
    private ImageView mAlbumArt;

    // Tiny artwork
    private ImageView mAlbumArtSmall;

    // Current time
    private TextView mCurrentTime;

    // Total time
    private TextView mTotalTime;

    // Queue switch
    private ImageView mQueueSwitch;

    // Progess
    private SeekBar mProgress;

    // Broadcast receiver
    private PlaybackStatus mPlaybackStatus;

    // Handler used to update the current time
    private TimeHandler mTimeHandler;

    // View pager
    private ViewPager mViewPager;

    // Pager adpater
    private PagerAdapter mPagerAdapter;

    // ViewPager container
    private FrameLayout mPageContainer;

    // Header
    private LinearLayout mAudioPlayerHeader;

    // Image cache
    private ImageFetcher mImageFetcher;

    // Theme resources
    private ThemeUtils mResources;

    private long mPosOverride = -1;

    private long mStartSeekPos = 0;

    private long mLastSeekEventTime;

    private boolean mIsPaused = false;

    private boolean mFromTouch = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Title bar shows up in gingerbread, I'm too tired to figure out why.
        if (!ApolloUtils.hasHoneycomb()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        // Initialze the theme resources
        mResources = new ThemeUtils(this);
        // Set the overflow style
        mResources.setOverflowStyle(this);

        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);

        // Initialize the image fetcher/cache
        mImageFetcher = ApolloUtils.getImageFetcher(this);

        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);

        // Theme the action bar
        final ActionBar actionBar = getSupportActionBar();
        mResources.themeActionBar(actionBar, getString(R.string.app_name));
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set the layout
        setContentView(R.layout.activity_player_base);

        // Cache all the items
        initPlaybackControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        invalidateOptionsMenu();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(final SeekBar bar, final int progress, final boolean fromuser) {
        if (!fromuser || mService == null) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            MusicUtils.seek(mPosOverride);
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(final SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(final SeekBar bar) {
        mPosOverride = -1;
        mFromTouch = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        mResources.setFavoriteIcon(menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Search view
        getSupportMenuInflater().inflate(R.menu.search, menu);
        // Theme the search icon
        mResources.setSearchIcon(menu);

        final SearchView searchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
        // Add voice search
        final SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(searchableInfo);
        // Perform the search
        searchView.setOnQueryTextListener(new OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String query) {
                // Open the search activity
                NavUtils.openSearch(AudioPlayerActivity.this, query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                // Nothing to do
                return false;
            }
        });

        // Favorite action
        getSupportMenuInflater().inflate(R.menu.favorite, menu);
        // Shuffle all
        getSupportMenuInflater().inflate(R.menu.shuffle, menu);
        // Share, ringtone, and equalizer
        getSupportMenuInflater().inflate(R.menu.audio_player, menu);
        // Settings
        getSupportMenuInflater().inflate(R.menu.activity_base, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to the home activity
                NavUtils.goHome(this);
                return true;
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(this);
                // Refresh the queue
                ((QueueFragment)mPagerAdapter.getFragment(0)).refreshQueue();
                return true;
            case R.id.menu_favorite:
                // Toggle the current track as a favorite and update the menu
                // item
                MusicUtils.toggleFavorite();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_audio_player_ringtone:
                // Set the current track as a ringtone
                MusicUtils.setRingtone(this, MusicUtils.getCurrentAudioId());
                return true;
            case R.id.menu_audio_player_share:
                // Share the current meta data
                shareCurrentTrack();
                return true;
            case R.id.menu_audio_player_equalizer:
                // Sound effects
                NavUtils.openEffectsPanel(this);
                return true;
            case R.id.menu_download_lyrics:
                updateLyrics(true);
                hideAlbumArt();
                return true;
            case R.id.menu_settings:
                // Settings
                NavUtils.openSettings(this);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavUtils.goHome(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Hide Apollo's notification
        MusicUtils.killForegroundService(this);
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Refresh the queue
        ((QueueFragment)mPagerAdapter.getFragment(0)).refreshQueue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Show Apollo's notification
        if (MusicUtils.isPlaying() && ApolloUtils.isApplicationSentToBackground(this)) {
            MusicUtils.startBackgroundService(this);
        }
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);
        // Unbind from the service
        if (mService != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }

        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }
    }

    /**
     * Initializes the items in the now playing screen
     */
    @SuppressWarnings("deprecation")
    private void initPlaybackControls() {
        // ViewPager container
        mPageContainer = (FrameLayout)findViewById(R.id.audio_player_pager_container);
        // Theme the pager container background
        mPageContainer
                .setBackgroundDrawable(mResources.getDrawable("audio_player_pager_container"));

        // Now playing header
        mAudioPlayerHeader = (LinearLayout)findViewById(R.id.audio_player_header);
        // Opens the currently playing album profile
        mAudioPlayerHeader.setOnClickListener(mOpenAlbumProfile);

        // Used to hide the artwork and show the queue/lyrics
        final FrameLayout mSwitch = (FrameLayout)findViewById(R.id.audio_player_switch);
        mSwitch.setOnClickListener(mToggleHiddenPanel);

        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(this);
        // Queue
        mPagerAdapter.add(QueueFragment.class, null);
        // Lyrics
        mPagerAdapter.add(LyricsFragment.class, null);

        // Initialize the ViewPager
        mViewPager = (ViewPager)findViewById(R.id.audio_player_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Play and pause button
        mPlayPauseButton = (PlayPauseButton)findViewById(R.id.action_button_play);
        // Shuffle button
        mShuffleButton = (ShuffleButton)findViewById(R.id.action_button_shuffle);
        // Repeat button
        mRepeatButton = (RepeatButton)findViewById(R.id.action_button_repeat);
        // Previous button
        mPreviousButton = (RepeatingImageButton)findViewById(R.id.action_button_previous);
        // Next button
        mNextButton = (RepeatingImageButton)findViewById(R.id.action_button_next);
        // Track name
        mTrackName = (TextView)findViewById(R.id.audio_player_track_name);
        // Artist name
        mArtistName = (TextView)findViewById(R.id.audio_player_artist_name);
        // Album art
        mAlbumArt = (ImageView)findViewById(R.id.audio_player_album_art);
        // Small album art
        mAlbumArtSmall = (ImageView)findViewById(R.id.audio_player_switch_album_art);
        // Current time
        mCurrentTime = (TextView)findViewById(R.id.audio_player_current_time);
        // Total time
        mTotalTime = (TextView)findViewById(R.id.audio_player_total_time);
        // Used to show and hide the queue and lyrics fragments
        mQueueSwitch = (ImageView)findViewById(R.id.audio_player_switch_queue);
        // Theme the queue switch icon
        mQueueSwitch.setImageDrawable(mResources.getDrawable("btn_switch_queue"));
        // Progress
        mProgress = (SeekBar)findViewById(android.R.id.progress);

        // Set the repeat listner for the previous button
        mPreviousButton.setRepeatListener(mRewindListener);
        // Set the repeat listner for the next button
        mNextButton.setRepeatListener(mFastForwardListener);
        // Update the progress
        mProgress.setOnSeekBarChangeListener(this);
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        // Set the track name
        mTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mArtistName.setText(MusicUtils.getArtistName());
        // Set the total time
        mTotalTime.setText(MusicUtils.makeTimeString(this, MusicUtils.duration() / 1000));
        // Set the album art
        mImageFetcher.loadCurrentArtwork(mAlbumArt);
        // Set the small artwork
        mImageFetcher.loadCurrentArtwork(mAlbumArtSmall);
        // Update the current time
        queueNextRefresh(1);

    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mPlayPauseButton.updateState();
        // Set the shuffle image
        mShuffleButton.updateShuffleState();
        // Set the repeat image
        mRepeatButton.updateRepeatState();
    }

    /**
     * Refreshes the lyrics and moves the view pager to the lyrics fragment.
     */
    public void updateLyrics(final boolean force) {
        ((LyricsFragment)mPagerAdapter.getFragment(1)).fetchLyrics(force);
        if (force && mViewPager.getCurrentItem() != 1) {
            mViewPager.setCurrentItem(1, true);
        }
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Used to scan backwards in time through the curren track
     * 
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanBackward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(this);
                final long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     * 
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanForward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (mService == null) {
            return 500;
        }
        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            if (pos >= 0 && MusicUtils.duration() > 0) {
                mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                final int progress = (int)(1000 * pos / MusicUtils.duration());
                mProgress.setProgress(progress);

                if (MusicUtils.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    final int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                            : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            final long smoothrefreshtime = MusicUtils.duration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (final Exception ignored) {

        }
        return 500;
    }

    /**
     * @param v The view to animate
     * @param alpha The alpha to apply
     */
    private void fade(final View v, final float alpha) {
        final ObjectAnimator fade = ObjectAnimator.ofFloat(v, "alpha", alpha);
        fade.setInterpolator(AnimationUtils.loadInterpolator(this,
                android.R.anim.accelerate_decelerate_interpolator));
        fade.setDuration(400);
        fade.start();
    }

    /**
     * Called to show the album art and hide the queue/lyrics
     */
    private void showAlbumArt() {
        mPageContainer.setVisibility(View.INVISIBLE);
        mAlbumArtSmall.setVisibility(View.GONE);
        mQueueSwitch.setVisibility(View.VISIBLE);
        // Fade out the pager container
        fade(mPageContainer, 0f);
        // Fade in the album art
        fade(mAlbumArt, 1f);
    }

    /**
     * Called to hide the album art and show the queue/lyrics
     */
    public void hideAlbumArt() {
        mPageContainer.setVisibility(View.VISIBLE);
        mQueueSwitch.setVisibility(View.GONE);
        mAlbumArtSmall.setVisibility(View.VISIBLE);
        // Fade out the artwork
        fade(mAlbumArt, 0f);
        // Fade in the pager container
        fade(mPageContainer, 1f);
    }

    /**
     * /** Used to shared what the user is currently listening to
     */
    private void shareCurrentTrack() {
        if (MusicUtils.getTrackName() == null || MusicUtils.getArtistName() == null) {
            return;
        }
        final Intent shareIntent = new Intent();
        final String shareMessage = getString(R.string.now_listening_to) + " "
                + MusicUtils.getTrackName() + " " + getString(R.string.by) + " "
                + MusicUtils.getArtistName() + " " + getString(R.string.hash_apollo);

        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
    }

    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };

    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanForward(repcnt, howlong);
        }
    };

    /**
     * Switches from the large album art screen to show the queue and lyric
     * fragments, then back again
     */
    private final OnClickListener mToggleHiddenPanel = new OnClickListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(final View v) {
            if (mPageContainer.getVisibility() == View.VISIBLE) {
                // Open the current album profile
                mAudioPlayerHeader.setOnClickListener(mOpenAlbumProfile);
                // Show the artwork, hide the queue and lyrics
                showAlbumArt();
            } else {
                // Scroll to the current track
                mAudioPlayerHeader.setOnClickListener(mScrollToCurrentSong);
                // Show the queue and lyrics, hide the artwork
                hideAlbumArt();
            }
        }
    };

    /**
     * Opens to the current album profile
     */
    private final OnClickListener mOpenAlbumProfile = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            NavUtils.openAlbumProfile(AudioPlayerActivity.this, MusicUtils.getAlbumName(),
                    MusicUtils.getArtistName());
        }
    };

    /**
     * Scrolls the queue to the currently playing song
     */
    private final OnClickListener mScrollToCurrentSong = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            ((QueueFragment)mPagerAdapter.getFragment(0)).scrollToCurrentSong();
        }
    };

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<AudioPlayerActivity> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        public TimeHandler(final AudioPlayerActivity player) {
            mAudioPlayer = new WeakReference<AudioPlayerActivity>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mAudioPlayer.get().refreshCurrentTime();
                    mAudioPlayer.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<AudioPlayerActivity> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final AudioPlayerActivity activity) {
            mReference = new WeakReference<AudioPlayerActivity>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                // Current info
                mReference.get().updateNowPlayingInfo();
                // Update the favorites icon
                mReference.get().invalidateOptionsMenu();
                // Update the lyrics
                mReference.get().updateLyrics(false);
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                // Set the play and pause image
                mReference.get().mPlayPauseButton.updateState();
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
                // Set the repeat image
                mReference.get().mRepeatButton.updateRepeatState();
                // Set the shuffle image
                mReference.get().mShuffleButton.updateShuffleState();
            }
        }
    }

}
