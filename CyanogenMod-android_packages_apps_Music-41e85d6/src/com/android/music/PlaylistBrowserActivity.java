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

import java.text.Collator;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.music.MusicUtils.ServiceToken;

public class PlaylistBrowserActivity extends ListActivity implements
		View.OnCreateContextMenuListener, MusicUtils.Defs, Shaker.Callback {
	private static final String TAG = "PlaylistBrowserActivity";
	private static final int DELETE_PLAYLIST = CHILD_MENU_BASE + 1;
	private static final int EDIT_PLAYLIST = CHILD_MENU_BASE + 2;
	private static final int RENAME_PLAYLIST = CHILD_MENU_BASE + 3;
	private static final int CHANGE_WEEKS = CHILD_MENU_BASE + 4;
	private static final long RECENTLY_ADDED_PLAYLIST = -1;
	private static final long ALL_SONGS_PLAYLIST = -2;
	private static final long PODCASTS_PLAYLIST = -3;
	private PlaylistListAdapter mAdapter;
	boolean mAdapterSent;
	private static int mLastListPosCourse = -1;
	private static int mLastListPosFine = -1;

	private boolean mCreateShortcut;
	private ServiceToken mToken;
	private ServiceToken mOSC;
	private IMediaPlaybackService mService = null;
	private SharedPreferences mPreferences;
	// Smaller now playing window buttons
	private ImageButton mDoSearch;
	private ImageButton mMarket;
	private ImageButton mNext;
	private ImageButton mPlay;
	private ImageButton mPlusPlaylist;
	private ImageButton mPrev;
	private ImageButton mShare;
	// Shake actions
	public Shaker Playlistshaker;
	private String artist_shake_actions_db;
	public boolean mShakeActions;
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

	public PlaylistBrowserActivity() {
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mPreferences = getSharedPreferences(
				MusicSettingsActivity.PREFERENCES_FILE, MODE_PRIVATE);

		mShakeActions = mPreferences.getBoolean(
				MusicSettingsActivity.KEY_ENABLE_BACKGROUND_SHAKE_ACTIONS,
				false);

		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			mCreateShortcut = true;
		}

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mToken = MusicUtils.bindToService(this, new ServiceConnection() {
			public void onServiceConnected(ComponentName classname, IBinder obj) {
				if (Intent.ACTION_VIEW.equals(action)) {
					Bundle b = intent.getExtras();
					if (b == null) {
						Log.w(TAG, "Unexpected:getExtras() returns null.");
					} else {
						try {
							long id = Long.parseLong(b.getString("playlist"));
							if (id == RECENTLY_ADDED_PLAYLIST) {
								playRecentlyAdded();
							} else if (id == PODCASTS_PLAYLIST) {
								playPodcasts();
							} else if (id == ALL_SONGS_PLAYLIST) {
								long[] list = MusicUtils
										.getAllSongs(PlaylistBrowserActivity.this);
								if (list != null) {
									MusicUtils.playAll(
											PlaylistBrowserActivity.this, list,
											0);
								}
							} else {
								MusicUtils.playPlaylist(
										PlaylistBrowserActivity.this, id);
							}
						} catch (NumberFormatException e) {
							Log.w(TAG, "Playlist id missing or broken");
						}
					}
					finish();
					return;
				}
				MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
			}

			public void onServiceDisconnected(ComponentName classname) {
			}

		});
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);

		setContentView(R.layout.media_picker_activity);
		MusicUtils.updateButtonBar(this, R.id.playlisttab);
		ListView lv = getListView();
		lv.setOnCreateContextMenuListener(this);
		lv.setTextFilterEnabled(true);

		mAdapter = (PlaylistListAdapter) getLastNonConfigurationInstance();
		if (mAdapter == null) {
			// Log.i("@@@", "starting query");
			mAdapter = new PlaylistListAdapter(getApplication(), this,
					R.layout.track_list_item, mPlaylistCursor,
					new String[] { MediaStore.Audio.Playlists.NAME },
					new int[] { android.R.id.text1 });
			setListAdapter(mAdapter);
			setTitle(R.string.working_playlists);
			getPlaylistCursor(mAdapter.getQueryHandler(), null);
		} else {
			mAdapter.setActivity(this);
			setListAdapter(mAdapter);
			mPlaylistCursor = mAdapter.getCursor();
			// If mPlaylistCursor is null, this can be because it doesn't have
			// a cursor yet (because the initial query that sets its cursor
			// is still in progress), or because the query failed.
			// In order to not flash the error dialog at the user for the
			// first case, simply retry the query when the cursor is null.
			// Worst case, we end up doing the same query twice.
			if (mPlaylistCursor != null) {
				init(mPlaylistCursor);
			} else {
				setTitle(R.string.working_playlists);
				getPlaylistCursor(mAdapter.getQueryHandler(), null);
			}
		}
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

		// Shake action sensitivity
		Playlistshaker = new Shaker(this, 2.25d, 500, this);

		// Smaller now playing window buttons
		mShare = (ImageButton) findViewById(R.id.share_song);
		mPlay = (ImageButton) findViewById(R.id.media_play);
		mNext = (ImageButton) findViewById(R.id.media_next);
		mPrev = (ImageButton) findViewById(R.id.media_prev);
		mMarket = (ImageButton) findViewById(R.id.market_music);
		mDoSearch = (ImageButton) findViewById(R.id.doSearch);
		mPlusPlaylist = (ImageButton) findViewById(R.id.plus_playlist);
		// I found that without setting the onClickListeners in Portrait mode
		// only, flipping into Landscape force closes.
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			// Smaller now playing window actions
			mPlusPlaylist.setOnClickListener(mAddPlaylist);
			mDoSearch.setOnClickListener(mDoSearchListener);
			mMarket.setOnClickListener(mMarketSearch);
			mPlay.setOnClickListener(mMediaPlay);
			mNext.setOnClickListener(mMediaNext);
			mPrev.setOnClickListener(mMediaPrev);
			mShare.setOnClickListener(mShareSong);
			// I only want this to show in the Playlist activity
			if (mPreferences.getBoolean(
					MusicSettingsActivity.KEY_ENABLE_NEW_PLAYLIST_BUTTON, true)) {
				mPlusPlaylist.setVisibility(View.VISIBLE);
			} else {
				mPlusPlaylist.setVisibility(View.GONE);
			} // Swipe gesture
			nowplayingview.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View vee, MotionEvent nowplayingevent) {
					if (gestureDetector.onTouchEvent(nowplayingevent)) {
						return false;
					}
					return false;
				}
			});
		}
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
							"vnd.android.cursor.dir/artistalbum");
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					PlaylistBrowserActivity.this.overridePendingTransition(
							R.anim.slide_in_right, R.anim.slide_out_left);
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					intent.setDataAndType(Uri.EMPTY,
							"vnd.android.cursor.dir/track");
					intent.putExtra("withtabs", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					PlaylistBrowserActivity.this.overridePendingTransition(
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

	private View.OnClickListener mAddPlaylist = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent;
			intent = new Intent();
			intent.setClass(PlaylistBrowserActivity.this, CreatePlaylist.class);
			startActivityForResult(intent, NEW_PLAYLIST);
			;
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
	public Object onRetainNonConfigurationInstance() {
		PlaylistListAdapter a = mAdapter;
		mAdapterSent = true;
		return a;
	}

	@Override
	public void onDestroy() {
		ListView lv = getListView();
		if (lv != null) {
			mLastListPosCourse = lv.getFirstVisiblePosition();
			View cv = lv.getChildAt(0);
			if (cv != null) {
				mLastListPosFine = cv.getTop();
			}
		}
		MusicUtils.unbindFromService(mToken);
		MusicUtils.unbindFromService(mOSC);
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
				MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
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
				intent.setClass(PlaylistBrowserActivity.this,
						MusicBrowserActivity.class);
				startActivity(intent);
			}
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService = null;
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		// This is needed to allow the media buttons on the small now playing
		// window to be controlled.
		mOSC = MusicUtils.bindToService(this, osc);
	}

	@Override
	public void onResume() {
		super.onResume();

		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.META_CHANGED);
		f.addAction(MediaPlaybackService.QUEUE_CHANGED);
		registerReceiver(mTrackListListener, f);
		mTrackListListener.onReceive(null, null);

		MusicUtils.setSpinnerState(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mTrackListListener);
		mReScanHandler.removeCallbacksAndMessages(null);
		if (!mShakeActions) {
			Playlistshaker.close();
		}
		super.onPause();
	}

	private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			getListView().invalidateViews();
			MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
		}
	};
	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MusicUtils.setSpinnerState(PlaylistBrowserActivity.this);
			mReScanHandler.sendEmptyMessage(0);
		}
	};

	private Handler mReScanHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mAdapter != null) {
				getPlaylistCursor(mAdapter.getQueryHandler(), null);
			}
		}
	};

	public void init(Cursor cursor) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(cursor);

		if (mPlaylistCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		// restore previous position
		if (mLastListPosCourse >= 0) {
			getListView().setSelectionFromTop(mLastListPosCourse,
					mLastListPosFine);
			mLastListPosCourse = -1;
		}
		MusicUtils.hideDatabaseError(this);
		MusicUtils.updateButtonBar(this, R.id.playlisttab);
		setTitle();
	}

	private void setTitle() {
		setTitle(R.string.playlists_title);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mCreateShortcut) {
			menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
			menu.add(0, SETTINGS, 0, R.string.settings);

		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MusicUtils.setPartyShuffleMenuIcon(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case PARTY_SHUFFLE:
			MusicUtils.togglePartyShuffle();
			return true;
		case SETTINGS:
			intent = new Intent();
			intent.setClass(this, MusicSettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfoIn) {
		if (mCreateShortcut) {
			return;
		}

		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;

		menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);

		if (mi.id >= 0 /* || mi.id == PODCASTS_PLAYLIST */) {
			menu.add(0, DELETE_PLAYLIST, 0, R.string.delete_playlist_menu);
		}

		if (mi.id == RECENTLY_ADDED_PLAYLIST) {
			menu.add(0, EDIT_PLAYLIST, 0, R.string.edit_playlist_menu);
		}

		if (mi.id >= 0) {
			menu.add(0, RENAME_PLAYLIST, 0, R.string.rename_playlist_menu);
		}

		mPlaylistCursor.moveToPosition(mi.position);
		menu.setHeaderTitle(mPlaylistCursor.getString(mPlaylistCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case PLAY_SELECTION:
			if (mi.id == RECENTLY_ADDED_PLAYLIST) {
				playRecentlyAdded();
			} else if (mi.id == PODCASTS_PLAYLIST) {
				playPodcasts();
			} else {
				MusicUtils.playPlaylist(this, mi.id);
			}
			break;
		case DELETE_PLAYLIST:
			Uri uri = ContentUris.withAppendedId(
					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mi.id);
			getContentResolver().delete(uri, null, null);
			Toast.makeText(this, R.string.playlist_deleted_message,
					Toast.LENGTH_SHORT).show();
			if (mPlaylistCursor.getCount() == 0) {
				setTitle(R.string.no_playlists_title);
			}
			break;
		case EDIT_PLAYLIST:
			if (mi.id == RECENTLY_ADDED_PLAYLIST) {
				Intent intent = new Intent();
				intent.setClass(this, WeekSelector.class);
				startActivityForResult(intent, CHANGE_WEEKS);
				return true;
			} else {
				Log.e(TAG, "should not be here");
			}
			break;
		case RENAME_PLAYLIST:
			Intent intent = new Intent();
			intent.setClass(this, RenamePlaylist.class);
			intent.putExtra("rename", mi.id);
			startActivityForResult(intent, RENAME_PLAYLIST);
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case SCAN_DONE:
			if (resultCode == RESULT_CANCELED) {
				finish();
			} else if (mAdapter != null) {
				getPlaylistCursor(mAdapter.getQueryHandler(), null);
			}
			break;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (mCreateShortcut) {
			final Intent shortcut = new Intent();
			shortcut.setAction(Intent.ACTION_VIEW);
			shortcut.setDataAndType(Uri.EMPTY,
					"vnd.android.cursor.dir/playlist");
			shortcut.putExtra("playlist", String.valueOf(id));

			final Intent intent = new Intent();
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
					((TextView) v.findViewById(R.id.line1)).getText());
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(this,
							R.drawable.ic_launcher_shortcut_music_playlist));

			setResult(RESULT_OK, intent);
			finish();
			return;
		}
		if (id == RECENTLY_ADDED_PLAYLIST) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("playlist", "recentlyadded");
			startActivity(intent);
		} else if (id == PODCASTS_PLAYLIST) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("playlist", "podcasts");
			startActivity(intent);
		} else {
			Intent intent = new Intent(Intent.ACTION_EDIT);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("playlist", Long.valueOf(id).toString());
			startActivity(intent);
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

	private void playPodcasts() {
		// do a query for all files that are podcasts
		final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
		Cursor cursor = MusicUtils.query(this,
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols,
				MediaStore.Audio.Media.IS_PODCAST + "=1", null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

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

	String[] mCols = new String[] { MediaStore.Audio.Playlists._ID,
			MediaStore.Audio.Playlists.NAME };

	private Cursor getPlaylistCursor(AsyncQueryHandler async,
			String filterstring) {

		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Playlists.NAME + " != ''");

		// Add in the filtering constraints
		String[] keywords = null;
		if (filterstring != null) {
			String[] searchWords = filterstring.split(" ");
			keywords = new String[searchWords.length];
			Collator col = Collator.getInstance();
			col.setStrength(Collator.PRIMARY);
			for (int i = 0; i < searchWords.length; i++) {
				keywords[i] = '%' + searchWords[i] + '%';
			}
			for (int i = 0; i < searchWords.length; i++) {
				where.append(" AND ");
				where.append(MediaStore.Audio.Playlists.NAME + " LIKE ?");
			}
		}

		String whereclause = where.toString();

		if (async != null) {
			async.startQuery(0, null,
					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mCols,
					whereclause, keywords, MediaStore.Audio.Playlists.NAME);
			return null;
		}
		Cursor c = null;
		c = MusicUtils.query(this,
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mCols,
				whereclause, keywords, MediaStore.Audio.Playlists.NAME);

		return mergedCursor(c);
	}

	private Cursor mergedCursor(Cursor c) {
		if (c == null) {
			return null;
		}
		if (c instanceof MergeCursor) {
			// this shouldn't happen, but fail gracefully
			Log.d("PlaylistBrowserActivity", "Already wrapped");
			return c;
		}
		MatrixCursor autoplaylistscursor = new MatrixCursor(mCols);
		if (mCreateShortcut) {
			ArrayList<Object> all = new ArrayList<Object>(2);
			all.add(ALL_SONGS_PLAYLIST);
			all.add(getString(R.string.play_all));
			autoplaylistscursor.addRow(all);
		}
		ArrayList<Object> recent = new ArrayList<Object>(2);
		recent.add(RECENTLY_ADDED_PLAYLIST);
		recent.add(getString(R.string.recentlyadded));
		autoplaylistscursor.addRow(recent);

		// check if there are any podcasts
		Cursor counter = MusicUtils.query(this,
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { "count(*)" }, "is_podcast=1", null, null);
		if (counter != null) {
			counter.moveToFirst();
			int numpodcasts = counter.getInt(0);
			counter.close();
			if (numpodcasts > 0) {
				ArrayList<Object> podcasts = new ArrayList<Object>(2);
				podcasts.add(PODCASTS_PLAYLIST);
				podcasts.add(getString(R.string.podcasts_listitem));
				autoplaylistscursor.addRow(podcasts);
			}
		}

		Cursor cc = new MergeCursor(new Cursor[] { autoplaylistscursor, c });
		return cc;
	}

	static class PlaylistListAdapter extends SimpleCursorAdapter {
		int mTitleIdx;
		int mIdIdx;
		private PlaylistBrowserActivity mActivity = null;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		class QueryHandler extends AsyncQueryHandler {
			QueryHandler(ContentResolver res) {
				super(res);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// Log.i("@@@", "query complete: " + cursor.getCount() + "   " +
				// mActivity);
				if (cursor != null) {
					cursor = mActivity.mergedCursor(cursor);
				}
				mActivity.init(cursor);
			}
		}

		PlaylistListAdapter(Context context,
				PlaylistBrowserActivity currentactivity, int layout,
				Cursor cursor, String[] from, int[] to) {
			super(context, layout, cursor, from, to);
			mActivity = currentactivity;
			getColumnIndices(cursor);
			mQueryHandler = new QueryHandler(context.getContentResolver());
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mTitleIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
				mIdIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
			}
		}

		public void setActivity(PlaylistBrowserActivity newactivity) {
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
		public void bindView(View view, Context context, Cursor cursor) {

			TextView tv = (TextView) view.findViewById(R.id.line1);

			String name = cursor.getString(mTitleIdx);
			tv.setText(name);

			long id = cursor.getLong(mIdIdx);

			ImageView iv = (ImageView) view.findViewById(R.id.icon);
			if (id == RECENTLY_ADDED_PLAYLIST) {
				iv.setImageResource(R.drawable.ic_mp_playlist_recently_added_list);
			} else {
				iv.setImageResource(R.drawable.ic_mp_playlist_list);
			}
			ViewGroup.LayoutParams p = iv.getLayoutParams();
			p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
			p.height = ViewGroup.LayoutParams.WRAP_CONTENT;

			iv = (ImageView) view.findViewById(R.id.play_indicator);
			iv.setVisibility(View.GONE);

			view.findViewById(R.id.line2).setVisibility(View.GONE);

			FrameLayout mContextMenu = (FrameLayout) view
					.findViewById(R.id.second_column_icon);
			iv.setVisibility(View.GONE);
			mContextMenu.setOnClickListener(mCML);
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mPlaylistCursor) {
				mActivity.mPlaylistCursor = cursor;
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
			Cursor c = mActivity.getPlaylistCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}
	}

	private Cursor mPlaylistCursor;

	// Methods for media control
	private void doPauseResume() {
		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				// We don't need refreshNow() because we aren't in the now
				// playing activity
				setPauseButtonImage();
			}
		} catch (RemoteException ex) {
		}
	}

	private void setPauseButtonImage() {
		try {
			if (mService != null && mService.isPlaying()) {
				mPlay.setImageResource(R.drawable.ic_media_pause);
			} else {
				mPlay.setImageResource(R.drawable.ic_appwidget_music_play);
			}
		} catch (RemoteException ex) {
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

	@Override
	public void shakingStarted() {
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "0");
		if (artist_shake_actions_db.equals("0")) {
			// NONE
		}
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "1");
		if (artist_shake_actions_db.equals("1")) {
			doPauseResume();
		}
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "2");
		if (artist_shake_actions_db.equals("2")) {
			doNext();
		}
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "3");
		if (artist_shake_actions_db.equals("3")) {
			doPrev();
		}
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "4");
		if (artist_shake_actions_db.equals("4")) {
			Cursor cursor;
			cursor = MusicUtils.query(this,
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[] { BaseColumns._ID }, AudioColumns.IS_MUSIC
							+ "=1", null,
					MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
			if (cursor != null) {
				MusicUtils.shuffleAll(this, cursor);
				cursor.close();
			}
		}
		artist_shake_actions_db = mPreferences.getString(
				"artist_shake_actions_db", "5");
		if (artist_shake_actions_db.equals("5")) {
			MusicUtils.togglePartyShuffle();
		}

	}

	@Override
	public void shakingStopped() {

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