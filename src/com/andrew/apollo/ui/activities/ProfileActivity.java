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

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.PhotoSelectionDialog;
import com.andrew.apollo.menu.PhotoSelectionDialog.ProfileType;
import com.andrew.apollo.ui.fragments.profile.AlbumSongFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistSongFragment;
import com.andrew.apollo.ui.fragments.profile.FavoriteFragment;
import com.andrew.apollo.ui.fragments.profile.GenreSongFragment;
import com.andrew.apollo.ui.fragments.profile.LastAddedFragment;
import com.andrew.apollo.ui.fragments.profile.PlaylistSongFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.SortOrder;
import com.andrew.apollo.widgets.ProfileTabCarousel;
import com.andrew.apollo.widgets.ProfileTabCarousel.Listener;

/**
 * The {@link SherlockFragmentActivity} is used to display the data for specific
 * artists, albums, playlists, and genres. This class is only used on phones.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileActivity extends BaseActivity implements OnPageChangeListener, Listener {

    private static final int NEW_PHOTO = 1;

    /**
     * The Bundle to pass into the Fragments
     */
    private Bundle mArguments;

    /**
     * View pager
     */
    private ViewPager mViewPager;

    /**
     * Pager adpater
     */
    private PagerAdapter mPagerAdapter;

    /**
     * Profile header carousel
     */
    private ProfileTabCarousel mTabCarousel;

    /**
     * MIME type of the profile
     */
    private String mType;

    /**
     * Artist name passed into the class
     */
    private String mArtistName;

    /**
     * The main profile title
     */
    private String mProfileName;

    /**
     * Image cache
     */
    private ImageFetcher mImageFetcher;

    private PreferenceUtils mPreferences;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Temporay until I can work out a nice landscape layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get the preferences
        mPreferences = PreferenceUtils.getInstace(this);

        // Initialze the image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(this);

        // Initialize the Bundle
        mArguments = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        // Get the MIME type
        mType = mArguments.getString(Config.MIME_TYPE);
        // Get the profile title
        mProfileName = mArguments.getString(Config.NAME);
        // Get the artist name
        if (isArtist() || isAlbum()) {
            mArtistName = mArguments.getString(Config.ARTIST_NAME);
        }

        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(this);

        // Initialze the carousel
        mTabCarousel = (ProfileTabCarousel)findViewById(R.id.acivity_profile_base_tab_carousel);
        mTabCarousel.reset();
        mTabCarousel.getPhoto().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ProfileType profileType;
                if (isArtist()) {
                    profileType = ProfileType.ARTIST;
                } else if (isAlbum()) {
                    profileType = ProfileType.ALBUM;
                } else {
                    profileType = ProfileType.OTHER;
                }
                PhotoSelectionDialog.newInstance(isArtist() ? mArtistName : mProfileName,
                        profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
            }
        });
        // Set up the action bar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        /* Set up the artist profile */
        if (isArtist()) {
            // Add the carousel images
            mTabCarousel.setArtistProfileHeader(this, mArtistName);

            // Artist profile fragments
            mPagerAdapter.add(ArtistSongFragment.class, mArguments);
            mPagerAdapter.add(ArtistAlbumFragment.class, mArguments);

            // Action bar title
            mResources.setTitle(mArtistName);

        } else
        // Set up the album profile
        if (isAlbum()) {
            // Add the carousel images
            mTabCarousel.setAlbumProfileHeader(this, mProfileName, mArtistName);

            // Album profile fragments
            mPagerAdapter.add(AlbumSongFragment.class, mArguments);

            // Action bar title = album name
            mResources.setTitle(mProfileName);
            // Action bar subtitle = year released
            mResources.setSubtitle(mArguments.getString(Config.ALBUM_YEAR));
        } else
        // Set up the favorites profile
        if (isFavorites()) {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);

            // Favorite fragment
            mPagerAdapter.add(FavoriteFragment.class, null);

            // Action bar title = Favorites
            mResources.setTitle(mProfileName);
        } else
        // Set up the last added profile
        if (isLastAdded()) {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);

            // Last added fragment
            mPagerAdapter.add(LastAddedFragment.class, null);

            // Action bar title = Last added
            mResources.setTitle(mProfileName);
        } else
        // Set up the user playlist profile
        if (isPlaylist()) {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);

            // Playlist profile fragments
            mPagerAdapter.add(PlaylistSongFragment.class, mArguments);

            // Action bar title = playlist name
            mResources.setTitle(mProfileName);
        } else
        // Set up the genre profile
        if (isGenre()) {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);

            // Genre profile fragments
            mPagerAdapter.add(GenreSongFragment.class, mArguments);

            // Action bar title = playlist name
            mResources.setTitle(mProfileName);
        }

        // Initialize the ViewPager
        mViewPager = (ViewPager)findViewById(R.id.acivity_profile_base_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Attach the page change listener
        mViewPager.setOnPageChangeListener(this);
        // Attach the carousel listener
        mTabCarousel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setContentView() {
        return R.layout.activity_profile_base;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // Theme the add to home screen icon
        mResources.setAddToHomeScreenIcon(menu);
        // Set the shuffle all title to "play all" if a playlist.
        final MenuItem shuffle = menu.findItem(R.id.menu_shuffle);
        String title = null;
        if (isFavorites() || isLastAdded() || isPlaylist()) {
            title = getString(R.string.menu_play_all);
        } else {
            title = getString(R.string.menu_shuffle);
        }
        shuffle.setTitle(title);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Pin to Home screen
        getSupportMenuInflater().inflate(R.menu.add_to_homescreen, menu);
        // Shuffle
        getSupportMenuInflater().inflate(R.menu.shuffle, menu);
        // Sort orders
        if (isArtistSongPage()) {
            getSupportMenuInflater().inflate(R.menu.artist_song_sort_by, menu);
        } else if (isArtistAlbumPage()) {
            getSupportMenuInflater().inflate(R.menu.artist_album_sort_by, menu);
        } else if (isAlbum()) {
            getSupportMenuInflater().inflate(R.menu.album_song_sort_by, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If an album profile, go up to the artist profile
                if (isAlbum()) {
                    NavUtils.openArtistProfile(this, mArtistName);
                    finish();
                } else {
                    // Otherwise just go back
                    goBack();
                }
                return true;
            case R.id.menu_add_to_homescreen:
                // Place the artist, album, genre, or playlist onto the Home
                // screen. Definitely one of my favorite features.
                final String name = isArtist() ? mArtistName : mProfileName;
                final Long id = mArguments.getLong(Config.ID);
                ApolloUtils.createShortcutIntent(name, id, mType, this);
                return true;
            case R.id.menu_shuffle:
                final String stringId = String.valueOf(mArguments.getLong(Config.ID));
                long[] list = null;
                if (isArtist()) {
                    list = MusicUtils.getSongListForArtist(this, stringId);
                } else if (isAlbum()) {
                    list = MusicUtils.getSongListForAlbum(this, stringId);
                } else if (isGenre()) {
                    list = MusicUtils.getSongListForGenre(this, stringId);
                }
                if (isPlaylist()) {
                    MusicUtils.playPlaylist(this, stringId);
                } else if (isFavorites()) {
                    MusicUtils.playFavorites(this);
                } else if (isLastAdded()) {
                    MusicUtils.playLastAdded(this);
                } else {
                    if (list != null && list.length > 0) {
                        MusicUtils.playAll(this, list, 0, true);
                    }
                }
                return true;
            case R.id.menu_sort_by_az:
                if (isArtistSongPage()) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_A_Z);
                    getArtistSongFragment().refresh();
                } else if (isArtistAlbumPage()) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
                    getArtistAlbumFragment().refresh();
                } else {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
                    getAlbumSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_za:
                if (isArtistSongPage()) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_Z_A);
                    getArtistSongFragment().refresh();
                } else if (isArtistAlbumPage()) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
                    getArtistAlbumFragment().refresh();
                } else {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
                    getAlbumSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_album:
                if (isArtistSongPage()) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_ALBUM);
                    getArtistSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_year:
                if (isArtistSongPage()) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_YEAR);
                    getArtistSongFragment().refresh();
                } else if (isArtistAlbumPage()) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
                    getArtistAlbumFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_duration:
                if (isArtistSongPage()) {
                    mPreferences
                            .setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DURATION);
                    getArtistSongFragment().refresh();
                } else {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                    getAlbumSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_date_added:
                if (isArtistSongPage()) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DATE);
                    getArtistSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_track_list:
                mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
                getAlbumSongFragment().refresh();
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
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mArguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goBack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(final int position, final float positionOffset,
            final int positionOffsetPixels) {
        if (mViewPager.isFakeDragging()) {
            return;
        }

        final int scrollToX = (int)((position + positionOffset) * mTabCarousel
                .getAllowedHorizontalScrollLength());
        mTabCarousel.scrollTo(scrollToX, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(final int position) {
        mTabCarousel.setCurrentTab(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(final int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            mTabCarousel.restoreYCoordinate(75, mViewPager.getCurrentItem());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchDown() {
        if (!mViewPager.isFakeDragging()) {
            mViewPager.beginFakeDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchUp() {
        if (mViewPager.isFakeDragging()) {
            mViewPager.endFakeDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        if (mViewPager.isFakeDragging()) {
            mViewPager.fakeDragBy(oldl - l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTabSelected(final int position) {
        mViewPager.setCurrentItem(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_PHOTO) {
            if (resultCode == RESULT_OK) {
                final Uri selectedImage = data.getData();
                final String[] filePathColumn = {
                    MediaStore.Images.Media.DATA
                };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null,
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    final String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    cursor = null;

                    String key = mProfileName;
                    if (isArtist()) {
                        key = mArtistName;
                    } else if (isAlbum()) {
                        key = mProfileName + Config.ALBUM_ART_SUFFIX;
                    }

                    final Bitmap bitmap = ImageFetcher.decodeSampledBitmapFromFile(picturePath);
                    mImageFetcher.addBitmapToCache(key, bitmap);
                    if (isAlbum()) {
                        mTabCarousel.getAlbumArt().setImageBitmap(bitmap);
                    } else {
                        mTabCarousel.getPhoto().setImageBitmap(bitmap);
                    }
                }
            } else {
                selectOldPhoto();
            }
        }
    }

    /**
     * Starts an activity for result that returns an image from the Gallery.
     */
    public void selectNewPhoto() {
        // First remove the old image
        removeFromCache();
        // Now open the gallery
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        startActivityForResult(intent, NEW_PHOTO);
        // Make sure the notfication starts
        if (MusicUtils.isPlaying()) {
            MusicUtils.startBackgroundService(this);
        }
    }

    /**
     * Fetchs for the artist or album art, other wise sets the default header
     * image.
     */
    public void selectOldPhoto() {
        // First remove the old image
        removeFromCache();
        // Apply the old photo
        if (isArtist()) {
            mTabCarousel.setArtistProfileHeader(this, mArtistName);
        } else if (isAlbum()) {
            mTabCarousel.setAlbumProfileHeader(this, mProfileName, mArtistName);
        } else {
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
        }
    }

    /**
     * When the user chooses {@code #selectOldPhoto()} while viewing an album
     * profile, the image is, most likely, reverted back to the locally found
     * artwork. This is specifically for fetching the image from Last.fm.
     */
    public void fetchAlbumArt() {
        // First remove the old image
        removeFromCache();
        // Fetch for the artwork
        mTabCarousel.fetchAlbumPhoto(this, mProfileName);
    }

    /**
     * Searches Google for the artist or album
     */
    public void googleSearch() {
        String query = mProfileName;
        if (isArtist()) {
            query = mArtistName;
        } else if (isAlbum()) {
            query = mProfileName + " " + mArtistName;
        }
        final Intent googleSearch = new Intent(Intent.ACTION_WEB_SEARCH);
        googleSearch.putExtra(SearchManager.QUERY, query);
        startActivity(googleSearch);
        // Make sure the notification starts.
        MusicUtils.startBackgroundService(this);
    }

    /**
     * Removes the header image from the cache.
     */
    private void removeFromCache() {
        String key = mProfileName;
        if (isArtist()) {
            key = mArtistName;
        } else if (isAlbum()) {
            key = mProfileName + Config.ALBUM_ART_SUFFIX;
        }
        mImageFetcher.removeFromCache(key);
        // Give the disk cache a little time before requesting a new image.
        SystemClock.sleep(80);
    }

    /**
     * Finishes the activity and overrides the default animation.
     */
    private void goBack() {
        finish();
    }

    /**
     * @return True if the MIME type is vnd.android.cursor.dir/artists, false
     *         otherwise.
     */
    private final boolean isArtist() {
        return mType.equals(MediaStore.Audio.Artists.CONTENT_TYPE);
    }

    /**
     * @return True if the MIME type is vnd.android.cursor.dir/albums, false
     *         otherwise.
     */
    private final boolean isAlbum() {
        return mType.equals(MediaStore.Audio.Albums.CONTENT_TYPE);
    }

    /**
     * @return True if the MIME type is vnd.android.cursor.dir/gere, false
     *         otherwise.
     */
    private final boolean isGenre() {
        return mType.equals(MediaStore.Audio.Genres.CONTENT_TYPE);
    }

    /**
     * @return True if the MIME type is vnd.android.cursor.dir/playlist, false
     *         otherwise.
     */
    private final boolean isPlaylist() {
        return mType.equals(MediaStore.Audio.Playlists.CONTENT_TYPE);
    }

    /**
     * @return True if the MIME type is "Favorites", false otherwise.
     */
    private final boolean isFavorites() {
        return mType.equals(getString(R.string.playlist_favorites));
    }

    /**
     * @return True if the MIME type is "LastAdded", false otherwise.
     */
    private final boolean isLastAdded() {
        return mType.equals(getString(R.string.playlist_last_added));
    }

    private boolean isArtistSongPage() {
        return isArtist() && mViewPager.getCurrentItem() == 0;
    }

    private boolean isArtistAlbumPage() {
        return isArtist() && mViewPager.getCurrentItem() == 1;
    }

    private ArtistSongFragment getArtistSongFragment() {
        return (ArtistSongFragment)mPagerAdapter.getFragment(0);
    }

    private ArtistAlbumFragment getArtistAlbumFragment() {
        return (ArtistAlbumFragment)mPagerAdapter.getFragment(1);
    }

    private AlbumSongFragment getAlbumSongFragment() {
        return (AlbumSongFragment)mPagerAdapter.getFragment(0);
    }
}
