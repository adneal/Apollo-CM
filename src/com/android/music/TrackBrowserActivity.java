/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.music;

import java.util.Arrays;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.AbstractCursor;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.music.MusicUtils.ServiceToken;

public class TrackBrowserActivity extends ListActivity implements
		View.OnCreateContextMenuListener, MusicUtils.Defs, ServiceConnection {
	private static final int Q_SELECTED = CHILD_MENU_BASE;
	private static final int Q_ALL = CHILD_MENU_BASE + 1;
	private static final int SAVE_AS_PLAYLIST = CHILD_MENU_BASE + 2;
	private static final int PLAY_ALL = CHILD_MENU_BASE + 3;
	private static final int CLEAR_PLAYLIST = CHILD_MENU_BASE + 4;
	private static final int REMOVE = CHILD_MENU_BASE + 5;
	private static final int SEARCH = CHILD_MENU_BASE + 6;

	private static final String LOGTAG = "TrackBrowser";

	private String[] mCursorCols;
	private String[] mPlaylistMemberCols;
	private boolean mDeletedOneRow = false;
	private boolean mEditMode = false;
	private String mCurrentTrackName;
	private String mCurrentAlbumName;
	private String mCurrentArtistNameForAlbum;
	private ListView mTrackList;
	private Cursor mTrackCursor;
	private TrackListAdapter mAdapter;
	private boolean mAdapterSent = false;
	private String mAlbumId;
	private String mArtistId;
	private String mPlaylist;
	private String mGenre;
	private String mSortOrder;
	private int mSelectedPosition;
	private long mSelectedId;
	private static int mLastListPosCourse = -1;
	private static int mLastListPosFine = -1;
	private boolean mUseLastListPos = false;
	private ServiceToken mToken;
	private ServiceToken mOSC;
	private IMediaPlaybackService mService = null;
	private SharedPreferences mPreferences;
	// Artist tab layout
	public LinearLayout mSongTab;
	public TabWidget mButtonBar;
	public TextView mButtonBarArtist;
	public TextView mButtonBarAlbum;
	public TextView mButtonBarSong;
	public TextView mButtonBarPlaylist;
	public TextView mButtonBarNP;
	public RelativeLayout mGroup;
	public RelativeLayout mChild;
	// Smaller now playing window buttons
	private LinearLayout mNowPlaying;
	private ImageView mInfoDivider;
	private ProgressBar mProgress;
	private ImageButton mDoSearch;
	private ImageButton mMarket;
	private ImageButton mNext;
	private ImageButton mPlay;
	private ImageButton mPlusPlaylist;
	private ImageButton mPrev;
	private ImageButton mShare;
	private ImageButton mFlow;
	// Back button long press
	public String back_button_db;
	// Smaller now playing window swipe gesture
	public GestureDetector gestureDetector;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private String np_swipe_gesture_db;
	// Swipe tabs
	private GestureDetector swipetabs;
	// ADW Theme constants
	public static final int THEME_ITEM_BACKGROUND = 0;
	public static final int THEME_ITEM_FOREGROUND = 1;
	public static final int THEME_ITEM_TEXT_DRAWABLE = 2;

	public TrackBrowserActivity() {
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ActionBar bar = getActionBar();
		try {
			bar.setTitle(mService.getArtistName());
			bar.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.solid_dark_pressed));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bar.setDisplayHomeAsUpEnabled(true);

		mPreferences = getSharedPreferences(
				MusicSettingsActivity.PREFERENCES_FILE, MODE_PRIVATE);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.getBooleanExtra("withtabs", false)) {
				bar.hide();
			}
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if (icicle != null) {
			mSelectedId = icicle.getLong("selectedtrack");
			mAlbumId = icicle.getString("album");
			mArtistId = icicle.getString("artist");
			mPlaylist = icicle.getString("playlist");
			mGenre = icicle.getString("genre");
			mEditMode = icicle.getBoolean("editmode", false);
		} else {
			mAlbumId = intent.getStringExtra("album");
			// If we have an album, show everything on the album, not just stuff
			// by a particular artist.
			mArtistId = intent.getStringExtra("artist");
			mPlaylist = intent.getStringExtra("playlist");
			mGenre = intent.getStringExtra("genre");
			mEditMode = intent.getAction().equals(Intent.ACTION_EDIT);
		}

		mCursorCols = new String[] { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ARTIST_ID,
				MediaStore.Audio.Media.DURATION };
		mPlaylistMemberCols = new String[] {
				MediaStore.Audio.Playlists.Members._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ARTIST_ID,
				MediaStore.Audio.Media.DURATION,
				MediaStore.Audio.Playlists.Members.PLAY_ORDER,
				MediaStore.Audio.Playlists.Members.AUDIO_ID,
				MediaStore.Audio.Media.IS_MUSIC };

		setContentView(R.layout.media_picker_activity);

		mToken = MusicUtils.bindToService(this, this);
		// We have to bind to osc to control the music, but we need to keep the
		// previous line to load the list view.
		mOSC = MusicUtils.bindToService(this, osc);

		mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
		mTrackList = getListView();
		mTrackList.setOnCreateContextMenuListener(this);
		mTrackList.setCacheColorHint(0);
		if (mEditMode) {
			LinearLayout nowPlaying = (LinearLayout) findViewById(R.id.nowplaying);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				nowPlaying.setVisibility(View.GONE);
			}
			((TouchInterceptor) mTrackList).setDropListener(mDropListener);
			((TouchInterceptor) mTrackList).setRemoveListener(mRemoveListener);
			mTrackList.setDivider(null);
			mTrackList.setSelector(R.drawable.list_selector_background);
			// ADW: Load the specified theme
			String themePackage = MusicUtils.getThemePackageName(this,
					MusicSettingsActivity.THEME_DEFAULT);
			PackageManager pm = getPackageManager();
			Resources themeResources = null;
			if (!themePackage.equals(MusicSettingsActivity.THEME_DEFAULT)) {
				try {
					themeResources = pm
							.getResourcesForApplication(themePackage);
				} catch (NameNotFoundException e) {
					// ADW The saved theme was uninstalled so we save the
					// default one
					MusicUtils.setThemePackageName(this,
							MusicSettingsActivity.THEME_DEFAULT);
				}
			}
			// Set Views for themes
			if (themeResources != null) {
				int lS = themeResources.getIdentifier("queue_selector",
						"drawable", themePackage);
				if (lS != 0) {
					mTrackList.setSelector(themeResources.getDrawable(lS));
				}
			}
		} else {
			mTrackList.setTextFilterEnabled(true);
		}
		mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();

		if (mAdapter != null) {
			mAdapter.setActivity(this);
			setListAdapter(mAdapter);
		}

		// Swipe up gesture
		gestureDetector = new GestureDetector(new NowPlayingGesture());
		View nowplayingview = (View) findViewById(R.id.nowplaying);
		// Tab swipe
		swipetabs = new GestureDetector(new TabSwipe());
		View mainview = (View) findViewById(android.R.id.list);

		// Set the touch listener for the main view to be our custom gesture
		// listener
		if (!mEditMode) {
			mainview.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (swipetabs.onTouchEvent(event)) {
						return false;
					}
					return false;
				}
			});
		} else {

		}
		// Smaller now playing window buttons
		mShare = (ImageButton) findViewById(R.id.share_song);
		mPlay = (ImageButton) findViewById(R.id.media_play);
		mNext = (ImageButton) findViewById(R.id.media_next);
		mPrev = (ImageButton) findViewById(R.id.media_prev);
		mMarket = (ImageButton) findViewById(R.id.market_music);
		mDoSearch = (ImageButton) findViewById(R.id.doSearch);
		mPlusPlaylist = (ImageButton) findViewById(R.id.plus_playlist);
		mFlow = (ImageButton) findViewById(R.id.overFlow);
		// I found that without setting the onClickListeners in Portrait mode
		// only, flipping into Landscape force closes.
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			// Smaller now playing window actions
			mDoSearch.setOnClickListener(mDoSearchListener);
			mMarket.setOnClickListener(mMarketSearch);
			mPlay.setOnClickListener(mMediaPlay);
			mNext.setOnClickListener(mMediaNext);
			mPrev.setOnClickListener(mMediaPrev);
			mShare.setOnClickListener(mShareSong);
			mFlow.setOnClickListener(mOverFlow);
			// I only want this to show in the Playlist activity
			mPlusPlaylist.setVisibility(View.GONE);
			// Swipe gesture
			nowplayingview.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View vee, MotionEvent nowplayingevent) {
					if (gestureDetector.onTouchEvent(nowplayingevent)) {
						return false;
					}
					return false;
				}
			});
		}
		// ADW: Load the specified theme
		String themePackage = MusicUtils.getThemePackageName(this,
				MusicSettingsActivity.THEME_DEFAULT);
		PackageManager pm = getPackageManager();
		Resources themeResources = null;
		if (!themePackage.equals(MusicSettingsActivity.THEME_DEFAULT)) {
			try {
				themeResources = pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// ADW The saved theme was uninstalled so we save the
				// default one
				MusicUtils.setThemePackageName(this,
						MusicSettingsActivity.THEME_DEFAULT);
			}
		}
		// Set Views for themes
		if (themeResources != null) {
			// Artist tab
			mSongTab = (LinearLayout) findViewById(R.id.album_tab);
			mButtonBar = (TabWidget) findViewById(R.id.buttonbar);
			mButtonBarArtist = (TextView) findViewById(R.id.artisttab);
			mButtonBarAlbum = (TextView) findViewById(R.id.albumtab);
			mButtonBarSong = (TextView) findViewById(R.id.songtab);
			mButtonBarPlaylist = (TextView) findViewById(R.id.playlisttab);
			mButtonBarNP = (TextView) findViewById(R.id.nowplayingtab);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_song", mSongTab, THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "buttonbar", mButtonBar,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_bg_artist", mButtonBarArtist,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_bg_album", mButtonBarAlbum,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_bg_song", mButtonBarSong,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_bg_playlist", mButtonBarPlaylist,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "tab_bg_nowplaying", mButtonBarNP,
					THEME_ITEM_BACKGROUND);
			// Small now playing window
			mNowPlaying = (LinearLayout) findViewById(R.id.nowplaying);
			mInfoDivider = (ImageView) findViewById(R.id.info_divider);
			mProgress = (ProgressBar) findViewById(R.id.progress);
			mShare = (ImageButton) findViewById(R.id.share_song);
			mPlay = (ImageButton) findViewById(R.id.media_play);
			mNext = (ImageButton) findViewById(R.id.media_next);
			mPrev = (ImageButton) findViewById(R.id.media_prev);
			mMarket = (ImageButton) findViewById(R.id.market_music);
			mDoSearch = (ImageButton) findViewById(R.id.doSearch);
			mFlow = (ImageButton) findViewById(R.id.overFlow);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_background", mNowPlaying,
						THEME_ITEM_BACKGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_progress", mProgress,
						THEME_ITEM_BACKGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_info_divider", mInfoDivider,
						THEME_ITEM_BACKGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_share", mShare,
						THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_play", mPlay, THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_next", mNext, THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_prev", mPrev, THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_market", mMarket,
						THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_search", mDoSearch,
						THEME_ITEM_FOREGROUND);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_flow", mFlow, THEME_ITEM_FOREGROUND);
			}
		}
		// don't set the album art until after the view has been layed out
		mTrackList.post(new Runnable() {

			public void run() {
				setAlbumArtBackground();
			}
		});
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);

		if (mAdapter == null) {
			// Log.i("@@@", "starting query");
			mAdapter = new TrackListAdapter(
					getApplication(), // need to use application context to
										// avoid leaks
					this,
					mEditMode ? R.layout.edit_track_list_item
							: R.layout.track_list_item,
					null, // cursor
					new String[] {}, new int[] {},
					"nowplaying".equals(mPlaylist),
					mPlaylist != null
							&& !(mPlaylist.equals("podcasts") || mPlaylist
									.equals("recentlyadded")));
			setListAdapter(mAdapter);
			setTitle(R.string.working_songs);
			getTrackCursor(mAdapter.getQueryHandler(), null, true);
		} else {
			mTrackCursor = mAdapter.getCursor();
			// If mTrackCursor is null, this can be because it doesn't have
			// a cursor yet (because the initial query that sets its cursor
			// is still in progress), or because the query failed.
			// In order to not flash the error dialog at the user for the
			// first case, simply retry the query when the cursor is null.
			// Worst case, we end up doing the same query twice.
			if (mTrackCursor != null) {
				init(mTrackCursor, false);
			} else {
				setTitle(R.string.working_songs);
				getTrackCursor(mAdapter.getQueryHandler(), null, true);
			}
		}
		if (!mEditMode) {
			MusicUtils.updateNowPlaying(this);
		}
	}

	public void onServiceDisconnected(ComponentName name) {
		// we can't really function without the service, so don't
		finish();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		TrackListAdapter a = mAdapter;
		mAdapterSent = true;
		return a;
	}

	// Tab Swipe
	class TabSwipe extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Intent intent = new Intent(Intent.ACTION_PICK);

			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					intent.setDataAndType(Uri.EMPTY,
							MediaStore.Audio.Playlists.CONTENT_TYPE);
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					TrackBrowserActivity.this.overridePendingTransition(
							R.anim.slide_in_right, R.anim.slide_out_left);
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					intent.setDataAndType(Uri.EMPTY,
							"vnd.android.cursor.dir/album");
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					TrackBrowserActivity.this.overridePendingTransition(
							R.anim.slide_in_left, R.anim.slide_out_right);
					return true;

				}

			} catch (Exception e) {
				Log.e("Fling", "There was an error processing the Fling event:"
						+ e.getMessage());
			}
			return true;
		}

		// It is necessary to return true from onDown for the onFling event to
		// register
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
	}

	// Swipe gesture
	class NowPlayingGesture extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e3, MotionEvent e4, float vX,
				float vY) {
			np_swipe_gesture_db = mPreferences.getString("np_swipe_gesture",
					"0");
			if (np_swipe_gesture_db.equals("0")) {
				if (e3.getY() - e4.getY() > SWIPE_MIN_DISTANCE
						&& Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY) {
					// NONE
				}
			}

			np_swipe_gesture_db = mPreferences.getString("np_swipe_gesture",
					"1");
			if (np_swipe_gesture_db.equals("1")) {
				if (e3.getY() - e4.getY() > SWIPE_MIN_DISTANCE
						&& Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY) {
					doPauseResume();
				}
			}

			np_swipe_gesture_db = mPreferences.getString("np_swipe_gesture",
					"2");
			if (np_swipe_gesture_db.equals("2")) {
				if (e3.getY() - e4.getY() > SWIPE_MIN_DISTANCE
						&& Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY) {
					doNext();
				}
			}
			np_swipe_gesture_db = mPreferences.getString("np_swipe_gesture",
					"3");
			if (np_swipe_gesture_db.equals("3")) {
				if (e3.getY() - e4.getY() > SWIPE_MIN_DISTANCE
						&& Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY) {
					doPrev();
				}
			}
			return false;

		}

		@Override
		public boolean onDown(MotionEvent f) {
			return true;
		}
	}

	// Smaller now playing window OnClickListerners
	private View.OnClickListener mMarketSearch = new View.OnClickListener() {
		public void onClick(View v) {
			Uri marketUri = null;
			try {
				marketUri = Uri.parse("market://search?q= "
						+ mService.getArtistName());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Intent marketIntent = new Intent(Intent.ACTION_VIEW)
					.setData(marketUri);
			startActivity(marketIntent);
			finish();

		}
	};
	private View.OnClickListener mShareSong = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			try {
				shareCurrentTrack();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	};

	private String shareCurrentTrack() throws RemoteException {
		if (mService.getTrackName() == null || mService.getArtistName() == null) {

		}

		Intent shareIntent = new Intent();
		String currentTrackMessage = "Now listening to: "
				+ mService.getTrackName() + " by " + mService.getArtistName();

		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, currentTrackMessage);

		startActivity(Intent.createChooser(shareIntent, "Share track using"));
		return currentTrackMessage;
	}

	private View.OnClickListener mOverFlow = new View.OnClickListener() {
		public void onClick(View v) {
			openOptionsMenu();
		}
	};
	private View.OnClickListener mDoSearchListener = new View.OnClickListener() {
		public void onClick(View v) {
			onSearchRequested();
		}
	};
	private View.OnClickListener mMediaPlay = new View.OnClickListener() {
		public void onClick(View v) {
			doPauseResume();
		}
	};
	private View.OnClickListener mMediaNext = new View.OnClickListener() {
		public void onClick(View v) {
			doNext();
		}
	};
	private View.OnClickListener mMediaPrev = new View.OnClickListener() {
		public void onClick(View v) {
			doPrev();
		}
	};

	@Override
	public void onDestroy() {
		ListView lv = getListView();
		if (lv != null) {
			if (mUseLastListPos) {
				mLastListPosCourse = lv.getFirstVisiblePosition();
				View cv = lv.getChildAt(0);
				if (cv != null) {
					mLastListPosFine = cv.getTop();
				}
			}
			if (mEditMode) {
				// clear the listeners so we won't get any more callbacks
				((TouchInterceptor) lv).setDropListener(null);
				((TouchInterceptor) lv).setRemoveListener(null);
			}
		}

		MusicUtils.unbindFromService(mToken);
		MusicUtils.unbindFromService(mOSC);
		try {
			if ("nowplaying".equals(mPlaylist)) {
				unregisterReceiverSafe(mNowPlayingListener);
			} else {
				unregisterReceiverSafe(mTrackListListener);
			}
		} catch (IllegalArgumentException ex) {
			// we end up here in case we never registered the listeners
		}

		// If we have an adapter and didn't send it off to another activity yet,
		// we should
		// close its cursor, which we do by assigning a null cursor to it. Doing
		// this
		// instead of closing the cursor directly keeps the framework from
		// accessing
		// the closed cursor later.
		if (!mAdapterSent && mAdapter != null) {
			mAdapter.changeCursor(null);
		}
		// Because we pass the adapter to the next activity, we need to make
		// sure it doesn't keep a reference to this activity. We can do this
		// by clearing its DatasetObservers, which setListAdapter(null) does.
		setListAdapter(null);
		mAdapter = null;
		unregisterReceiverSafe(mScanListener);
		super.onDestroy();
	}

	/**
	 * Unregister a receiver, but eat the exception that is thrown if the
	 * receiver was never registered to begin with. This is a little easier than
	 * keeping track of whether the receivers have actually been registered by
	 * the time onDestroy() is called.
	 */
	private void unregisterReceiverSafe(BroadcastReceiver receiver) {
		try {
			unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	private void startPlayback() {

		if (mService == null)
			return;
		Intent intent = getIntent();
		String filename = "";
		Uri uri = intent.getData();
		if (uri != null && uri.toString().length() > 0) {
			// If this is a file:// URI, just use the path directly instead
			// of going through the open-from-filedescriptor codepath.
			String scheme = uri.getScheme();
			if ("file".equals(scheme)) {
				filename = uri.getPath();
			} else {
				filename = uri.toString();
			}
			try {
				refreshProgress();
				mService.stop();
				mService.openFile(filename);
				mService.play();
				setIntent(new Intent());
			} catch (Exception ex) {
				Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
			}
		}
	}

	private ServiceConnection osc = new ServiceConnection() {
		public void onServiceConnected(ComponentName classname, IBinder obj) {
			mService = IMediaPlaybackService.Stub.asInterface(obj);
			startPlayback();
			// This is for orientation changes or entering the tab from a
			// different activity
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				setPauseButtonImage();
				// This is so the controls will load correctly according to the
				// user's settings
				MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
			}
			try {
				if (mService.getAudioId() >= 0 || mService.isPlaying()
						|| mService.getPath() != null) {
					return;
				}
			} catch (RemoteException ex) {
			}
			if (getIntent().getData() == null) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setClass(TrackBrowserActivity.this,
						MusicBrowserActivity.class);
				startActivity(intent);
			}
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService = null;
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		if (mTrackCursor != null) {
			getListView().invalidateViews();
		}
		MusicUtils.setSpinnerState(this);
	}

	@Override
	public void onPause() {
		mReScanHandler.removeCallbacksAndMessages(null);
		super.onPause();
	}

	/*
	 * This listener gets called when the media scanner starts up or finishes,
	 * and when the sd card is unmounted.
	 */
	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)
					|| Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
				MusicUtils.setSpinnerState(TrackBrowserActivity.this);
			}
			mReScanHandler.sendEmptyMessage(0);
		}
	};

	private Handler mReScanHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mAdapter != null) {
				getTrackCursor(mAdapter.getQueryHandler(), null, true);
			}
			// if the query results in a null cursor, onQueryComplete() will
			// call init(), which will post a delayed message to this handler
			// in order to try again.
		}
	};

	public void onSaveInstanceState(Bundle outcicle) {
		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		outcicle.putLong("selectedtrack", mSelectedId);
		outcicle.putString("artist", mArtistId);
		outcicle.putString("album", mAlbumId);
		outcicle.putString("playlist", mPlaylist);
		outcicle.putString("genre", mGenre);
		outcicle.putBoolean("editmode", mEditMode);
		super.onSaveInstanceState(outcicle);
	}

	public void init(Cursor newCursor, boolean isLimited) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(newCursor); // also sets mTrackCursor

		if (mTrackCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		MusicUtils.hideDatabaseError(this);
		mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
		setTitle();

		// Restore previous position
		if (mLastListPosCourse >= 0 && mUseLastListPos) {
			ListView lv = getListView();
			// this hack is needed because otherwise the position doesn't change
			// for the 2nd (non-limited) cursor
			lv.setAdapter(lv.getAdapter());
			lv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
			if (!isLimited) {
				mLastListPosCourse = -1;
			}
		}

		// When showing the queue, position the selection on the currently
		// playing track
		// Otherwise, position the selection on the first matching artist, if
		// any
		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.META_CHANGED);
		f.addAction(MediaPlaybackService.QUEUE_CHANGED);
		f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
		f.addAction(MediaPlaybackService.PROGRESSBAR_CHANGED);
		if ("nowplaying".equals(mPlaylist)) {
			try {
				int cur = MusicUtils.sService.getQueuePosition();
				setSelection(cur);
				registerReceiver(mNowPlayingListener, new IntentFilter(f));
				mNowPlayingListener.onReceive(this, new Intent(
						MediaPlaybackService.META_CHANGED));
			} catch (RemoteException ex) {
			}
		} else {
			String key = getIntent().getStringExtra("artist");
			if (key != null) {
				int keyidx = mTrackCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
				mTrackCursor.moveToFirst();
				while (!mTrackCursor.isAfterLast()) {
					String artist = mTrackCursor.getString(keyidx);
					if (artist.equals(key)) {
						setSelection(mTrackCursor.getPosition());
						break;
					}
					mTrackCursor.moveToNext();
				}
			}
			registerReceiver(mTrackListListener, new IntentFilter(f));
			mTrackListListener.onReceive(this, new Intent(
					MediaPlaybackService.META_CHANGED));
		}
	}

	private void setAlbumArtBackground() {
		if (!mEditMode) {
			try {
				long albumid = Long.valueOf(mAlbumId);
				Bitmap bm = MusicUtils.getArtwork(TrackBrowserActivity.this,
						-1, albumid, false);
				if (bm != null) {
					MusicUtils.setBackground(mTrackList, bm);
					mTrackList.setCacheColorHint(0);
					return;
				}
			} catch (Exception ex) {
			}
		}
		mTrackList.setCacheColorHint(0);
	}

	private void setTitle() {

		CharSequence fancyName = null;
		if (mAlbumId != null) {
			int numresults = mTrackCursor != null ? mTrackCursor.getCount() : 0;
			if (numresults > 0) {
				mTrackCursor.moveToFirst();
				int idx = mTrackCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
				fancyName = mTrackCursor.getString(idx);
				// For compilation albums show only the album title,
				// but for regular albums show "artist - album".
				// To determine whether something is a compilation
				// album, do a query for the artist + album of the
				// first item, and see if it returns the same number
				// of results as the album query.
				String where = MediaStore.Audio.Media.ALBUM_ID
						+ "='"
						+ mAlbumId
						+ "' AND "
						+ MediaStore.Audio.Media.ARTIST_ID
						+ "="
						+ mTrackCursor
								.getLong(mTrackCursor
										.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
				Cursor cursor = MusicUtils.query(this,
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Audio.Media.ALBUM }, where,
						null, null);
				if (cursor != null) {
					if (cursor.getCount() != numresults) {
						// compilation album
						fancyName = mTrackCursor.getString(idx);
					}
					cursor.deactivate();
				}
				if (fancyName == null
						|| fancyName.equals(MediaStore.UNKNOWN_STRING)) {
					fancyName = getString(R.string.unknown_album_name);
				}
			}
		} else if (mPlaylist != null) {
			if (mPlaylist.equals("nowplaying")) {
				if (MusicUtils.getCurrentShuffleMode() == MediaPlaybackService.SHUFFLE_AUTO) {
					fancyName = getText(R.string.party_shuffle);
				} else {
					fancyName = getText(R.string.nowplaying_title);
				}
			} else if (mPlaylist.equals("podcasts")) {
				fancyName = getText(R.string.podcasts_title);
			} else if (mPlaylist.equals("recentlyadded")) {
				fancyName = getText(R.string.recentlyadded_title);
			} else {
				String[] cols = new String[] { MediaStore.Audio.Playlists.NAME };
				Cursor cursor = MusicUtils.query(this, ContentUris
						.withAppendedId(Playlists.EXTERNAL_CONTENT_URI,
								Long.valueOf(mPlaylist)), cols, null, null,
						null);
				if (cursor != null) {
					if (cursor.getCount() != 0) {
						cursor.moveToFirst();
						fancyName = cursor.getString(0);
					}
					cursor.deactivate();
				}
			}
		} else if (mGenre != null) {
			String[] cols = new String[] { MediaStore.Audio.Genres.NAME };
			Cursor cursor = MusicUtils.query(this, ContentUris.withAppendedId(
					MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
					Long.valueOf(mGenre)), cols, null, null, null);
			if (cursor != null) {
				if (cursor.getCount() != 0) {
					cursor.moveToFirst();
					fancyName = cursor.getString(0);
				}
				cursor.deactivate();
			}
		}

		if (fancyName != null) {
			setTitle(fancyName);
		} else {
			try {

				setTitle(mService.getArtistName());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
		public void drop(int from, int to) {
			if (mTrackCursor instanceof NowPlayingCursor) {
				// update the currently playing list
				NowPlayingCursor c = (NowPlayingCursor) mTrackCursor;
				c.moveItem(from, to);
				((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
				getListView().invalidateViews();
				mDeletedOneRow = true;
			} else {
				// update a saved playlist
				Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri(
						"external", Long.valueOf(mPlaylist));
				ContentValues values = new ContentValues();
				String where = MediaStore.Audio.Playlists.Members._ID + "=?";
				String[] wherearg = new String[1];
				ContentResolver res = getContentResolver();
				int colidx = mTrackCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
				if (from < to) {
					// move the item to somewhere later in the list
					mTrackCursor.moveToPosition(to);
					long toidx = mTrackCursor.getLong(colidx);
					mTrackCursor.moveToPosition(from);
					values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							toidx);
					wherearg[0] = mTrackCursor.getString(0);
					res.update(baseUri, values, where, wherearg);
					for (int i = from + 1; i <= to; i++) {
						mTrackCursor.moveToPosition(i);
						values.put(
								MediaStore.Audio.Playlists.Members.PLAY_ORDER,
								i - 1);
						wherearg[0] = mTrackCursor.getString(0);
						res.update(baseUri, values, where, wherearg);
					}
				} else if (from > to) {
					// move the item to somewhere earlier in the list
					mTrackCursor.moveToPosition(to);
					long toidx = mTrackCursor.getLong(colidx);
					mTrackCursor.moveToPosition(from);
					values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							toidx);
					wherearg[0] = mTrackCursor.getString(0);
					res.update(baseUri, values, where, wherearg);
					for (int i = from - 1; i >= to; i--) {
						mTrackCursor.moveToPosition(i);
						values.put(
								MediaStore.Audio.Playlists.Members.PLAY_ORDER,
								i + 1);
						wherearg[0] = mTrackCursor.getString(0);
						res.update(baseUri, values, where, wherearg);
					}
				}

			}
		}
	};

	private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
		public void remove(int which) {
			removePlaylistItem(which);
		}
	};

	private void removePlaylistItem(int which) {
		View v = mTrackList.getChildAt(which
				- mTrackList.getFirstVisiblePosition());
		if (v == null) {
			Log.d(LOGTAG, "No view when removing playlist item " + which);
			return;
		}
		try {
			if (MusicUtils.sService != null
					&& which != MusicUtils.sService.getQueuePosition()) {
				mDeletedOneRow = true;
			}
		} catch (RemoteException e) {
			// Service died, so nothing playing.
			mDeletedOneRow = true;
		}
		v.setVisibility(View.GONE);
		mTrackList.invalidateViews();
		if (mTrackCursor instanceof NowPlayingCursor) {
			((NowPlayingCursor) mTrackCursor).removeItem(which);
		} else {
			int colidx = mTrackCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members._ID);
			mTrackCursor.moveToPosition(which);
			long id = mTrackCursor.getLong(colidx);
			Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
					"external", Long.valueOf(mPlaylist));
			getContentResolver().delete(ContentUris.withAppendedId(uri, id),
					null, null);
		}
		v.setVisibility(View.VISIBLE);
		mTrackList.invalidateViews();
	}

	private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			getListView().invalidateViews();
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				refreshProgress();
				if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
					setPauseButtonImage();
				}
				if (!mEditMode) {
					MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
				}
			}
		}
	};

	private BroadcastReceiver mNowPlayingListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MediaPlaybackService.META_CHANGED)) {
				getListView().invalidateViews();
			} else if (intent.getAction().equals(
					MediaPlaybackService.QUEUE_CHANGED)) {
				if (mDeletedOneRow) {
					// This is the notification for a single row that was
					// deleted previously, which is already reflected in
					// the UI.
					mDeletedOneRow = false;
					return;
				}
				// The service could disappear while the broadcast was in
				// flight,
				// so check to see if it's still valid
				if (MusicUtils.sService == null) {
					finish();
					return;
				}
				if (mAdapter != null) {
					Cursor c = new NowPlayingCursor(MusicUtils.sService,
							mCursorCols);
					if (c.getCount() == 0) {
						finish();
						return;
					}
					mAdapter.changeCursor(c);
				}
			}
		}
	};

	// Cursor should be positioned on the entry to be checked
	// Returns false if the entry matches the naming pattern used for
	// recordings,
	// or if it is marked as not music in the database.
	private boolean isMusic(Cursor c) {
		int titleidx = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
		int albumidx = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
		int artistidx = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);

		String title = c.getString(titleidx);
		String album = c.getString(albumidx);
		String artist = c.getString(artistidx);
		if (MediaStore.UNKNOWN_STRING.equals(album)
				&& MediaStore.UNKNOWN_STRING.equals(artist) && title != null
				&& title.startsWith("recording")) {
			// not music
			return false;
		}

		int ismusic_idx = c.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
		boolean ismusic = true;
		if (ismusic_idx >= 0) {
			ismusic = mTrackCursor.getInt(ismusic_idx) != 0;
		}
		return ismusic;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfoIn) {
		menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
		SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
				R.string.add_to_playlist);
		MusicUtils.makePlaylistMenu(this, sub);
		if (mEditMode) {
			menu.add(0, REMOVE, 0, R.string.remove_from_playlist);
		}
		menu.add(0, USE_AS_RINGTONE, 0, R.string.ringtone_menu);
		menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
		mSelectedPosition = mi.position;
		mTrackCursor.moveToPosition(mSelectedPosition);
		try {
			int id_idx = mTrackCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
			mSelectedId = mTrackCursor.getLong(id_idx);
		} catch (IllegalArgumentException ex) {
			mSelectedId = mi.id;
		}
		// only add the 'search' menu if the selected item is music
		if (isMusic(mTrackCursor)) {
			menu.add(0, SEARCH, 0, R.string.search_title);
		}
		mCurrentAlbumName = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
		mCurrentArtistNameForAlbum = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
		mCurrentTrackName = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
		menu.setHeaderTitle(mCurrentTrackName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PLAY_SELECTION: {
			// play the track
			int position = mSelectedPosition;
			MusicUtils.playAll(this, mTrackCursor, position);
			return true;
		}

		case QUEUE: {
			long[] list = new long[] { mSelectedId };
			MusicUtils.addToCurrentPlaylist(this, list);
			return true;
		}

		case NEW_PLAYLIST: {
			Intent intent = new Intent();
			intent.setClass(this, CreatePlaylist.class);
			startActivityForResult(intent, NEW_PLAYLIST);
			return true;
		}

		case PLAYLIST_SELECTED: {
			long[] list = new long[] { mSelectedId };
			long playlist = item.getIntent().getLongExtra("playlist", 0);
			MusicUtils.addToPlaylist(this, list, playlist);
			return true;
		}

		case USE_AS_RINGTONE:
			// Set the system setting to make this the current ringtone
			MusicUtils.setRingtone(this, mSelectedId);
			return true;

		case DELETE_ITEM: {
			long[] list = new long[1];
			list[0] = (int) mSelectedId;
			Bundle b = new Bundle();
			String f;
			if (android.os.Environment.isExternalStorageRemovable()) {
				f = getString(R.string.delete_song_desc);
			} else {
				f = getString(R.string.delete_song_desc_nosdcard);
			}
			String desc = String.format(f, mCurrentTrackName);
			b.putString("description", desc);
			b.putLongArray("items", list);
			Intent intent = new Intent();
			intent.setClass(this, DeleteItems.class);
			intent.putExtras(b);
			startActivityForResult(intent, -1);
			return true;
		}

		case REMOVE:
			removePlaylistItem(mSelectedPosition);
			return true;

		case SEARCH:
			doSearch();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	void doSearch() {
		CharSequence title = null;
		String query = null;

		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		title = mCurrentTrackName;
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentArtistNameForAlbum)) {
			query = mCurrentTrackName;
		} else {
			query = mCurrentArtistNameForAlbum + " " + mCurrentTrackName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST,
					mCurrentArtistNameForAlbum);
		}
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentAlbumName)) {
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
		}
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	// In order to use alt-up/down as a shortcut for moving the selected item
	// in the list, we need to override dispatchKeyEvent, not onKeyDown.
	// (onKeyDown never sees these events, since they are handled by the list)
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mPlaylist != null && event.getMetaState() != 0
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_UP:
				moveItem(true);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				moveItem(false);
				return true;
			case KeyEvent.KEYCODE_DEL:
				removeItem();
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	private void removeItem() {
		int curcount = mTrackCursor.getCount();
		int curpos = mTrackList.getSelectedItemPosition();
		if (curcount == 0 || curpos < 0) {
			return;
		}

		if ("nowplaying".equals(mPlaylist)) {
			// remove track from queue

			// Work around bug 902971. To get quick visual feedback
			// of the deletion of the item, hide the selected view.
			try {
				if (curpos != MusicUtils.sService.getQueuePosition()) {
					mDeletedOneRow = true;
				}
			} catch (RemoteException ex) {
			}
			View v = mTrackList.getSelectedView();
			v.setVisibility(View.GONE);
			mTrackList.invalidateViews();
			((NowPlayingCursor) mTrackCursor).removeItem(curpos);
			v.setVisibility(View.VISIBLE);
			mTrackList.invalidateViews();
		} else {
			// remove track from playlist
			int colidx = mTrackCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members._ID);
			mTrackCursor.moveToPosition(curpos);
			long id = mTrackCursor.getLong(colidx);
			Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
					"external", Long.valueOf(mPlaylist));
			getContentResolver().delete(ContentUris.withAppendedId(uri, id),
					null, null);
			curcount--;
			if (curcount == 0) {
				finish();
			} else {
				mTrackList.setSelection(curpos < curcount ? curpos : curcount);
			}
		}
	}

	private void moveItem(boolean up) {
		int curcount = mTrackCursor.getCount();
		int curpos = mTrackList.getSelectedItemPosition();
		if ((up && curpos < 1) || (!up && curpos >= curcount - 1)) {
			return;
		}

		if (mTrackCursor instanceof NowPlayingCursor) {
			NowPlayingCursor c = (NowPlayingCursor) mTrackCursor;
			c.moveItem(curpos, up ? curpos - 1 : curpos + 1);
			((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
			getListView().invalidateViews();
			mDeletedOneRow = true;
			if (up) {
				mTrackList.setSelection(curpos - 1);
			} else {
				mTrackList.setSelection(curpos + 1);
			}
		} else {
			int colidx = mTrackCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
			mTrackCursor.moveToPosition(curpos);
			int currentplayidx = mTrackCursor.getInt(colidx);
			Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri(
					"external", Long.valueOf(mPlaylist));
			ContentValues values = new ContentValues();
			String where = MediaStore.Audio.Playlists.Members._ID + "=?";
			String[] wherearg = new String[1];
			ContentResolver res = getContentResolver();
			if (up) {
				values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
						currentplayidx - 1);
				wherearg[0] = mTrackCursor.getString(0);
				res.update(baseUri, values, where, wherearg);
				mTrackCursor.moveToPrevious();
			} else {
				values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
						currentplayidx + 1);
				wherearg[0] = mTrackCursor.getString(0);
				res.update(baseUri, values, where, wherearg);
				mTrackCursor.moveToNext();
			}
			values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
					currentplayidx);
			wherearg[0] = mTrackCursor.getString(0);
			res.update(baseUri, values, where, wherearg);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (mTrackCursor.getCount() == 0) {
			return;
		}
		// When selecting a track from the queue, just jump there instead of
		// reloading the queue. This is both faster, and prevents accidentally
		// dropping out of party shuffle.
		if (mTrackCursor instanceof NowPlayingCursor) {
			if (MusicUtils.sService != null) {
				try {
					MusicUtils.sService.setQueuePosition(position);
					return;
				} catch (RemoteException ex) {
				}
			}
		}
		MusicUtils.playAll(this, mTrackCursor, position);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			setPauseButtonImage();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * This activity is used for a number of different browsing modes, and
		 * the menu can be different for each of them: - all tracks, optionally
		 * restricted to an album, artist or playlist - the list of currently
		 * playing songs
		 */
		super.onCreateOptionsMenu(menu);
		if (mPlaylist == null) {
			menu.add(0, PLAY_ALL, 0, R.string.play_all);
		}
		menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
		menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all);
		menu.add(0, SETTINGS, 0, R.string.settings);

		if (mPlaylist != null) {
			menu.add(0, SAVE_AS_PLAYLIST, 0, R.string.save_as_playlist)
					.setIcon(android.R.drawable.ic_menu_save);
			if (mPlaylist.equals("nowplaying")) {
				menu.add(0, CLEAR_PLAYLIST, 0, R.string.clear_playlist);
			}
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MusicUtils.setPartyShuffleMenuIcon(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		Cursor cursor;
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		case PLAY_ALL: {
			MusicUtils.playAll(this, mTrackCursor);
			return true;
		}

		case PARTY_SHUFFLE:
			MusicUtils.togglePartyShuffle();
			break;

		case SHUFFLE_ALL:
			// Should 'shuffle all' shuffle ALL, or only the tracks shown?
			cursor = MusicUtils.query(this,
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[] { MediaStore.Audio.Media._ID },
					MediaStore.Audio.Media.IS_MUSIC + "=1", null,
					MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
			if (cursor != null) {
				MusicUtils.shuffleAll(this, cursor);
				cursor.close();
			}
			return true;

		case SAVE_AS_PLAYLIST:
			intent = new Intent();
			intent.setClass(this, CreatePlaylist.class);
			startActivityForResult(intent, SAVE_AS_PLAYLIST);
			return true;

		case CLEAR_PLAYLIST:
			// We only clear the current playlist
			MusicUtils.clearQueue();
			return true;
		case SETTINGS:
			intent = new Intent();
			intent.setClass(this, MusicSettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case SCAN_DONE:
			if (resultCode == RESULT_CANCELED) {
				finish();
			} else {
				getTrackCursor(mAdapter.getQueryHandler(), null, true);
			}
			break;

		case NEW_PLAYLIST:
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					long[] list = new long[] { mSelectedId };
					MusicUtils.addToPlaylist(this, list,
							Integer.valueOf(uri.getLastPathSegment()));
				}
			}
			break;

		case SAVE_AS_PLAYLIST:
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					long[] list = MusicUtils.getSongListForCursor(mTrackCursor);
					int plid = Integer.parseInt(uri.getLastPathSegment());
					MusicUtils.addToPlaylist(this, list, plid);
				}
			}
			break;
		}
	}

	private Cursor getTrackCursor(
			TrackListAdapter.TrackQueryHandler queryhandler, String filter,
			boolean async) {

		if (queryhandler == null) {
			throw new IllegalArgumentException();
		}

		Cursor ret = null;
		mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Media.TITLE + " != ''");

		if (mGenre != null) {
			Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external",
					Integer.valueOf(mGenre));
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon()
						.appendQueryParameter("filter", Uri.encode(filter))
						.build();
			}
			mSortOrder = MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER;
			ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
					null, mSortOrder, async);
		} else if (mPlaylist != null) {
			if (mPlaylist.equals("nowplaying")) {
				if (MusicUtils.sService != null) {
					ret = new NowPlayingCursor(MusicUtils.sService, mCursorCols);
					if (ret.getCount() == 0) {
						finish();
					}
				} else {
					// Nothing is playing.
				}
			} else if (mPlaylist.equals("podcasts")) {
				where.append(" AND " + MediaStore.Audio.Media.IS_PODCAST + "=1");
				Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
						null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
			} else if (mPlaylist.equals("recentlyadded")) {
				// do a query for all songs added in the last X weeks
				Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				int X = MusicUtils.getIntPref(this, "numweeks", 2)
						* (3600 * 24 * 7);
				where.append(" AND " + MediaStore.MediaColumns.DATE_ADDED + ">");
				where.append(System.currentTimeMillis() / 1000 - X);
				ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
						null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
			} else {
				Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
						"external", Long.valueOf(mPlaylist));
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				mSortOrder = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER;
				ret = queryhandler.doQuery(uri, mPlaylistMemberCols,
						where.toString(), null, mSortOrder, async);
			}
		} else {
			if (mAlbumId != null) {
				where.append(" AND " + MediaStore.Audio.Media.ALBUM_ID + "="
						+ mAlbumId);
				mSortOrder = MediaStore.Audio.Media.TRACK + ", " + mSortOrder;
			}
			if (mArtistId != null) {
				where.append(" AND " + MediaStore.Audio.Media.ARTIST_ID + "="
						+ mArtistId);
			}
			where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon()
						.appendQueryParameter("filter", Uri.encode(filter))
						.build();
			}
			ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
					null, mSortOrder, async);
		}

		// This special case is for the "nowplaying" cursor, which cannot be
		// handled
		// asynchronously using AsyncQueryHandler, so we do some extra
		// initialization here.
		if (ret != null && async) {
			init(ret, false);
			setTitle();
		}
		return ret;
	}

	private class NowPlayingCursor extends AbstractCursor {
		public NowPlayingCursor(IMediaPlaybackService service, String[] cols) {
			mCols = cols;
			mService = service;
			makeNowPlayingCursor();
		}

		private void makeNowPlayingCursor() {
			mCurrentPlaylistCursor = null;
			try {
				mNowPlaying = mService.getQueue();
			} catch (RemoteException ex) {
				mNowPlaying = new long[0];
			}
			mSize = mNowPlaying.length;
			if (mSize == 0) {
				return;
			}

			StringBuilder where = new StringBuilder();
			where.append(MediaStore.Audio.Media._ID + " IN (");
			for (int i = 0; i < mSize; i++) {
				where.append(mNowPlaying[i]);
				if (i < mSize - 1) {
					where.append(",");
				}
			}
			where.append(")");

			mCurrentPlaylistCursor = MusicUtils.query(
					TrackBrowserActivity.this,
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCols,
					where.toString(), null, MediaStore.Audio.Media._ID);

			if (mCurrentPlaylistCursor == null) {
				mSize = 0;
				return;
			}

			int size = mCurrentPlaylistCursor.getCount();
			mCursorIdxs = new long[size];
			mCurrentPlaylistCursor.moveToFirst();
			int colidx = mCurrentPlaylistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
			for (int i = 0; i < size; i++) {
				mCursorIdxs[i] = mCurrentPlaylistCursor.getLong(colidx);
				mCurrentPlaylistCursor.moveToNext();
			}
			mCurrentPlaylistCursor.moveToFirst();
			mCurPos = -1;

			// At this point we can verify the 'now playing' list we got
			// earlier to make sure that all the items in there still exist
			// in the database, and remove those that aren't. This way we
			// don't get any blank items in the list.
			try {
				int removed = 0;
				for (int i = mNowPlaying.length - 1; i >= 0; i--) {
					long trackid = mNowPlaying[i];
					int crsridx = Arrays.binarySearch(mCursorIdxs, trackid);
					if (crsridx < 0) {
						// Log.i("@@@@@", "item no longer exists in db: " +
						// trackid);
						removed += mService.removeTrack(trackid);
					}
				}
				if (removed > 0) {
					mNowPlaying = mService.getQueue();
					mSize = mNowPlaying.length;
					if (mSize == 0) {
						mCursorIdxs = null;
						return;
					}
				}
			} catch (RemoteException ex) {
				mNowPlaying = new long[0];
			}
		}

		@Override
		public int getCount() {
			return mSize;
		}

		@Override
		public boolean onMove(int oldPosition, int newPosition) {
			if (oldPosition == newPosition)
				return true;

			if (mNowPlaying == null || mCursorIdxs == null
					|| newPosition >= mNowPlaying.length) {
				return false;
			}

			// The cursor doesn't have any duplicates in it, and is not ordered
			// in queue-order, so we need to figure out where in the cursor we
			// should be.

			long newid = mNowPlaying[newPosition];
			int crsridx = Arrays.binarySearch(mCursorIdxs, newid);
			mCurrentPlaylistCursor.moveToPosition(crsridx);
			mCurPos = newPosition;

			return true;
		}

		public boolean removeItem(int which) {
			try {
				if (mService.removeTracks(which, which) == 0) {
					return false; // delete failed
				}
				int i = (int) which;
				mSize--;
				while (i < mSize) {
					mNowPlaying[i] = mNowPlaying[i + 1];
					i++;
				}
				onMove(-1, (int) mCurPos);
			} catch (RemoteException ex) {
			}
			return true;
		}

		public void moveItem(int from, int to) {
			try {
				mService.moveQueueItem(from, to);
				mNowPlaying = mService.getQueue();
				onMove(-1, mCurPos); // update the underlying cursor
			} catch (RemoteException ex) {
			}
		}

		private void dump() {
			String where = "(";
			for (int i = 0; i < mSize; i++) {
				where += mNowPlaying[i];
				if (i < mSize - 1) {
					where += ",";
				}
			}
			where += ")";
			Log.i("NowPlayingCursor: ", where);
		}

		@Override
		public String getString(int column) {
			try {
				return mCurrentPlaylistCursor.getString(column);
			} catch (Exception ex) {
				onChange(true);
				return "";
			}
		}

		@Override
		public short getShort(int column) {
			return mCurrentPlaylistCursor.getShort(column);
		}

		@Override
		public int getInt(int column) {
			try {
				return mCurrentPlaylistCursor.getInt(column);
			} catch (Exception ex) {
				onChange(true);
				return 0;
			}
		}

		@Override
		public long getLong(int column) {
			try {
				return mCurrentPlaylistCursor.getLong(column);
			} catch (Exception ex) {
				onChange(true);
				return 0;
			}
		}

		@Override
		public float getFloat(int column) {
			return mCurrentPlaylistCursor.getFloat(column);
		}

		@Override
		public double getDouble(int column) {
			return mCurrentPlaylistCursor.getDouble(column);
		}

		@Override
		public int getType(int column) {
			return mCurrentPlaylistCursor.getType(column);
		}

		@Override
		public boolean isNull(int column) {
			return mCurrentPlaylistCursor.isNull(column);
		}

		@Override
		public String[] getColumnNames() {
			return mCols;
		}

		@Override
		public void deactivate() {
			if (mCurrentPlaylistCursor != null)
				mCurrentPlaylistCursor.deactivate();
		}

		@Override
		public boolean requery() {
			makeNowPlayingCursor();
			return true;
		}

		private String[] mCols;
		private Cursor mCurrentPlaylistCursor; // updated in onMove
		private int mSize; // size of the queue
		private long[] mNowPlaying;
		private long[] mCursorIdxs;
		private int mCurPos;
		private IMediaPlaybackService mService;
	}

	static class TrackListAdapter extends SimpleCursorAdapter implements
			SectionIndexer {
		boolean mIsNowPlaying;
		boolean mDisableNowPlayingIndicator;

		int mTitleIdx;
		int mArtistIdx;
		int mDurationIdx;
		int mAudioIdIdx;
		int mAlbumIdx;

		private final StringBuilder mBuilder = new StringBuilder();
		private final String mUnknownArtist;
		private final String mUnknownAlbum;

		private AlphabetIndexer mIndexer;

		private TrackBrowserActivity mActivity = null;
		private TrackQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			TextView duration;
			ImageView play_indicator;
			CharArrayBuffer buffer1;
			char[] buffer2;
			ImageView mCM;
			public FrameLayout mContextMenu;
		}

		class TrackQueryHandler extends AsyncQueryHandler {

			class QueryArgs {
				public Uri uri;
				public String[] projection;
				public String selection;
				public String[] selectionArgs;
				public String orderBy;
			}

			TrackQueryHandler(ContentResolver res) {
				super(res);
			}

			public Cursor doQuery(Uri uri, String[] projection,
					String selection, String[] selectionArgs, String orderBy,
					boolean async) {
				if (async) {
					// Get 100 results first, which is enough to allow the user
					// to start scrolling,
					// while still being very fast.
					Uri limituri = uri.buildUpon()
							.appendQueryParameter("limit", "100").build();
					QueryArgs args = new QueryArgs();
					args.uri = uri;
					args.projection = projection;
					args.selection = selection;
					args.selectionArgs = selectionArgs;
					args.orderBy = orderBy;

					startQuery(0, args, limituri, projection, selection,
							selectionArgs, orderBy);
					return null;
				}
				return MusicUtils.query(mActivity, uri, projection, selection,
						selectionArgs, orderBy);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// Log.i("@@@", "query complete: " + cursor.getCount() + "   " +
				// mActivity);
				mActivity.init(cursor, cookie != null);
				if (token == 0 && cookie != null && cursor != null
						&& cursor.getCount() >= 100) {
					QueryArgs args = (QueryArgs) cookie;
					startQuery(1, null, args.uri, args.projection,
							args.selection, args.selectionArgs, args.orderBy);
				}
			}
		}

		TrackListAdapter(Context context, TrackBrowserActivity currentactivity,
				int layout, Cursor cursor, String[] from, int[] to,
				boolean isnowplaying, boolean disablenowplayingindicator) {
			super(context, layout, cursor, from, to);
			mActivity = currentactivity;
			getColumnIndices(cursor);
			mIsNowPlaying = isnowplaying;
			mDisableNowPlayingIndicator = disablenowplayingindicator;
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
			mUnknownAlbum = context.getString(R.string.unknown_album_name);

			mQueryHandler = new TrackQueryHandler(context.getContentResolver());
		}

		public void setActivity(TrackBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public TrackQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mTitleIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				mArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				mDurationIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
				mAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);

				try {
					mAudioIdIdx = cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
				} catch (IllegalArgumentException ex) {
					mAudioIdIdx = cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				}

				if (mIndexer != null) {
					mIndexer.setCursor(cursor);
				} else if (!mActivity.mEditMode && mActivity.mAlbumId == null) {
					String alpha = mActivity
							.getString(R.string.fast_scroll_alphabet);

					mIndexer = new MusicAlphabetIndexer(cursor, mTitleIdx,
							alpha);
				}
			}
		}

		private View.OnClickListener mCML = new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();

			}
		};

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			iv.setVisibility(View.GONE);

			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.duration = (TextView) v.findViewById(R.id.duration);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.buffer1 = new CharArrayBuffer(100);
			vh.buffer2 = new char[200];
			v.setTag(vh);
			vh.mCM = (ImageView) v.findViewById(R.id.CM);
			vh.mContextMenu = (FrameLayout) v
					.findViewById(R.id.second_column_icon);
			vh.mContextMenu.setOnClickListener(mCML);
			// ADW: Load the specified theme
			String themePackage = MusicUtils.getThemePackageName(context,
					MusicSettingsActivity.THEME_DEFAULT);
			PackageManager pm = context.getPackageManager();
			Resources themeResources = null;
			if (!themePackage.equals(MusicSettingsActivity.THEME_DEFAULT)) {
				try {
					themeResources = pm
							.getResourcesForApplication(themePackage);
				} catch (NameNotFoundException e) {
					// ADW The saved theme was uninstalled so we save the
					// default one
					MusicUtils.setThemePackageName(context,
							MusicSettingsActivity.THEME_DEFAULT);
				}
			}
			if (themeResources != null) {
				int line1 = themeResources.getIdentifier(
						"song_tab_track_name_color", "color", themePackage);
				if (line1 != 0) {
					vh.line1.setTextColor(themeResources.getColor(line1));
				}
				int line2 = themeResources.getIdentifier(
						"song_tab_album_name_color", "color", themePackage);
				if (line2 != 0) {
					vh.line2.setTextColor(themeResources.getColor(line2));
				}
				int duration = themeResources.getIdentifier(
						"song_tab_duration_color", "color", themePackage);
				if (duration != 0) {
					vh.duration.setTextColor(themeResources.getColor(duration));
				}
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "bt_context_menu", vh.mCM,
						THEME_ITEM_FOREGROUND);
			}
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// ADW: Load the specified theme
			String themePackage = MusicUtils.getThemePackageName(context,
					MusicSettingsActivity.THEME_DEFAULT);
			PackageManager pm = context.getPackageManager();
			Resources themeResources = null;
			if (!themePackage.equals(MusicSettingsActivity.THEME_DEFAULT)) {
				try {
					themeResources = pm
							.getResourcesForApplication(themePackage);
				} catch (NameNotFoundException e) {
					// ADW The saved theme was uninstalled so we save the
					// default one
					MusicUtils.setThemePackageName(context,
							MusicSettingsActivity.THEME_DEFAULT);
				}
			}

			ViewHolder vh = (ViewHolder) view.getTag();

			cursor.copyStringToBuffer(mTitleIdx, vh.buffer1);
			vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);

			int secs = cursor.getInt(mDurationIdx) / 1000;
			if (secs == 0) {
				vh.duration.setText("");
			} else {
				vh.duration.setText(MusicUtils.makeTimeString(context, secs));
			}

			final StringBuilder builder = mBuilder;
			builder.delete(0, builder.length());

			String name = cursor.getString(mAlbumIdx);
			if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
				builder.append(mUnknownArtist);
			} else {
				builder.append(name);
			}
			int len = builder.length();
			if (vh.buffer2.length < len) {
				vh.buffer2 = new char[len];
			}
			builder.getChars(0, len, vh.buffer2, 0);
			vh.line2.setText(vh.buffer2, 0, len);

			ImageView iv = vh.play_indicator;
			long id = -1;
			if (MusicUtils.sService != null) {
				// TODO: IPC call on each bind??
				try {
					if (mIsNowPlaying) {
						id = MusicUtils.sService.getQueuePosition();
					} else {
						id = MusicUtils.sService.getAudioId();
					}
				} catch (RemoteException ex) {
				}
			}

			// Determining whether and where to show the "now playing indicator
			// is tricky, because we don't actually keep track of where the
			// songs
			// in the current playlist came from after they've started playing.
			//
			// If the "current playlists" is shown, then we can simply match by
			// position,
			// otherwise, we need to match by id. Match-by-id gets a little
			// weird if
			// a song appears in a playlist more than once, and you're in
			// edit-playlist
			// mode. In that case, both items will have the "now playing"
			// indicator.
			// For this reason, we don't show the play indicator at all when in
			// edit
			// playlist mode (except when you're viewing the "current playlist",
			// which is not really a playlist)
			if ((mIsNowPlaying && cursor.getPosition() == id)
					|| (!mIsNowPlaying && !mDisableNowPlayingIndicator && cursor
							.getLong(mAudioIdIdx) == id)) {
				iv.setBackgroundResource(R.anim.peak_meter);
				AnimationDrawable frameAnimation = (AnimationDrawable) iv
						.getBackground();
				if (themeResources != null) {
					int peak = themeResources.getIdentifier("peak_meter",
							"anim", themePackage);
					if (peak != 0) {
						iv.setBackgroundDrawable(themeResources
								.getDrawable(peak));
						frameAnimation.start();
					}
				}
				// Start the animation (looped playback by default).
				frameAnimation.start();
				iv.setVisibility(View.VISIBLE);
			} else {
				iv.setVisibility(View.GONE);
			}
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mTrackCursor) {
				mActivity.mTrackCursor = cursor;
				super.changeCursor(cursor);
				getColumnIndices(cursor);
			}
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String s = constraint.toString();
			if (mConstraintIsValid
					&& ((s == null && mConstraint == null) || (s != null && s
							.equals(mConstraint)))) {
				return getCursor();
			}
			Cursor c = mActivity.getTrackCursor(mQueryHandler, s, false);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}

		// SectionIndexer methods

		public Object[] getSections() {
			if (mIndexer != null) {
				return mIndexer.getSections();
			} else {
				return new String[] { " " };
			}
		}

		public int getPositionForSection(int section) {
			if (mIndexer != null) {
				return mIndexer.getPositionForSection(section);
			}
			return 0;
		}

		public int getSectionForPosition(int position) {
			return 0;
		}
	}

	// Methods for media control
	private void doPauseResume() {
		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					setPauseButtonImage();
				}
			}
		} catch (RemoteException ex) {
		}
	}

	private void setPauseButtonImage() {
		// ADW: Load the specified theme
		String themePackage = MusicUtils.getThemePackageName(this,
				MusicSettingsActivity.THEME_DEFAULT);
		PackageManager pm = getPackageManager();
		Resources themeResources = null;
		if (!themePackage.equals(MusicSettingsActivity.THEME_DEFAULT)) {
			try {
				themeResources = pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// ADW The saved theme was uninstalled so we save the
				// default one
				MusicUtils.setThemePackageName(this,
						MusicSettingsActivity.THEME_DEFAULT);
			}
		}
		try {
			if (mService != null && mService.isPlaying()) {
				mPlay.setImageResource(R.drawable.ic_media_pause);
				ArtistAlbumBrowserActivity
						.loadThemeResource(themeResources, themePackage,
								"snp_pause", mPlay, THEME_ITEM_FOREGROUND);
			} else {
				mPlay.setImageResource(R.drawable.ic_appwidget_music_play);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "snp_play", mPlay, THEME_ITEM_FOREGROUND);
			}
		} catch (RemoteException ex) {
		}
	}

	private void refreshProgress() {
		ProgressBar mProgress = (ProgressBar) findViewById(R.id.progress);
		mProgress.setMax(1000);
		try {
			if ((MusicUtils.sService.position() >= 0)
					&& (MusicUtils.sService.duration() > 0)) {
				mProgress.setProgress((int) (1000 * MusicUtils.sService
						.position() / MusicUtils.sService.duration()));
			} else {
				mProgress.setProgress(1000);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void doPrev() {
		if (mService == null)
			return;
		try {
			if (mService.position() < 2000) {
				mService.prev();
			} else {
				mService.seek(0);
				mService.play();
			}
			setPauseButtonImage();
		} catch (RemoteException ex) {
		}
	}

	private void doNext() {
		if (mService == null)
			return;
		try {
			mService.next();
			setPauseButtonImage();
		} catch (RemoteException ex) {
		}
	}

	private void playRecentlyAdded() {
		// do a query for all songs added in the last X weeks
		int X = MusicUtils.getIntPref(this, "numweeks", 2) * (3600 * 24 * 7);
		final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
		String where = MediaStore.MediaColumns.DATE_ADDED + ">"
				+ (System.currentTimeMillis() / 1000 - X);
		Cursor cursor = MusicUtils.query(this,
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where,
				null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		if (cursor == null) {
			// Todo: show a message
			return;
		}
		try {
			int len = cursor.getCount();
			long[] list = new long[len];
			for (int i = 0; i < len; i++) {
				cursor.moveToNext();
				list[i] = cursor.getLong(0);
			}
			MusicUtils.playAll(this, list, 0);
		} catch (SQLiteException ex) {
		} finally {
			cursor.close();
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			back_button_db = mPreferences.getString("back_button_db", "0");
			if (back_button_db.equals("0")) {
				MusicUtils.togglePartyShuffle();
				return true;
			}
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				back_button_db = mPreferences.getString("back_button_db", "1");
				if (back_button_db.equals("1")) {
					playRecentlyAdded();
					return true;
				}

			}
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				back_button_db = mPreferences.getString("back_button_db", "2");
				if (back_button_db.equals("2")) {
					Cursor cursor;
					cursor = MusicUtils.query(this,
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							new String[] { BaseColumns._ID },
							AudioColumns.IS_MUSIC + "=1", null,
							MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
					if (cursor != null) {
						MusicUtils.shuffleAll(this, cursor);
						cursor.close();
						return true;
					}
				}
			}
		}
		return super.onKeyLongPress(keyCode, event);

	}
}
