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
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Window;

import com.andrew.apollo.BottomActionBarControlsFragment;
import com.andrew.apollo.BottomActionBarFragment;
import com.andrew.apollo.Constants;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.adapters.ScrollingTabsAdapter;
import com.andrew.apollo.grid.fragments.AlbumsFragment;
import com.andrew.apollo.grid.fragments.ArtistsFragment;
import com.andrew.apollo.list.fragments.GenresFragment;
import com.andrew.apollo.list.fragments.PlaylistsFragment;
import com.andrew.apollo.list.fragments.RecentlyAddedFragment;
import com.andrew.apollo.list.fragments.TracksFragment;
import com.andrew.apollo.service.ApolloService;
import com.andrew.apollo.service.ServiceToken;
import com.andrew.apollo.ui.widgets.ScrollableTabView;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

/**
 * @author Andrew Neal
 * @Note This is the "holder" for all of the tabs
 */
public class MusicLibrary extends FragmentActivity implements ServiceConnection, Constants {

    private ServiceToken mToken;

    @Override
    protected void onCreate(Bundle icicle) {
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Scan for music
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Layout
        setContentView(R.layout.library_browser);

        // Hide the ActionBar
        getActionBar().hide();

        // Important!
        initPager();

        // Update the BottomActionBar
        initBottomActionBar();
        super.onCreate(icicle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);

        IntentFilter scannerFilter = new IntentFilter();
        scannerFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        scannerFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        scannerFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        scannerFilter.addDataScheme(DATA_SCHEME);

        registerReceiver(mScanListener, scannerFilter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);

        unregisterReceiver(mScanListener);
        super.onStop();
    }

    /**
     * Initiate ViewPager and PagerAdapter
     */
    public void initPager() {
        // Initiate PagerAdapter
        PagerAdapter mPagerAdapter = new PagerAdapter(getSupportFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString(MIME_TYPE, Audio.Playlists.CONTENT_TYPE);
        bundle.putLong(BaseColumns._ID, PLAYLIST_RECENTLY_ADDED);
        // Recently added tracks
        mPagerAdapter.addFragment(new RecentlyAddedFragment(bundle));
        // Artists
        mPagerAdapter.addFragment(new ArtistsFragment());
        // Albums
        mPagerAdapter.addFragment(new AlbumsFragment());
        // // Tracks
        mPagerAdapter.addFragment(new TracksFragment());
        // // Playlists
        mPagerAdapter.addFragment(new PlaylistsFragment());
        // // Genres
        mPagerAdapter.addFragment(new GenresFragment());

        // Initiate ViewPager
        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
        mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(1);

        // Tabs
        initScrollableTabs(mViewPager);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mViewPager, "viewpager", THEME_ITEM_BACKGROUND);
        ThemeUtils.setMarginDrawable(this, mViewPager, "viewpager_margin");
    }

    /**
     * Initiate the tabs
     */
    public void initScrollableTabs(ViewPager mViewPager) {
        ScrollableTabView mScrollingTabs = (ScrollableTabView)findViewById(R.id.scrollingTabs);
        ScrollingTabsAdapter mScrollingTabsAdapter = new ScrollingTabsAdapter(this);
        mScrollingTabs.setAdapter(mScrollingTabsAdapter);
        mScrollingTabs.setViewPager(mViewPager);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mScrollingTabs, "scrollable_tab_background",
                THEME_ITEM_BACKGROUND);
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

    /*
     * This listener gets called when the media scanner starts up or finishes,
     * and when the sd card is unmounted.
     */
    private final BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)
                    || Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                MusicUtils.setSpinnerState(MusicLibrary.this);
            }
        }
    };
}
