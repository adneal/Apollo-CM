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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuInflater;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.SongAdapter;
import com.andrew.apollo.dragdrop.DragSortListView;
import com.andrew.apollo.dragdrop.DragSortListView.DragScrollProfile;
import com.andrew.apollo.dragdrop.DragSortListView.DropListener;
import com.andrew.apollo.dragdrop.DragSortListView.RemoveListener;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.List;

/**
 * This class is used to display all of the songs in the queue.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueFragment extends SherlockFragment implements LoaderCallbacks<List<Song>>,
        OnItemClickListener, DropListener, RemoveListener, DragScrollProfile {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 13;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * The adapter for the list
     */
    private SongAdapter mAdapter;

    /**
     * The list view
     */
    private DragSortListView mListView;

    /**
     * Represents a song
     */
    private Song mSong;

    /**
     * Position of a context menu item
     */
    private int mSelectedPosition;

    /**
     * Id of a context menu item
     */
    private long mSelectedId;

    /**
     * Song, album, and artist name used in the context menu
     */
    private String mSongName, mAlbumName, mArtistName;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public QueueFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adpater
        mAdapter = new SongAdapter(getSherlockActivity(), R.layout.edit_track_list_item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.list_base, null);
        // Initialize the list
        mListView = (DragSortListView)rootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        mListView.setOnCreateContextMenuListener(this);
        // Play the selected song
        mListView.setOnItemClickListener(this);
        // Set the drop listener
        mListView.setDropListener(this);
        // Set the swipe to remove listener
        mListView.setRemoveListener(this);
        // Quick scroll while dragging
        mListView.setDragScrollProfile(this);
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
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(final com.actionbarsherlock.view.Menu menu,
            final MenuInflater inflater) {
        inflater.inflate(R.menu.queue, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_queue:
                NowPlayingCursor queue = (NowPlayingCursor)QueueLoader
                        .makeQueueCursor(getSherlockActivity());
                CreateNewPlaylist.getInstance(MusicUtils.getSongListForCursor(queue)).show(
                        getFragmentManager(), "CreatePlaylist");
                queue.close();
                queue = null;
                return true;
            case R.id.menu_clear_queue:
                MusicUtils.clearQueue();
                NavUtils.goHome(getSherlockActivity());
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
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the position of the selected item
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        mSelectedPosition = info.position;
        // Creat a new song
        mSong = mAdapter.getItem(mSelectedPosition);
        mSelectedId = Long.valueOf(mSong.mSongId);
        mSongName = mSong.mSongName;
        mAlbumName = mSong.mAlbumName;
        mArtistName = mSong.mArtistName;

        // Play the song next
        menu.add(GROUP_ID, FragmentMenuItems.PLAY_NEXT, Menu.NONE,
                getString(R.string.context_menu_play_next));

        // Add the song to the queue
        menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE,
                getString(R.string.add_to_queue));

        // Add the song to a playlist
        final SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST,
                Menu.NONE, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(getSherlockActivity(), GROUP_ID, subMenu, true);

        // View more content by the song artist
        menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE,
                getString(R.string.context_menu_more_by_artist));

        // Make the song a ringtone
        menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE,
                getString(R.string.context_menu_use_as_ringtone));

        // Delete the song
        // menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE,
        // getString(R.string.context_menu_delete));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_NEXT:
                    NowPlayingCursor queue = (NowPlayingCursor)QueueLoader
                            .makeQueueCursor(getSherlockActivity());
                    queue.removeItem(mSelectedPosition);
                    queue.close();
                    queue = null;
                    MusicUtils.playNext(new long[] {
                        mSelectedId
                    });
                    getLoaderManager().restartLoader(LOADER, null, this);
                    return true;
                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(getSherlockActivity(), new long[] {
                        mSelectedId
                    });
                    return true;
                case FragmentMenuItems.ADD_TO_FAVORITES:
                    FavoritesStore.getInstance(getSherlockActivity()).addSongId(
                            Long.valueOf(mSelectedId), mSongName, mAlbumName, mArtistName);
                    return true;
                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(new long[] {
                        mSelectedId
                    }).show(getFragmentManager(), "CreatePlaylist");
                    return true;
                case FragmentMenuItems.PLAYLIST_SELECTED:
                    final long mPlaylistId = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(getSherlockActivity(), new long[] {
                        mSelectedId
                    }, mPlaylistId);
                    return true;
                case FragmentMenuItems.MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getSherlockActivity(), mArtistName);
                    return true;
                case FragmentMenuItems.USE_AS_RINGTONE:
                    MusicUtils.setRingtone(getSherlockActivity(), mSelectedId);
                    return true;
                    // case FragmentMenuItems.DELETE:
                    // return true;
                default:
                    break;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        // When selecting a track from the queue, just jump there instead of
        // reloading the queue. This is both faster, and prevents accidentally
        // dropping out of party shuffle.
        MusicUtils.setQueuePosition(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new QueueLoader(getSherlockActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<List<Song>> loader, final List<Song> data) {
        // Check for any errors
        if (data.isEmpty()) {
            return;
        }

        // Start fresh
        mAdapter.unload();
        // Add the data to the adpater
        for (final Song song : data) {
            mAdapter.add(song);
        }
        // Build the cache
        mAdapter.buildCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(final Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getSpeed(final float w, final long t) {
        if (w > 0.8f) {
            return mAdapter.getCount() / 0.001f;
        } else {
            return 10.0f * w;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final int which) {
        mSong = mAdapter.getItem(which);
        mAdapter.remove(mSong);
        mAdapter.notifyDataSetChanged();
        MusicUtils.removeTrack(Long.valueOf(mSong.mSongId));
        // Build the cache
        mAdapter.buildCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final int from, final int to) {
        mSong = mAdapter.getItem(from);
        mAdapter.remove(mSong);
        mAdapter.insert(mSong, to);
        mAdapter.notifyDataSetChanged();
        MusicUtils.moveQueueItem(from, to);
        // Build the cache
        mAdapter.buildCache();
    }

    /**
     * Scrolls the list to the currently playing song when the user touches the
     * header in the {@link TitlePageIndicator}.
     */
    public void scrollToCurrentSong() {
        final int currentSongPosition = getItemPositionBySong();

        if (currentSongPosition != 0) {
            mListView.setSelection(currentSongPosition);
        }
    }

    /**
     * @return The position of an item in the list based on the name of the
     *         currently playing song.
     */
    private int getItemPositionBySong() {
        final String trackName = String.valueOf(MusicUtils.getCurrentAudioId());
        if (mAdapter == null || TextUtils.isEmpty(trackName)) {
            return 0;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mAdapter.getItem(i).mSongId.equals(trackName)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Called to restart the loader callbacks
     */
    public void refreshQueue() {
        if (isAdded()) {
            getLoaderManager().restartLoader(LOADER, null, this);
        }
    }
}
