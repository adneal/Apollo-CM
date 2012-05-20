/**
 * 
 */

package com.andrew.apollo.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrew.apollo.BottomActionBarControlsFragment;
import com.andrew.apollo.BottomActionBarFragment;
import com.andrew.apollo.Constants;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.list.fragments.ArtistAlbumsFragment;
import com.andrew.apollo.list.fragments.TracksFragment;
import com.andrew.apollo.service.ApolloService;
import com.andrew.apollo.service.ServiceToken;
import com.andrew.apollo.tasks.GetCachedImages;
import com.andrew.apollo.tasks.LastfmGetAlbumImages;
import com.andrew.apollo.tasks.LastfmGetArtistImagesOriginal;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

/**
 * @author Andrew Neal
 * @Note This displays specific track or album listings
 */
public class TracksBrowser extends FragmentActivity implements Constants, ServiceConnection {

    // Bundle
    private Bundle bundle;

    private Intent intent;

    private String mimeType;

    private ServiceToken mToken;

    private final long[] mHits = new long[3];

    @Override
    protected void onCreate(Bundle icicle) {
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Layout
        setContentView(R.layout.track_browser);

        // Important!
        whatBundle(icicle);

        // Update the colorstrip color
        initColorstrip();

        // Update the ActionBar
        initActionBar();

        // Update the half_and_half layout
        initUpperHalf();

        // Important!
        initPager();

        // Update the BottomActionBar
        initBottomActionBar();
        super.onCreate(icicle);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putAll(bundle);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    /**
     * Update next BottomActionBar as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ApolloUtils.isArtist(mimeType) || ApolloUtils.isAlbum(mimeType))
                setArtistImage();
        }

    };

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        registerReceiver(mMediaStatusReceiver, filter);

        setTitle();
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);

        unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Artist ID
                long id = ApolloUtils.getArtistId(getArtist(), ARTIST_ID, this);
                if (ApolloUtils.isAlbum(mimeType) && id != 0)
                    tracksBrowser(id);
                super.onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param icicle
     * @return what Bundle we're dealing with
     */
    public void whatBundle(Bundle icicle) {
        intent = getIntent();
        bundle = icicle != null ? icicle : intent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (bundle.getString(INTENT_ACTION) == null) {
            bundle.putString(INTENT_ACTION, intent.getAction());
        }
        if (bundle.getString(MIME_TYPE) == null) {
            bundle.putString(MIME_TYPE, intent.getType());
        }
        mimeType = bundle.getString(MIME_TYPE);
    }

    /**
     * For the theme chooser
     */
    private void initColorstrip() {
        FrameLayout mColorstrip = (FrameLayout)findViewById(R.id.colorstrip);
        mColorstrip.setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
        ThemeUtils.setBackgroundColor(this, mColorstrip, "colorstrip");
    }

    /**
     * Set the ActionBar title
     */
    private void initActionBar() {
        ApolloUtils.showUpTitleOnly(getActionBar());

        // The ActionBar Title and UP ids are hidden.
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        int upId = Resources.getSystem().getIdentifier("up", "id", "android");

        TextView actionBarTitle = (TextView)findViewById(titleId);
        ImageView actionBarUp = (ImageView)findViewById(upId);

        // Theme chooser
        ThemeUtils.setActionBarBackground(this, getActionBar(), "action_bar_background");
        ThemeUtils.setTextColor(this, actionBarTitle, "action_bar_title_color");
        ThemeUtils.initThemeChooser(this, actionBarUp, "action_bar_up", THEME_ITEM_BACKGROUND);

    }

    /**
     * Sets up the @half_and_half.xml layout
     */
    private void initUpperHalf() {

        if (ApolloUtils.isArtist(mimeType)) {
            // Get next artist image
        } else if (ApolloUtils.isAlbum(mimeType)) {
            // Album image
            setAlbumImage();

            // Artist name
            TextView mArtistName = (TextView)findViewById(R.id.half_artist_image_text);
            mArtistName.setVisibility(View.VISIBLE);
            mArtistName.setText(getArtist());
            mArtistName.setBackgroundColor(getResources().getColor(R.color.transparent_black));

            // Album name
            TextView mAlbumName = (TextView)findViewById(R.id.half_album_image_text);
            mAlbumName.setText(getAlbum());
            mAlbumName.setBackgroundColor(getResources().getColor(R.color.transparent_black));

            // Album half container
            RelativeLayout mSecondHalfContainer = (RelativeLayout)findViewById(R.id.album_half_container);
            // Show the second half while viewing an album
            mSecondHalfContainer.setVisibility(View.VISIBLE);
        } else {
            // Set the logo
            setPromoImage();
        }
    }

    /**
     * Initiate ViewPager and PagerAdapter
     */
    private void initPager() {
        // Initiate PagerAdapter
        PagerAdapter mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        if (ApolloUtils.isArtist(mimeType))
            // Show all albums for an artist
            mPagerAdapter.addFragment(new ArtistAlbumsFragment(bundle));
        // Show the tracks for an artist or album
        mPagerAdapter.addFragment(new TracksFragment(bundle));

        // Set up ViewPager
        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
        mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mViewPager.setAdapter(mPagerAdapter);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mViewPager, "viewpager", THEME_ITEM_BACKGROUND);
        ThemeUtils.setMarginDrawable(this, mViewPager, "viewpager_margin");
    }

    /**
     * Initiate the BottomActionBar
     */
    private void initBottomActionBar() {
        PagerAdapter pagerAdatper = new PagerAdapter(getSupportFragmentManager());
        pagerAdatper.addFragment(new BottomActionBarFragment());
        pagerAdatper.addFragment(new BottomActionBarControlsFragment());
        ViewPager viewPager = (ViewPager)findViewById(R.id.bottomActionBarPager);
        viewPager.setAdapter(pagerAdatper);
    }

    /**
     * @return artist name from Bundle
     */
    public String getArtist() {
        if (bundle.getString(ARTIST_KEY) != null)
            return bundle.getString(ARTIST_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return album name from Bundle
     */
    public String getAlbum() {
        if (bundle.getString(ALBUM_KEY) != null)
            return bundle.getString(ALBUM_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return genre name from Bundle
     */
    public String getGenre() {
        if (bundle.getString(GENRE_KEY) != null)
            return bundle.getString(GENRE_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return playlist name from Bundle
     */
    public String getPlaylist() {
        if (bundle.getString(PLAYLIST_NAME) != null)
            return bundle.getString(PLAYLIST_NAME);
        return getResources().getString(R.string.app_name);
    }

    /**
     * Set the header when viewing a genre
     */
    public void setPromoImage() {

        // Artist image & Genre image
        ImageView mFirstHalfImage = (ImageView)findViewById(R.id.half_artist_image);

        Bitmap header = BitmapFactory.decodeResource(getResources(), R.drawable.promo);
        ApolloUtils.runnableBackground(mFirstHalfImage, header);
    }

    /**
     * Cache and set artist image
     */
    public void setArtistImage() {

        // Artist image & Genre image
        final ImageView mFirstHalfImage = (ImageView)findViewById(R.id.half_artist_image);

        mFirstHalfImage.post(new Runnable() {
            @Override
            public void run() {
                // Only download images we don't already have
                if (ApolloUtils.getImageURL(getArtist(), ARTIST_IMAGE_ORIGINAL, TracksBrowser.this) == null)
                    new LastfmGetArtistImagesOriginal(TracksBrowser.this, mFirstHalfImage)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getArtist());
                // Get and set cached image
                new GetCachedImages(TracksBrowser.this, 0, mFirstHalfImage).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, getArtist());
            }
        });

        mFirstHalfImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 250)) {
                    AnimationDrawable meow = ApolloUtils.getNyanCat(TracksBrowser.this);
                    mFirstHalfImage.setImageDrawable(meow);
                    meow.start();
                }
            }
        });
    }

    /**
     * Cache and set album image
     */
    public void setAlbumImage() {

        // Album image
        final ImageView mSecondHalfImage = (ImageView)findViewById(R.id.half_album_image);

        mSecondHalfImage.post(new Runnable() {
            @Override
            public void run() {
                // Only download images we don't already have
                if (ApolloUtils.getImageURL(getAlbum(), ALBUM_IMAGE, TracksBrowser.this) == null)
                    new LastfmGetAlbumImages(TracksBrowser.this, mSecondHalfImage, 1)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getArtist(),
                                    getAlbum());
                // Get and set cached image
                new GetCachedImages(TracksBrowser.this, 1, mSecondHalfImage).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, getAlbum());
            }
        });
    }

    /**
     * Return here from viewing the tracks for an album and view all albums and
     * tracks for the same artist
     */
    private void tracksBrowser(long id) {

        bundle.putString(MIME_TYPE, Audio.Artists.CONTENT_TYPE);
        bundle.putString(ARTIST_KEY, getArtist());
        bundle.putLong(BaseColumns._ID, id);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, TracksBrowser.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Set the correct title
     */
    private void setTitle() {
        String name;
        long id;
        if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            switch ((int)id) {
                case (int)PLAYLIST_QUEUE:
                    setTitle(R.string.nowplaying);
                    return;
                case (int)PLAYLIST_FAVORITES:
                    setTitle(R.string.favorite);
                    return;
                default:
                    if (id < 0) {
                        setTitle(R.string.app_name);
                        return;
                    }
            }
            name = MusicUtils.getPlaylistName(this, id);
        } else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name = MusicUtils.getArtistName(this, id, true);
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name = MusicUtils.getAlbumName(this, id, true);
        } else if (Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name = MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true));
        } else {
            setTitle(R.string.app_name);
            return;
        }
        setTitle(name);
    }
}
