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

import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.music.MusicUtils.ServiceToken;

public class ArtistAlbumBrowserActivity extends ExpandableListActivity
		implements View.OnCreateContextMenuListener, MusicUtils.Defs,
		ServiceConnection {
	private String mCurrentArtistId;
	private String mCurrentArtistName;
	private String mCurrentAlbumId;
	private String mCurrentAlbumName;
	private String mCurrentArtistNameForAlbum;
	boolean mIsUnknownArtist;
	boolean mIsUnknownAlbum;
	private ArtistAlbumListAdapter mAdapter;
	private boolean mAdapterSent;
	private final static int SEARCH = CHILD_MENU_BASE;
	private static int mLastListPosCourse = -1;
	private static int mLastListPosFine = -1;
	private ServiceToken mToken;
	private IMediaPlaybackService mService = null;
	private SharedPreferences mPreferences;
	// Artist tab layout
	public LinearLayout mArtistTab;
	public TabWidget mButtonBar;
	public TextView mButtonBarArtist;
	public TextView mButtonBarAlbum;
	public TextView mButtonBarSong;
	public TextView mButtonBarPlaylist;
	public TextView mButtonBarNP;
	public RelativeLayout mGroup;
	public RelativeLayout mChild;
	public AnimationDrawable mPeak;
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
	private static ImageButton mShare;
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mPreferences = getSharedPreferences(
				MusicSettingsActivity.PREFERENCES_FILE, MODE_PRIVATE);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if (icicle != null) {
			mCurrentAlbumId = icicle.getString("selectedalbum");
			mCurrentAlbumName = icicle.getString("selectedalbumname");
			mCurrentArtistId = icicle.getString("selectedartist");
			mCurrentArtistName = icicle.getString("selectedartistname");
		}
		mToken = MusicUtils.bindToService(this, osc);

		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);

		setContentView(R.layout.media_picker_activity_expanding);
		MusicUtils.updateButtonBar(this, R.id.artisttab);

		// Swipe up gesture
		gestureDetector = new GestureDetector(new NowPlayingGesture());
		View nowplayingview = (View) findViewById(R.id.nowplaying);
		// Tab swipe
		swipetabs = new GestureDetector(new TabSwipe());
		View mainview = (View) findViewById(android.R.id.list);

		// Set the touch listener for the main view to be our custom gesture
		// listener
		mainview.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (swipetabs.onTouchEvent(event)) {
					return false;
				}
				return false;
			}
		});

		ExpandableListView lv = getExpandableListView();
		lv.setOnCreateContextMenuListener(this);
		lv.setTextFilterEnabled(true);

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
			mArtistTab = (LinearLayout) findViewById(R.id.artist_tab);
			mButtonBar = (TabWidget) findViewById(R.id.buttonbar);
			mButtonBarArtist = (TextView) findViewById(R.id.artisttab);
			mButtonBarAlbum = (TextView) findViewById(R.id.albumtab);
			mButtonBarSong = (TextView) findViewById(R.id.songtab);
			mButtonBarPlaylist = (TextView) findViewById(R.id.playlisttab);
			mButtonBarNP = (TextView) findViewById(R.id.nowplayingtab);
			loadThemeResource(themeResources, themePackage, "tab_artist",
					mArtistTab, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "buttonbar",
					mButtonBar, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "tab_bg_artist",
					mButtonBarArtist, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "tab_bg_album",
					mButtonBarAlbum, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "tab_bg_song",
					mButtonBarSong, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage, "tab_bg_playlist",
					mButtonBarPlaylist, THEME_ITEM_BACKGROUND);
			loadThemeResource(themeResources, themePackage,
					"tab_bg_nowplaying", mButtonBarNP, THEME_ITEM_BACKGROUND);

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
				loadThemeResource(themeResources, themePackage,
						"snp_background", mNowPlaying, THEME_ITEM_BACKGROUND);
				loadThemeResource(themeResources, themePackage, "snp_progress",
						mProgress, THEME_ITEM_BACKGROUND);
				loadThemeResource(themeResources, themePackage,
						"snp_info_divider", mInfoDivider, THEME_ITEM_BACKGROUND);
				loadThemeResource(themeResources, themePackage, "snp_share",
						mShare, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_play",
						mPlay, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_next",
						mNext, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_prev",
						mPrev, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_market",
						mMarket, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_search",
						mDoSearch, THEME_ITEM_FOREGROUND);
				loadThemeResource(themeResources, themePackage, "snp_flow",
						mFlow, THEME_ITEM_FOREGROUND);
			}
		}
		mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
		if (mAdapter == null) {
			// Log.i("@@@", "starting query");
			mAdapter = new ArtistAlbumListAdapter(getApplication(),
					this,
					null, // cursor
					R.layout.track_list_item_group, new String[] {},
					new int[] {}, R.layout.track_list_item_child,
					new String[] {}, new int[] {});
			setListAdapter(mAdapter);
			setTitle(R.string.working_artists);
			getArtistCursor(mAdapter.getQueryHandler(), null);

		} else {
			mAdapter.setActivity(this);
			setListAdapter(mAdapter);
			mArtistCursor = mAdapter.getCursor();
			if (mArtistCursor != null) {
				init(mArtistCursor);
			} else {
				getArtistCursor(mAdapter.getQueryHandler(), null);
			}
		}
	}

	public static void loadThemeResource(Resources themeResources,
			String themePackage, String item_name, View item, int themeType) {
		Drawable d = null;
		if (themeResources != null) {
			int resource_id = themeResources.getIdentifier(item_name,
					"drawable", themePackage);
			if (resource_id != 0) {
				try {
					d = themeResources.getDrawable(resource_id);
				} catch (Resources.NotFoundException e) {
					return;
				}
				if (themeType == THEME_ITEM_FOREGROUND
						&& item instanceof ImageView) {
					// ADW remove the old drawable
					Drawable tmp = ((ImageView) item).getDrawable();
					if (tmp != null) {
						tmp.setCallback(null);
						tmp = null;
					}
					((ImageView) item).setImageDrawable(d);
				} else {
					// ADW remove the old drawable
					Drawable tmp = item.getBackground();
					if (tmp != null) {
						tmp.setCallback(null);
						tmp = null;
					}
					item.setBackgroundDrawable(d);
				}
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		mAdapterSent = true;
		return mAdapter;
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {
		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		outcicle.putString("selectedalbum", mCurrentAlbumId);
		outcicle.putString("selectedalbumname", mCurrentAlbumName);
		outcicle.putString("selectedartist", mCurrentArtistId);
		outcicle.putString("selectedartistname", mCurrentArtistName);
		super.onSaveInstanceState(outcicle);
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
							"vnd.android.cursor.dir/album");
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					ArtistAlbumBrowserActivity.this.overridePendingTransition(
							R.anim.slide_in_right, R.anim.slide_out_left);
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					intent.setDataAndType(Uri.EMPTY,
							MediaStore.Audio.Playlists.CONTENT_TYPE);
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					ArtistAlbumBrowserActivity.this.overridePendingTransition(
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
		ExpandableListView lv = getExpandableListView();
		if (lv != null) {
			mLastListPosCourse = lv.getFirstVisiblePosition();
			View cv = lv.getChildAt(0);
			if (cv != null) {
				mLastListPosFine = cv.getTop();
			}
		}

		MusicUtils.unbindFromService(mToken);
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
		unregisterReceiver(mScanListener);
		setListAdapter(null);
		super.onDestroy();
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
				MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);
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
				intent.setClass(ArtistAlbumBrowserActivity.this,
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
		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.META_CHANGED);
		f.addAction(MediaPlaybackService.QUEUE_CHANGED);
		f.addAction(MediaPlaybackService.PROGRESSBAR_CHANGED);
		f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
		registerReceiver(mTrackListListener, f);
		MusicUtils.setSpinnerState(this);
	}

	private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			getExpandableListView().invalidateViews();
			MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				refreshProgress();
				if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
					setPauseButtonImage();
				}
			}
		}
	};
	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
			mReScanHandler.sendEmptyMessage(0);
			if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				MusicUtils.clearAlbumArtCache();
			}
		}
	};

	private Handler mReScanHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mAdapter != null) {
				getArtistCursor(mAdapter.getQueryHandler(), null);
			}
		}
	};

	@Override
	public void onPause() {
		unregisterReceiver(mTrackListListener);
		mReScanHandler.removeCallbacksAndMessages(null);

		super.onPause();
	}

	public void init(Cursor c) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(c); // also sets mArtistCursor

		if (mArtistCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		// restore previous position
		if (mLastListPosCourse >= 0) {
			ExpandableListView elv = getExpandableListView();
			elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
			mLastListPosCourse = -1;
		}

		MusicUtils.hideDatabaseError(this);
		MusicUtils.updateButtonBar(this, R.id.artisttab);
		setTitle();
	}

	private void setTitle() {
		setTitle(R.string.artists_title);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		mCurrentAlbumId = Long.valueOf(id).toString();

		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
		intent.putExtra("album", mCurrentAlbumId);
		Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition,
				childPosition);
		String album = c.getString(c
				.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
		if (album == null || album.equals(MediaStore.UNKNOWN_STRING)) {
			// unknown album, so we should include the artist ID to limit the
			// songs to songs only by that artist
			mArtistCursor.moveToPosition(groupPosition);
			mCurrentArtistId = mArtistCursor.getString(mArtistCursor
					.getColumnIndex(MediaStore.Audio.Artists._ID));
			intent.putExtra("artist", mCurrentArtistId);
		}
		startActivity(intent);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be
																// set in
																// onPrepareOptionsMenu()
		menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all);
		menu.add(0, SETTINGS, 0, R.string.settings);
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
		case PARTY_SHUFFLE:
			MusicUtils.togglePartyShuffle();
			break;

		case SHUFFLE_ALL:
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

		case SETTINGS:
			intent = new Intent();
			intent.setClass(this, MusicSettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfoIn) {
		menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
		SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
				R.string.add_to_playlist);
		MusicUtils.makePlaylistMenu(this, sub);
		menu.add(0, DELETE_ITEM, 0, R.string.delete_item);

		ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfoIn;

		int itemtype = ExpandableListView
				.getPackedPositionType(mi.packedPosition);
		int gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
		int cpos = ExpandableListView.getPackedPositionChild(mi.packedPosition);
		if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			if (gpos == -1) {
				// this shouldn't happen
				Log.d("Artist/Album", "no group");
				return;
			}
			gpos = gpos - getExpandableListView().getHeaderViewsCount();
			mArtistCursor.moveToPosition(gpos);
			menu.add(0, SHOW_ALL, 0, R.string.show_all);
			mCurrentArtistId = mArtistCursor.getString(mArtistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
			mCurrentArtistName = mArtistCursor.getString(mArtistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
			mCurrentAlbumId = null;
			mIsUnknownArtist = mCurrentArtistName == null
					|| mCurrentArtistName.equals(MediaStore.UNKNOWN_STRING);
			mIsUnknownAlbum = true;
			if (mIsUnknownArtist) {
				menu.setHeaderTitle(getString(R.string.unknown_artist_name));
			} else {
				menu.setHeaderTitle(mCurrentArtistName);
				menu.add(0, SEARCH, 0, R.string.search_title);
			}
			return;
		} else if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			if (cpos == -1) {
				// this shouldn't happen
				Log.d("Artist/Album", "no child");
				return;
			}
			Cursor c = (Cursor) getExpandableListAdapter().getChild(gpos, cpos);
			c.moveToPosition(cpos);
			mCurrentArtistId = null;
			mCurrentAlbumId = Long.valueOf(mi.id).toString();
			mCurrentAlbumName = c.getString(c
					.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
			gpos = gpos - getExpandableListView().getHeaderViewsCount();
			mArtistCursor.moveToPosition(gpos);
			mCurrentArtistNameForAlbum = mArtistCursor.getString(mArtistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
			mIsUnknownArtist = mCurrentArtistNameForAlbum == null
					|| mCurrentArtistNameForAlbum
							.equals(MediaStore.UNKNOWN_STRING);
			mIsUnknownAlbum = mCurrentAlbumName == null
					|| mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
			if (mIsUnknownAlbum) {
				menu.setHeaderTitle(getString(R.string.unknown_album_name));
			} else {
				menu.setHeaderTitle(mCurrentAlbumName);
			}
			if (!mIsUnknownAlbum || !mIsUnknownArtist) {
				menu.add(0, SEARCH, 0, R.string.search_title);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PLAY_SELECTION: {
			// play everything by the selected artist
			long[] list = mCurrentArtistId != null ? MusicUtils
					.getSongListForArtist(this,
							Long.parseLong(mCurrentArtistId)) : MusicUtils
					.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));

			MusicUtils.playAll(this, list, 0);
			return true;
		}
		case SHOW_ALL: {
			// Show everything by the selected artist
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("artist", Long.valueOf(mCurrentArtistId).toString());
			intent.putExtra("album_artist", mCurrentAlbumId);
			startActivity(intent);
			return true;
		}
		case QUEUE: {
			long[] list = mCurrentArtistId != null ? MusicUtils
					.getSongListForArtist(this,
							Long.parseLong(mCurrentArtistId)) : MusicUtils
					.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
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
			long[] list = mCurrentArtistId != null ? MusicUtils
					.getSongListForArtist(this,
							Long.parseLong(mCurrentArtistId)) : MusicUtils
					.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
			long playlist = item.getIntent().getLongExtra("playlist", 0);
			MusicUtils.addToPlaylist(this, list, playlist);
			return true;
		}

		case DELETE_ITEM: {
			long[] list;
			String desc;
			if (mCurrentArtistId != null) {
				list = MusicUtils.getSongListForArtist(this,
						Long.parseLong(mCurrentArtistId));
				String f;
				if (android.os.Environment.isExternalStorageRemovable()) {
					f = getString(R.string.delete_artist_desc);
				} else {
					f = getString(R.string.delete_artist_desc_nosdcard);
				}
				desc = String.format(f, mCurrentArtistName);
			} else {
				list = MusicUtils.getSongListForAlbum(this,
						Long.parseLong(mCurrentAlbumId));
				String f;
				if (android.os.Environment.isExternalStorageRemovable()) {
					f = getString(R.string.delete_album_desc);
				} else {
					f = getString(R.string.delete_album_desc_nosdcard);
				}

				desc = String.format(f, mCurrentAlbumName);
			}
			Bundle b = new Bundle();
			b.putString("description", desc);
			b.putLongArray("items", list);
			Intent intent = new Intent();
			intent.setClass(this, DeleteItems.class);
			intent.putExtras(b);
			startActivityForResult(intent, -1);
			return true;
		}

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

		if (mCurrentArtistId != null) {
			title = mCurrentArtistName;
			query = mCurrentArtistName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistName);
			i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS,
					MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
		} else {
			if (mIsUnknownAlbum) {
				title = query = mCurrentArtistNameForAlbum;
			} else {
				title = query = mCurrentAlbumName;
				if (!mIsUnknownArtist) {
					query = query + " " + mCurrentArtistNameForAlbum;
				}
			}
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST,
					mCurrentArtistNameForAlbum);
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
			i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS,
					MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
		}
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case SCAN_DONE:
			if (resultCode == RESULT_CANCELED) {
				finish();
			} else {
				getArtistCursor(mAdapter.getQueryHandler(), null);
			}
			break;

		case NEW_PLAYLIST:
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					long[] list = null;
					if (mCurrentArtistId != null) {
						list = MusicUtils.getSongListForArtist(this,
								Long.parseLong(mCurrentArtistId));
					} else if (mCurrentAlbumId != null) {
						list = MusicUtils.getSongListForAlbum(this,
								Long.parseLong(mCurrentAlbumId));
					}
					MusicUtils.addToPlaylist(this, list,
							Long.parseLong(uri.getLastPathSegment()));
				}
			}
			break;
		}
	}

	private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {

		String[] cols = new String[] { MediaStore.Audio.Artists._ID,
				MediaStore.Audio.Artists.ARTIST,
				MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
				MediaStore.Audio.Artists.NUMBER_OF_TRACKS };

		Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
		if (!TextUtils.isEmpty(filter)) {
			uri = uri.buildUpon()
					.appendQueryParameter("filter", Uri.encode(filter)).build();
		}

		Cursor ret = null;
		if (async != null) {
			async.startQuery(0, null, uri, cols, null, null,
					MediaStore.Audio.Artists.ARTIST_KEY);
		} else {
			ret = MusicUtils.query(this, uri, cols, null, null,
					MediaStore.Audio.Artists.ARTIST_KEY);
		}
		return ret;
	}

	static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter
			implements SectionIndexer {

		private final BitmapDrawable mDefaultAlbumIcon;
		private int mGroupArtistIdIdx;
		private int mGroupArtistIdx;
		private int mGroupAlbumIdx;
		private int mGroupSongIdx;
		private final Context mContext;
		private final Resources mResources;
		private final String mAlbumSongSeparator;
		private final String mUnknownAlbum;
		private final String mUnknownArtist;
		private final StringBuilder mBuffer = new StringBuilder();
		private final Object[] mFormatArgs = new Object[1];
		private final Object[] mFormatArgs3 = new Object[3];
		private MusicAlphabetIndexer mIndexer;
		private ArtistAlbumBrowserActivity mActivity;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			ImageView play_indicator;
			ImageView icon;
			ImageView mCM;
			RelativeLayout mGroup;
			RelativeLayout mChild;
			FrameLayout mContextMenu;
		}

		class QueryHandler extends AsyncQueryHandler {
			QueryHandler(ContentResolver res) {
				super(res);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// Log.i("@@@", "query complete");
				mActivity.init(cursor);
			}
		}

		ArtistAlbumListAdapter(Context context,
				ArtistAlbumBrowserActivity currentactivity, Cursor cursor,
				int glayout, String[] gfrom, int[] gto, int clayout,
				String[] cfrom, int[] cto) {
			super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());

			Resources r = context.getResources();
			mDefaultAlbumIcon = (BitmapDrawable) r
					.getDrawable(R.drawable.albumart_mp_unknown_list);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);

			mContext = context;
			getColumnIndices(cursor);
			mResources = context.getResources();
			mAlbumSongSeparator = context
					.getString(R.string.albumsongseparator);
			mUnknownAlbum = context.getString(R.string.unknown_album_name);
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mGroupArtistIdIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
				mGroupArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
				mGroupAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
				mGroupSongIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
				if (mIndexer != null) {
					mIndexer.setCursor(cursor);
				} else {
					mIndexer = new MusicAlphabetIndexer(cursor,
							mGroupArtistIdx,
							mResources.getString(R.string.fast_scroll_alphabet));
				}
			}
		}

		public void setActivity(ArtistAlbumBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public AsyncQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		private View.OnClickListener mCML = new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();

			}
		};

		@Override
		public View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {
			View v = super.newGroupView(context, cursor, isExpanded, parent);
			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			ViewGroup.LayoutParams p = iv.getLayoutParams();
			p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
			p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			vh.mCM = (ImageView) v.findViewById(R.id.CM);
			vh.mContextMenu = (FrameLayout) v
					.findViewById(R.id.second_column_icon);
			vh.mContextMenu.setOnClickListener(mCML);
			vh.mGroup = (RelativeLayout) v.findViewById(R.id.group_item);

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
						"artist_tab_artist_name_color_group", "color",
						themePackage);
				if (line1 != 0) {
					vh.line1.setTextColor(themeResources.getColor(line1));
				}
				int line2 = themeResources.getIdentifier(
						"artist_tab_number_of_albums_color", "color",
						themePackage);
				if (line2 != 0) {
					vh.line2.setTextColor(themeResources.getColor(line2));
				}
				int mGroupItem = themeResources.getIdentifier("group_bg",
						"drawable", themePackage);
				if (mGroupItem != 0) {
					vh.mGroup.setBackgroundDrawable(themeResources
							.getDrawable(mGroupItem));
				}
				loadThemeResource(themeResources, themePackage,
						"bt_context_menu", vh.mCM, THEME_ITEM_FOREGROUND);

			}
			return v;
		}

		@Override
		public View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			View v = super.newChildView(context, cursor, isLastChild, parent);
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			vh.mCM = (ImageView) v.findViewById(R.id.CM);
			vh.mContextMenu = (FrameLayout) v
					.findViewById(R.id.second_column_icon);
			vh.mContextMenu.setOnClickListener(mCML);
			vh.mChild = (RelativeLayout) v.findViewById(R.id.child_item);
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
						"artist_tab_artist_name_color_child", "color",
						themePackage);
				if (line1 != 0) {
					vh.line1.setTextColor(themeResources.getColor(line1));
				}
				int line2 = themeResources.getIdentifier(
						"artist_tab_number_of_songs_color", "color",
						themePackage);
				if (line2 != 0) {
					vh.line2.setTextColor(themeResources.getColor(line2));
				}
				int mChildItem = themeResources.getIdentifier("child_bg",
						"drawable", themePackage);
				if (mChildItem != 0) {
					vh.mChild.setBackgroundDrawable(themeResources
							.getDrawable(mChildItem));
				}
				loadThemeResource(themeResources, themePackage,
						"bt_context_menu", vh.mCM, THEME_ITEM_FOREGROUND);
			}
			return v;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor,
				boolean isexpanded) {
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

			String artist = cursor.getString(mGroupArtistIdx);
			String displayartist = artist;
			boolean unknown = artist == null
					|| artist.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayartist = mUnknownArtist;
			}
			vh.line1.setText(displayartist);

			int numalbums = cursor.getInt(mGroupAlbumIdx);
			int numsongs = cursor.getInt(mGroupSongIdx);

			String songs_albums = MusicUtils.makeAlbumsLabel(context,
					numalbums, numsongs, unknown);

			vh.line2.setText(songs_albums);

			long currentartistid = MusicUtils.getCurrentArtistId();
			long artistid = cursor.getLong(mGroupArtistIdIdx);
			ImageView iv = vh.play_indicator;
			if (currentartistid == artistid && !isexpanded) {
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
		public void bindChildView(View view, Context context, Cursor cursor,
				boolean islast) {
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

			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
			String displayname = name;
			boolean unknown = name == null
					|| name.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayname = mUnknownAlbum;
			}
			vh.line1.setText(displayname);

			int numsongs = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
			int numartistsongs = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));

			final StringBuilder builder = mBuffer;
			builder.delete(0, builder.length());
			if (unknown) {
				numsongs = numartistsongs;
			}

			if (numsongs == 1) {
				builder.append(context.getString(R.string.onesong));
			} else {
				if (numsongs == numartistsongs) {
					final Object[] args = mFormatArgs;
					args[0] = numsongs;
					builder.append(mResources.getQuantityString(
							R.plurals.Nsongs, numsongs, args));
				} else {
					final Object[] args = mFormatArgs3;
					args[0] = numsongs;
					args[1] = numartistsongs;
					args[2] = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
					builder.append(mResources.getQuantityString(
							R.plurals.Nsongscomp, numsongs, args));
				}
			}
			vh.line2.setText(builder.toString());

			ImageView iv = vh.icon;
			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			String art = cursor.getString(cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
			if (unknown || art == null || art.length() == 0) {
				iv.setBackgroundDrawable(mDefaultAlbumIcon);
				iv.setImageDrawable(null);
			} else {
				long artIndex = cursor.getLong(0);
				Drawable d = MusicUtils.getCachedArtwork(context, artIndex,
						mDefaultAlbumIcon);
				iv.setImageDrawable(d);
			}

			long currentalbumid = MusicUtils.getCurrentAlbumId();
			long aid = cursor.getLong(0);
			iv = vh.play_indicator;
			if (currentalbumid == aid) {
				iv.setBackgroundResource(R.anim.peak_meter);
				AnimationDrawable frameAnimation = (AnimationDrawable) iv
						.getBackground();
				if (themeResources != null) {
					int peak = themeResources.getIdentifier("peak_meter",
							"anim", themePackage);
					if (peak != 0) {
						iv.setBackgroundDrawable(themeResources
								.getDrawable(peak));
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
		protected Cursor getChildrenCursor(Cursor groupCursor) {

			long id = groupCursor.getLong(groupCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));

			String[] cols = new String[] { MediaStore.Audio.Albums._ID,
					MediaStore.Audio.Albums.ALBUM,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
					MediaStore.Audio.Albums.ALBUM_ART };
			Cursor c = MusicUtils.query(mActivity,
					MediaStore.Audio.Artists.Albums.getContentUri("external",
							id), cols, null, null,
					MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

			class MyCursorWrapper extends CursorWrapper {
				String mArtistName;
				int mMagicColumnIdx;

				MyCursorWrapper(Cursor c, String artist) {
					super(c);
					mArtistName = artist;
					if (mArtistName == null
							|| mArtistName.equals(MediaStore.UNKNOWN_STRING)) {
						mArtistName = mUnknownArtist;
					}
					mMagicColumnIdx = c.getColumnCount();
				}

				@Override
				public String getString(int columnIndex) {
					if (columnIndex != mMagicColumnIdx) {
						return super.getString(columnIndex);
					}
					return mArtistName;
				}

				@Override
				public int getColumnIndexOrThrow(String name) {
					if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
						return mMagicColumnIdx;
					}
					return super.getColumnIndexOrThrow(name);
				}

				@Override
				public String getColumnName(int idx) {
					if (idx != mMagicColumnIdx) {
						return super.getColumnName(idx);
					}
					return MediaStore.Audio.Albums.ARTIST;
				}

				@Override
				public int getColumnCount() {
					return super.getColumnCount() + 1;
				}
			}
			return new MyCursorWrapper(c,
					groupCursor.getString(mGroupArtistIdx));
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mArtistCursor) {
				mActivity.mArtistCursor = cursor;
				getColumnIndices(cursor);
				super.changeCursor(cursor);
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
			Cursor c = mActivity.getArtistCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}

		public Object[] getSections() {
			return mIndexer.getSections();
		}

		public int getPositionForSection(int sectionIndex) {
			return mIndexer.getPositionForSection(sectionIndex);
		}

		public int getSectionForPosition(int position) {
			return 0;
		}
	}

	private Cursor mArtistCursor;

	public void onServiceConnected(ComponentName name, IBinder service) {
		MusicUtils.updateNowPlaying(this);
	}

	public void onServiceDisconnected(ComponentName name) {
		finish();
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
		} catch (RemoteException ex) {
		}
	}

	private void doNext() {
		if (mService == null)
			return;
		try {
			mService.next();
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
