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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.music.MusicUtils.ServiceToken;

public class MediaPlaybackActivity extends Activity implements MusicUtils.Defs,
		View.OnTouchListener, View.OnLongClickListener, Shaker.Callback {
	private static final int USE_AS_RINGTONE = CHILD_MENU_BASE;

	private SharedPreferences mPreferences;
	private boolean mSeeking = false;
	private boolean mDeviceHasDpad;
	private long mStartSeekPos = 0;
	private long mLastSeekEventTime;
	private IMediaPlaybackService mService = null;
	private Worker mAlbumArtWorker;
	private AlbumArtHandler mAlbumArtHandler;
	private Toast mToast;
	private int mTouchSlop;
	private ServiceToken mToken;
	private GestureOverlayView mGestureOverlayView;
	private GestureLibrary mGestureLibrary;
	private Vibrator mVibrator;
	// Media buttons
	private RepeatingImageButton mPrevButton;
	private ImageButton mPauseButton;
	private RepeatingImageButton mNextButton;
	private LinearLayout mRepeatButton;
	private TextView mRepeatButtonText;
	private LinearLayout mShuffleButton;
	private TextView mShuffleButtonText;
	private LinearLayout mShareButton;
	private ImageButton mRepeatButtonImage;
	private ImageButton mShuffleButtonImage;
	public ImageButton mShareButtonImage;
	public ImageButton mEffectsButtonImage;
	private LinearLayout mExtra;
	private LinearLayout mEQButton;
	private ImageButton mRepeatNormal;
	private ImageButton mShuffleNormal;
	private ImageButton mDeleteButton;
	private ImageButton mEQ;
	private ImageButton mShopButton;
	private ImageButton mRingButton;
	private LinearLayout mRingtone;
	private LinearLayout mShop;
	private LinearLayout mDelete;
	// Screen on while playing/charging
	private boolean pluggedIn;
	// Shake actions
	private Shaker shaker;
	private String shake_actions_db;
	// Animations
	private String np_animation_ui_db;
	// ADW Theme constants
	public static final int THEME_ITEM_BACKGROUND = 0;
	public static final int THEME_ITEM_FOREGROUND = 1;
	public static final int THEME_ITEM_TEXT_DRAWABLE = 2;

	public MediaPlaybackActivity() {
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ActionBar bar = getActionBar();
		bar.setTitle(R.string.nowplaying_title);
		bar.setDisplayHomeAsUpEnabled(true);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mPreferences = getSharedPreferences(
				MusicSettingsActivity.PREFERENCES_FILE, MODE_PRIVATE);

		mAlbumArtWorker = new Worker("album art worker");
		mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());

		setContentView(R.layout.player);

		// Shake Action Sensitivity
		shaker = new Shaker(this, 2.25d, 500, this);

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mProgress = (ProgressBar) findViewById(android.R.id.progress);
		mAlbum = (ImageView) findViewById(R.id.album);
		mAlbumDummy = (ImageView) findViewById(R.id.album_dummy);
		mAlbumName = (TextView) findViewById(R.id.albumname);
		mTrackName = (TextView) findViewById(R.id.trackname);
		mArtistName = (TextView) findViewById(R.id.artistname);
		mExtra = (LinearLayout) findViewById(R.id.extra_controls);

		mAlbumName.setOnLongClickListener(this);
		mAlbumName.setOnTouchListener(this);

		mTrackName.setOnLongClickListener(this);
		mTrackName.setOnTouchListener(this);
		mTrackName.setOnClickListener(mTrackNameClick);

		mArtistName.setOnLongClickListener(this);
		// For some reason the onTouchListerner won't work for this view, so I
		// resorted to this instead.
		mArtistName.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.btn_bg));
		mArtistName.setOnClickListener(mArtistNameClick);

		mAlbum.setOnClickListener(mExtraControls);
		mAlbum.setOnLongClickListener(mQueueListener);
		mAlbumDummy.setOnLongClickListener(mQueueListener);
		mAlbumDummy.setOnClickListener(mExtraControlsDummy);
		mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
		mPrevButton.setOnClickListener(mPrevListener);
		mPrevButton.setRepeatListener(mRewListener, 260);
		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);
		mNextButton = (RepeatingImageButton) findViewById(R.id.next);
		mNextButton.setOnClickListener(mNextListener);
		mNextButton.setRepeatListener(mFfwdListener, 260);
		mRepeatNormal = (ImageButton) findViewById(R.id.repeat_normal);
		mShuffleNormal = (ImageButton) findViewById(R.id.shuffle_normal);

		seekmethod = 1;

		mDeviceHasDpad = (getResources().getConfiguration().navigation == Configuration.NAVIGATION_DPAD);

		mShuffleButton = ((LinearLayout) findViewById(R.id.shuffle));
		mShuffleButton.setOnClickListener(mShuffleListener);
		mShuffleButton.setOnLongClickListener(mPartyShuffle);
		mShuffleButtonText = (TextView) findViewById(R.id.shuffle_text);
		mRepeatButton = ((LinearLayout) findViewById(R.id.repeat));
		mRepeatButtonText = (TextView) findViewById(R.id.repeat_text);
		mRepeatButton.setOnClickListener(mRepeatListener);
		mShareButton = (LinearLayout) findViewById(R.id.share_extra);
		mShareButton.setOnClickListener(shareTrack);
		mEQButton = (LinearLayout) findViewById(R.id.eq_extra);
		mEQButton.setOnClickListener(EQ);
		mShuffleButtonImage = (ImageButton) findViewById(R.id.shuffle_extra_image);
		mRepeatButtonImage = (ImageButton) findViewById(R.id.repeat_extra_image);
		mEffectsButtonImage = (ImageButton) findViewById(R.id.eq_extra_image);
		mShareButtonImage = (ImageButton) findViewById(R.id.share_extra_image);
		mRingtone = (LinearLayout) findViewById(R.id.ringtone);
		mShop = (LinearLayout) findViewById(R.id.shop);
		mDelete = (LinearLayout) findViewById(R.id.delete_extra);

		try {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mExtra.setVisibility(View.VISIBLE);
				mRepeatNormal.setVisibility(View.VISIBLE);
				mShuffleNormal.setVisibility(View.VISIBLE);
				mRingtone.setVisibility(View.VISIBLE);
				mShop.setVisibility(View.VISIBLE);
				mDelete.setVisibility(View.VISIBLE);
				mRepeatButton.setVisibility(View.GONE);
				mShuffleButton.setVisibility(View.GONE);

				mShop.setOnClickListener(mShopper);
				mDelete.setOnClickListener(mEraser);
				mRingtone.setOnClickListener(mRing);
				mShuffleNormal.setOnClickListener(mShuffleListener);
				mShuffleNormal.setOnLongClickListener(mPartyShuffle);
				mRepeatNormal.setOnClickListener(mRepeatListener);

				mAlbumName.setOnClickListener(mTrackNameClick);

			} else {
				mAlbumName.setOnClickListener(mArtistNameClick);
			}
		} catch (NullPointerException e) {

		}

		if (mProgress instanceof SeekBar) {
			SeekBar seeker = (SeekBar) mProgress;
			seeker.setOnSeekBarChangeListener(mSeekListener);
		}
		mProgress.setMax(1000);

		mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
			mShuffleButton = ((LinearLayout) findViewById(R.id.shuffle));
			mShuffleButtonText = (TextView) findViewById(R.id.shuffle_text);
			mRepeatButton = ((LinearLayout) findViewById(R.id.repeat));
			mRepeatButtonText = (TextView) findViewById(R.id.repeat_text);
			mShareButton = (LinearLayout) findViewById(R.id.share_extra);
			mEQButton = (LinearLayout) findViewById(R.id.eq_extra);
			mShuffleButtonImage = (ImageButton) findViewById(R.id.shuffle_extra_image);
			mRepeatButtonImage = (ImageButton) findViewById(R.id.repeat_extra_image);
			mEffectsButtonImage = (ImageButton) findViewById(R.id.eq_extra_image);
			mShareButtonImage = (ImageButton) findViewById(R.id.share_extra_image);
			mRingtone = (LinearLayout) findViewById(R.id.ringtone);
			mShop = (LinearLayout) findViewById(R.id.shop);
			mDelete = (LinearLayout) findViewById(R.id.delete_extra);
			mNextButton = (RepeatingImageButton) findViewById(R.id.next);
			mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
			mRepeatNormal = (ImageButton) findViewById(R.id.repeat_normal);
			mShuffleNormal = (ImageButton) findViewById(R.id.shuffle_normal);
			mShopButton = (ImageButton) findViewById(R.id.shop_button);
			mEQ = (ImageButton) findViewById(R.id.eq_extra_image);
			mRingButton = (ImageButton) findViewById(R.id.ring_button);
			mDeleteButton = (ImageButton) findViewById(R.id.delete_button);
			SeekBar seekBar = (SeekBar) findViewById(android.R.id.progress);
			// Extra Controls background
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_shuffle_bg", mShuffleButton,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_repeat_bg", mRepeatButton,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_share_bg", mShareButton,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_market_bg", mShop, THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_ring_bg", mRingtone,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_delete_bg", mDelete,
					THEME_ITEM_BACKGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_eq_bg", mEQButton, THEME_ITEM_BACKGROUND);
			// Extra Controls ImageButtons
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_market", mShopButton,
					THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity
					.loadThemeResource(themeResources, themePackage, "np_ring",
							mRingButton, THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_share", mShareButton,
					THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_eq", mEQ, THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
					themePackage, "np_delete", mDeleteButton,
					THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity
					.loadThemeResource(themeResources, themePackage, "np_prev",
							mNextButton, THEME_ITEM_FOREGROUND);
			ArtistAlbumBrowserActivity
					.loadThemeResource(themeResources, themePackage, "np_next",
							mPrevButton, THEME_ITEM_FOREGROUND);
			int seeker = themeResources.getIdentifier("progress_horizontal",
					"drawable", themePackage);
			if (seeker != 0) {
				seekBar.setProgressDrawable(themeResources.getDrawable(seeker));
			}

		}
	}

	int mInitialX = -1;
	int mLastX = -1;
	int mTextWidth = 0;
	int mViewWidth = 0;
	boolean mDraggingLabel = false;

	TextView textViewForContainer(View v) {
		View vv;
		vv = v.findViewById(R.id.albumname);
		if (vv != null)
			return (TextView) vv;
		vv = v.findViewById(R.id.trackname);
		if (vv != null)
			return (TextView) vv;
		return null;
	}

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		TextView tv = textViewForContainer(v);
		if (tv == null) {
			return false;
		}
		if (action == MotionEvent.ACTION_DOWN) {
			v.setBackgroundColor(getResources().getColor(R.color.ics_opaque));
			mInitialX = mLastX = (int) event.getX();
			mDraggingLabel = false;
		} else if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_CANCEL) {
			v.setBackgroundColor(0);
			if (mDraggingLabel) {
				Message msg = mLabelScroller.obtainMessage(0, tv);
				mLabelScroller.sendMessageDelayed(msg, 1000);
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (mDraggingLabel) {
				int scrollx = tv.getScrollX();
				int x = (int) event.getX();
				int delta = mLastX - x;
				if (delta != 0) {
					mLastX = x;
					scrollx += delta;
					if (scrollx > mTextWidth) {
						// scrolled the text completely off the view to the left
						scrollx -= mTextWidth;
						scrollx -= mViewWidth;
					}
					if (scrollx < -mViewWidth) {
						// scrolled the text completely off the view to the
						// right
						scrollx += mViewWidth;
						scrollx += mTextWidth;
					}
					tv.scrollTo(scrollx, 0);
				}
				return true;
			}
			int delta = mInitialX - (int) event.getX();
			if (Math.abs(delta) > mTouchSlop) {
				// start moving
				mLabelScroller.removeMessages(0, tv);

				// Only turn ellipsizing off when it's not already off, because
				// it
				// causes the scroll position to be reset to 0.
				if (tv.getEllipsize() != null) {
					tv.setEllipsize(null);
				}
				Layout ll = tv.getLayout();
				// layout might be null if the text just changed, or ellipsizing
				// was just turned off
				if (ll == null) {
					return false;
				}
				// get the non-ellipsized line width, to determine whether
				// scrolling
				// should even be allowed
				mTextWidth = (int) tv.getLayout().getLineWidth(0);
				mViewWidth = tv.getWidth();
				if (mViewWidth > mTextWidth) {
					tv.setEllipsize(TruncateAt.END);
					v.cancelLongPress();
					return false;
				}
				mDraggingLabel = true;
				tv.setHorizontalFadingEdgeEnabled(true);
				v.cancelLongPress();
				return true;
			}
		}
		return false;
	}

	Handler mLabelScroller = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			TextView tv = (TextView) msg.obj;
			int x = tv.getScrollX();
			x = x * 3 / 4;
			tv.scrollTo(x, 0);
			if (x == 0) {
				tv.setEllipsize(TruncateAt.END);
			} else {
				Message newmsg = obtainMessage(0, tv);
				mLabelScroller.sendMessageDelayed(newmsg, 15);
			}
		}
	};

	public boolean onLongClick(View view) {

		CharSequence title = null;
		String mime = null;
		String query = null;
		String artist;
		String album;
		String song;
		long audioid;

		try {
			artist = mService.getArtistName();
			album = mService.getAlbumName();
			song = mService.getTrackName();
			audioid = mService.getAudioId();
		} catch (RemoteException ex) {
			return true;
		} catch (NullPointerException ex) {
			// we might not actually have the service yet
			return true;
		}

		if (MediaStore.UNKNOWN_STRING.equals(album)
				&& MediaStore.UNKNOWN_STRING.equals(artist) && song != null
				&& song.startsWith("recording")) {
			// not music
			return false;
		}

		if (audioid < 0) {
			return false;
		}

		Cursor c = MusicUtils.query(this, ContentUris.withAppendedId(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioid),
				new String[] { MediaStore.Audio.Media.IS_MUSIC }, null, null,
				null);
		boolean ismusic = true;
		if (c != null) {
			if (c.moveToFirst()) {
				ismusic = c.getInt(0) != 0;
			}
			c.close();
		}
		if (!ismusic) {
			return false;
		}

		boolean knownartist = (artist != null)
				&& !MediaStore.UNKNOWN_STRING.equals(artist);

		boolean knownalbum = (album != null)
				&& !MediaStore.UNKNOWN_STRING.equals(album);
		if (knownartist && view.equals(mArtistName)) {
			title = artist;
			query = artist;
			mime = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE;

		} else if (knownalbum && view.equals(mAlbumName)) {
			title = album;
			if (knownartist) {
				query = artist + " " + album;
			} else {
				query = album;
			}
			mime = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE;
		} else if (view.equals(mTrackName) || !knownartist || !knownalbum) {

			if ((song == null) || MediaStore.UNKNOWN_STRING.equals(song)) {
				// A popup of the form "Search for null/'' using ..." is pretty
				// unhelpful, plus, we won't find any way to buy it anyway.
				return true;
			}

			title = song;
			if (knownartist) {
				query = artist + " " + song;
			} else {
				query = song;
			}
			mime = "audio/*"; // the specific type doesn't matter, so don't
								// bother retrieving it
		} else {
			throw new RuntimeException("shouldn't be here");
		}
		title = getString(R.string.mediasearch, title);

		Intent i = new Intent();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.putExtra(SearchManager.QUERY, query);
		if (knownartist) {
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		}
		if (knownalbum) {
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
		}
		i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, song);
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, mime);

		startActivity(Intent.createChooser(i, title));
		return true;
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mLastSeekEventTime = 0;
			mFromTouch = true;
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser || (mService == null))
				return;
			long now = SystemClock.elapsedRealtime();
			if ((now - mLastSeekEventTime) > 250) {
				mLastSeekEventTime = now;
				mPosOverride = mDuration * progress / 1000;
				try {
					mService.seek(mPosOverride);
				} catch (RemoteException ex) {
				}

				// trackball event, allow progress updates
				if (!mFromTouch) {
					refreshNow();
					mPosOverride = -1;
				}
			}
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mPosOverride = -1;
			mFromTouch = false;
		}
	};
	private View.OnClickListener mRing = new View.OnClickListener() {
		public void onClick(View v) {
			// Set the system setting to make this the current ringtone
			if (mService != null) {
				try {
					MusicUtils.setRingtone(getBaseContext(),
							mService.getAudioId());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	};
	private View.OnClickListener mShopper = new View.OnClickListener() {
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
	private View.OnClickListener mEraser = new View.OnClickListener() {
		Intent intent;

		public void onClick(View v) {
			if (mService != null) {
				long[] list = new long[1];
				list[0] = MusicUtils.getCurrentAudioId();
				Bundle b = new Bundle();
				String f;
				if (android.os.Environment.isExternalStorageRemovable()) {
					try {
						f = getString(R.string.delete_song_desc,
								mService.getTrackName());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						f = getString(R.string.delete_song_desc_nosdcard,
								mService.getTrackName());
						b.putString("description", f);
						b.putLongArray("items", list);
						intent = new Intent();
						intent.setClass(MediaPlaybackActivity.this,
								DeleteItems.class);
						intent.putExtras(b);
						startActivityForResult(intent, -1);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}
	};
	private View.OnLongClickListener mPartyShuffle = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			MusicUtils.togglePartyShuffle();
			setShuffleButtonImage();
			return true;
		}
	};
	private View.OnClickListener shareTrack = new View.OnClickListener() {
		public void onClick(View v) {
			try {
				shareCurrentTrack();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	private View.OnClickListener mExtraControls = new View.OnClickListener() {
		public void onClick(View v) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				mAlbum.setVisibility(View.GONE);
				mAlbumDummy.setVisibility(View.VISIBLE);
				mExtra.setVisibility(View.VISIBLE);
				mExtra.startAnimation(AnimationUtils.loadAnimation(
						getBaseContext(), android.R.anim.fade_in));
			} else {
				mExtra.setVisibility(View.VISIBLE);
			}
		}
	};
	private View.OnClickListener mExtraControlsDummy = new View.OnClickListener() {
		public void onClick(View v) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				mAlbum.setVisibility(View.VISIBLE);
				mAlbumDummy.setVisibility(View.GONE);
				mExtra.setVisibility(View.GONE);
				mExtra.startAnimation(AnimationUtils.loadAnimation(
						getBaseContext(), android.R.anim.fade_out));
			} else {
				mExtra.setVisibility(View.VISIBLE);
			}
		}
	};
	private View.OnLongClickListener mQueueListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			startActivity(new Intent(Intent.ACTION_EDIT).setDataAndType(
					Uri.EMPTY, "vnd.android.cursor.dir/track").putExtra(
					"playlist", "nowplaying"));
			return true;
		}
	};

	private View.OnClickListener mShuffleListener = new View.OnClickListener() {
		public void onClick(View v) {
			toggleShuffle();
		}
	};

	private View.OnClickListener mRepeatListener = new View.OnClickListener() {
		public void onClick(View v) {
			cycleRepeat();
		}
	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			doPauseResume();
		}
	};

	private View.OnClickListener mPrevListener = new View.OnClickListener() {
		public void onClick(View v) {
			doPrev();
		}
	};

	private View.OnClickListener mNextListener = new View.OnClickListener() {
		public void onClick(View v) {
			doNext();
		}
	};

	private RepeatingImageButton.RepeatListener mRewListener = new RepeatingImageButton.RepeatListener() {
		public void onRepeat(View v, long howlong, int repcnt) {
			scanBackward(repcnt, howlong);
		}
	};

	private RepeatingImageButton.RepeatListener mFfwdListener = new RepeatingImageButton.RepeatListener() {
		public void onRepeat(View v, long howlong, int repcnt) {
			scanForward(repcnt, howlong);
		}
	};
	private View.OnClickListener mTrackNameClick = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("album",
					Long.valueOf(MusicUtils.getCurrentAlbumId()).toString());
			intent.putExtra("album_artist", 0);
			startActivity(intent);
		}
	};
	private View.OnClickListener mArtistNameClick = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("artist",
					Long.valueOf(MusicUtils.getCurrentArtistId()).toString());
			intent.putExtra("album_artist", 0);
			startActivity(intent);
		}
	};

	private View.OnClickListener EQ = new View.OnClickListener() {
		public void onClick(View v) {
			Intent i = new Intent(
					AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
			try {
				i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
						mService.getAudioSessionId());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			startActivityForResult(i, EFFECTS_PANEL);
		}
	};

	@Override
	public void onStop() {
		paused = true;
		mHandler.removeMessages(REFRESH);
		unregisterReceiver(mStatusListener);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();
		paused = false;

		mToken = MusicUtils.bindToService(this, osc);
		if (mToken == null) {
			// something went wrong
			mHandler.sendEmptyMessage(QUIT);
		}

		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
		f.addAction(MediaPlaybackService.META_CHANGED);
		registerReceiver(mStatusListener, new IntentFilter(f));

		updateTrackInfo();
		long next = refreshNow();
		queueNextRefresh(next);
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onPause() {
		shaker.close(); // The only shake actions to stay in the "background"
						// are the tabs.
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateTrackInfo();
		setPauseButtonImage();
	}

	@Override
	public void onDestroy() {
		mAlbumArtWorker.quit();
		shaker.close();
		super.onDestroy();
		// System.out.println("***************** playback activity onDestroy\n");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Don't show the menu items if we got launched by path/filedescriptor,
		// or
		// if we're in one shot mode. In most cases, these menu items are not
		// useful in those modes, so for consistency we never show them in these
		// modes, instead of tailoring them to the specific file being played.
		if (MusicUtils.getCurrentAudioId() >= 0) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				menu.add(0, SHOP_MARKET, 0, R.string.shop);
				menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
				menu.add(1, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short);
				menu.add(0, SETTINGS, 0, R.string.settings);
				menu.add(1, DELETE_ITEM, 0, R.string.delete_item).setIcon(
						R.drawable.ic_menu_delete);
			} else {
				menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
				menu.add(0, SETTINGS, 0, R.string.settings);
			}

			SubMenu addTo = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
					R.string.add_to_playlist).setIcon(R.drawable.ic_menu_add);
			MenuItem subMenuItem = addTo.getItem();
			subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
					| MenuItem.SHOW_AS_ACTION_WITH_TEXT);

			return true;
		}
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mService == null)
			return false;
		MenuItem item = menu.findItem(PARTY_SHUFFLE);
		if (item != null) {
			int shuffle = MusicUtils.getCurrentShuffleMode();
			if (shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
				item.setTitle(R.string.party_shuffle_off);
			} else {
				item.setTitle(R.string.party_shuffle);
			}
			item = menu.findItem(ADD_TO_PLAYLIST);
			if (item != null) {
				SubMenu sub = item.getSubMenu();
				MusicUtils.makePlaylistMenuNowPlaying(this, sub);
			}
		}

		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		try {
			switch (item.getItemId()) {
			case android.R.id.home:
				intent = new Intent();
				intent.setClass(this, MusicBrowserActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				finish();
				break;
			case USE_AS_RINGTONE: {
				// Set the system setting to make this the current ringtone
				if (mService != null) {
					MusicUtils.setRingtone(this, mService.getAudioId());
				}
				return true;
			}
			case PARTY_SHUFFLE:
				MusicUtils.togglePartyShuffle();
				setShuffleButtonImage();
				break;

			case NEW_PLAYLIST: {
				intent = new Intent();
				intent.setClass(this, CreatePlaylist.class);
				startActivityForResult(intent, NEW_PLAYLIST);
				return true;
			}

			case PLAYLIST_SELECTED: {
				long[] list = new long[1];
				list[0] = MusicUtils.getCurrentAudioId();
				long playlist = item.getIntent().getLongExtra("playlist", 0);
				MusicUtils.addToPlaylist(this, list, playlist);
				return true;
			}

			case DELETE_ITEM: {
				if (mService != null) {
					long[] list = new long[1];
					list[0] = MusicUtils.getCurrentAudioId();
					Bundle b = new Bundle();
					String f;
					if (android.os.Environment.isExternalStorageRemovable()) {
						f = getString(R.string.delete_song_desc,
								mService.getTrackName());
					} else {
						f = getString(R.string.delete_song_desc_nosdcard,
								mService.getTrackName());
					}
					b.putString("description", f);
					b.putLongArray("items", list);
					intent = new Intent();
					intent.setClass(this, DeleteItems.class);
					intent.putExtras(b);
					startActivityForResult(intent, -1);
				}
				return true;
			}

			case SETTINGS: {
				intent = new Intent();
				intent.setClass(this, MusicSettingsActivity.class);
				startActivityForResult(intent, SETTINGS);
				return true;
			}
			case SHOP_MARKET:
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
			return true;

		} catch (RemoteException ex) {
		}
		return super.onOptionsItemSelected(item);
	}

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

	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case NEW_PLAYLIST:
			Uri uri = intent.getData();
			if (uri != null) {
				long[] list = new long[1];
				list[0] = MusicUtils.getCurrentAudioId();
				int playlist = Integer.parseInt(uri.getLastPathSegment());
				MusicUtils.addToPlaylist(this, list, playlist);
			}
			break;
		}
	}

	private final int keyboard[][] = {
			{ KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E,
					KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
					KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
					KeyEvent.KEYCODE_P, },
			{ KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
					KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
					KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
					KeyEvent.KEYCODE_DEL, },
			{ KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C,
					KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_N,
					KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_COMMA,
					KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_ENTER }

	};

	private int lastX;
	private int lastY;

	private boolean seekMethod1(int keyCode) {
		if (mService == null)
			return false;
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 3; y++) {
				if (keyboard[y][x] == keyCode) {
					int dir = 0;
					// top row
					if (x == lastX && y == lastY)
						dir = 0;
					else if (y == 0 && lastY == 0 && x > lastX)
						dir = 1;
					else if (y == 0 && lastY == 0 && x < lastX)
						dir = -1;
					// bottom row
					else if (y == 2 && lastY == 2 && x > lastX)
						dir = -1;
					else if (y == 2 && lastY == 2 && x < lastX)
						dir = 1;
					// moving up
					else if (y < lastY && x <= 4)
						dir = 1;
					else if (y < lastY && x >= 5)
						dir = -1;
					// moving down
					else if (y > lastY && x <= 4)
						dir = -1;
					else if (y > lastY && x >= 5)
						dir = 1;
					lastX = x;
					lastY = y;
					try {
						mService.seek(mService.position() + dir * 5);
					} catch (RemoteException ex) {
					}
					refreshNow();
					return true;
				}
			}
		}
		lastX = -1;
		lastY = -1;
		return false;
	}

	private boolean seekMethod2(int keyCode) {
		if (mService == null)
			return false;
		for (int i = 0; i < 10; i++) {
			if (keyboard[0][i] == keyCode) {
				int seekpercentage = 100 * i / 10;
				try {
					mService.seek(mService.duration() * seekpercentage / 100);
				} catch (RemoteException ex) {
				}
				refreshNow();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		try {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (mService != null) {
					if (!mSeeking && mStartSeekPos >= 0) {
						mPauseButton.requestFocus();
						if (mStartSeekPos < 1000) {
							mService.prev();
						} else {
							mService.seek(0);
						}
					} else {
						scanBackward(-1,
								event.getEventTime() - event.getDownTime());
						mPauseButton.requestFocus();
						mStartSeekPos = -1;
					}
				}
				mSeeking = false;
				mPosOverride = -1;
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (mService != null) {
					if (!mSeeking && mStartSeekPos >= 0) {
						mPauseButton.requestFocus();
						mService.next();
					} else {
						scanForward(-1,
								event.getEventTime() - event.getDownTime());
						mPauseButton.requestFocus();
						mStartSeekPos = -1;
					}
				}
				mSeeking = false;
				mPosOverride = -1;
				return true;
			}
		} catch (RemoteException ex) {
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean useDpadMusicControl() {
		if (mDeviceHasDpad
				&& (mPrevButton.isFocused() || mNextButton.isFocused() || mPauseButton
						.isFocused())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int direction = -1;
		int repcnt = event.getRepeatCount();

		if ((seekmethod == 0) ? seekMethod1(keyCode) : seekMethod2(keyCode))
			return true;

		switch (keyCode) {
		/*
		 * // image scale case KeyEvent.KEYCODE_Q: av.adjustParams(-0.05, 0.0,
		 * 0.0, 0.0, 0.0,-1.0); break; case KeyEvent.KEYCODE_E: av.adjustParams(
		 * 0.05, 0.0, 0.0, 0.0, 0.0, 1.0); break; // image translate case
		 * KeyEvent.KEYCODE_W: av.adjustParams( 0.0, 0.0,-1.0, 0.0, 0.0, 0.0);
		 * break; case KeyEvent.KEYCODE_X: av.adjustParams( 0.0, 0.0, 1.0, 0.0,
		 * 0.0, 0.0); break; case KeyEvent.KEYCODE_A: av.adjustParams( 0.0,-1.0,
		 * 0.0, 0.0, 0.0, 0.0); break; case KeyEvent.KEYCODE_D: av.adjustParams(
		 * 0.0, 1.0, 0.0, 0.0, 0.0, 0.0); break; // camera rotation case
		 * KeyEvent.KEYCODE_R: av.adjustParams( 0.0, 0.0, 0.0, 0.0, 0.0,-1.0);
		 * break; case KeyEvent.KEYCODE_U: av.adjustParams( 0.0, 0.0, 0.0, 0.0,
		 * 0.0, 1.0); break; // camera translate case KeyEvent.KEYCODE_Y:
		 * av.adjustParams( 0.0, 0.0, 0.0, 0.0,-1.0, 0.0); break; case
		 * KeyEvent.KEYCODE_N: av.adjustParams( 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
		 * break; case KeyEvent.KEYCODE_G: av.adjustParams( 0.0, 0.0, 0.0,-1.0,
		 * 0.0, 0.0); break; case KeyEvent.KEYCODE_J: av.adjustParams( 0.0, 0.0,
		 * 0.0, 1.0, 0.0, 0.0); break;
		 */

		case KeyEvent.KEYCODE_SLASH:
			seekmethod = 1 - seekmethod;
			return true;

		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (!useDpadMusicControl()) {
				break;
			}
			if (!mPrevButton.hasFocus()) {
				mPrevButton.requestFocus();
			}
			scanBackward(repcnt, event.getEventTime() - event.getDownTime());
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (!useDpadMusicControl()) {
				break;
			}
			if (!mNextButton.hasFocus()) {
				mNextButton.requestFocus();
			}
			scanForward(repcnt, event.getEventTime() - event.getDownTime());
			return true;

		case KeyEvent.KEYCODE_S:
			toggleShuffle();
			return true;

		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_SPACE:
			doPauseResume();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void scanBackward(int repcnt, long delta) {
		if (mService == null)
			return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
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
					mService.prev();
					long duration = mService.duration();
					mStartSeekPos += duration;
					newpos += duration;
				}
				if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void scanForward(int repcnt, long delta) {
		if (mService == null)
			return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
				if (delta < 5000) {
					// seek at 10x speed for the first 5 seconds
					delta = delta * 10;
				} else {
					// seek at 40x after that
					delta = 50000 + (delta - 5000) * 40;
				}
				long newpos = mStartSeekPos + delta;
				long duration = mService.duration();
				if (newpos >= duration) {
					// move to next track
					mService.next();
					mStartSeekPos -= duration; // is OK to go negative
					newpos -= duration;
				}
				if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void doPauseResume() {
		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				refreshNow();
				setPauseButtonImage();
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

	private void toggleShuffle() {
		if (mService == null) {
			return;
		}
		try {
			int shuffle = mService.getShuffleMode();
			if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
				;
				mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
				if (mService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
					mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
					setRepeatButtonImage();
				}
				showToast(R.string.shuffle_on_notif);
			} else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL
					|| shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
				mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
				showToast(R.string.shuffle_off_notif);
			} else {
				Log.e("MediaPlaybackActivity", "Invalid shuffle mode: "
						+ shuffle);
			}
			setShuffleButtonImage();
		} catch (RemoteException ex) {
		}
	}

	private void cycleRepeat() {
		if (mService == null) {
			return;
		}
		try {
			int mode = mService.getRepeatMode();
			if (mode == MediaPlaybackService.REPEAT_NONE) {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
				showToast(R.string.repeat_all_notif);
			} else if (mode == MediaPlaybackService.REPEAT_ALL) {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
				if (mService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
					mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
					setShuffleButtonImage();
				}
				showToast(R.string.repeat_current_notif);
			} else {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
				showToast(R.string.repeat_off_notif);
			}
			setRepeatButtonImage();
		} catch (RemoteException ex) {
		}

	}

	private void showToast(int resid) {
		if (mToast == null) {
			mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		}
		mToast.setText(resid);
		mToast.show();
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

		updateTrackInfo();
		long next = refreshNow();
		queueNextRefresh(next);
	}

	private ServiceConnection osc = new ServiceConnection() {
		public void onServiceConnected(ComponentName classname, IBinder obj) {
			mService = IMediaPlaybackService.Stub.asInterface(obj);
			startPlayback();
			try {
				// Assume something is playing when the service says it is,
				// but also if the audio ID is valid but the service is paused.
				if (mService.getAudioId() >= 0 || mService.isPlaying()
						|| mService.getPath() != null) {
					// something is playing now, we're done
					setRepeatButtonImage();
					setShuffleButtonImage();
					setPauseButtonImage();
					return;
				}
			} catch (RemoteException ex) {
			}
			// Service is dead or not playing anything. If we got here as part
			// of a "play this file" Intent, exit. Otherwise go to the Music
			// app start screen.
			if (getIntent().getData() == null) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setClass(MediaPlaybackActivity.this,
						MusicBrowserActivity.class);
				startActivity(intent);
			}
			finish();
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService = null;
		}
	};

	private void setRepeatButtonImage() {
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
		if (mService == null)
			return;
		try {
			switch (mService.getRepeatMode()) {
			case MediaPlaybackService.REPEAT_ALL:
				mRepeatButtonImage
						.setImageResource(R.drawable.ic_mp_repeat_all_btn);
				mRepeatNormal.setImageResource(R.drawable.ic_mp_repeat_all_btn);
				mRepeatButtonText.setText(R.string.repeat_extra_all);
				mRepeatButtonText.setTextColor(getResources().getColor(
						R.color.ics));
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_repeat_all_extra", mRepeatButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_repeat_all",
							mRepeatNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"repeat_all", "color", themePackage);
					if (textColorId != 0) {
						mRepeatButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}
				}
				break;
			case MediaPlaybackService.REPEAT_CURRENT:
				mRepeatNormal
						.setImageResource(R.drawable.ic_mp_repeat_once_btn);
				mRepeatButtonImage
						.setImageResource(R.drawable.ic_mp_repeat_once_btn);
				mRepeatButtonText.setText(R.string.repeat_extra_one);
				mRepeatButtonText.setTextColor(getResources().getColor(
						R.color.android));
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_repeat_one_extra", mRepeatButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_repeat_one",
							mRepeatNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"repeat_one", "color", themePackage);
					if (textColorId != 0) {
						mRepeatButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}

				}

				break;
			default:
				mRepeatNormal.setImageResource(R.drawable.ic_mp_repeat_off_btn);
				mRepeatButtonImage
						.setImageResource(R.drawable.ic_mp_repeat_off_btn);
				mRepeatButtonText.setText(R.string.repeat_extra);
				mRepeatButtonText.setTextColor(Color.WHITE);
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_repeat_off_extra", mRepeatButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_repeat_off",
							mRepeatNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"repeat_off", "color", themePackage);
					if (textColorId != 0) {
						mRepeatButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}
				}
				break;
			}
		} catch (RemoteException ex) {
		}
	}

	private void setShuffleButtonImage() {
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
		if (mService == null)
			return;
		try {
			switch (mService.getShuffleMode()) {
			case MediaPlaybackService.SHUFFLE_NONE:
				mShuffleNormal
						.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
				mShuffleButtonImage
						.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
				mShuffleButtonText.setTextColor(Color.WHITE);
				mShuffleButtonText.setText(R.string.shuffle_extra);
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_shuffle_extra_off", mShuffleButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_shuffle_off",
							mShuffleNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"shuffle_off", "color", themePackage);
					if (textColorId != 0) {
						mShuffleButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}
				}
				break;
			case MediaPlaybackService.SHUFFLE_AUTO:
				mShuffleNormal
						.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
				mShuffleButtonImage
						.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
				mShuffleButtonText.setTextColor(getResources().getColor(
						R.color.android));
				mShuffleButtonText.setText(R.string.party_shuffle);
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_party_shuffle_extra", mShuffleButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_party_shuffle",
							mShuffleNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"party_shuffle", "color", themePackage);
					if (textColorId != 0) {
						mShuffleButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}
				}
				break;
			default:
				mShuffleNormal
						.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
				mShuffleButtonImage
						.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
				mShuffleButtonText.setTextColor(getResources().getColor(
						R.color.ics));
				mShuffleButtonText.setText(R.string.shuffle_on_notif);
				if (themeResources != null) {
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage,
							"np_shuffle_extra_on", mShuffleButtonImage,
							THEME_ITEM_FOREGROUND);
					ArtistAlbumBrowserActivity.loadThemeResource(
							themeResources, themePackage, "np_shuffle_on",
							mShuffleNormal, THEME_ITEM_FOREGROUND);
					int textColorId = themeResources.getIdentifier(
							"shuffle_on", "color", themePackage);
					if (textColorId != 0) {
						mShuffleButtonText.setTextColor(themeResources
								.getColor(textColorId));
					}
				}

				break;
			}
		} catch (RemoteException ex) {
		} catch (NullPointerException ex) {

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
				mPauseButton.setImageResource(R.drawable.ic_media_pause);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "np_pause", mPauseButton,
						THEME_ITEM_FOREGROUND);
			} else {
				mPauseButton
						.setImageResource(R.drawable.ic_appwidget_music_play);
				ArtistAlbumBrowserActivity.loadThemeResource(themeResources,
						themePackage, "np_play", mPauseButton,
						THEME_ITEM_FOREGROUND);
			}
		} catch (RemoteException ex) {
		}
	}

	private ImageView mAlbum;
	private ImageView mAlbumDummy;
	private TextView mCurrentTime;
	private TextView mTotalTime;
	private TextView mAlbumName;
	private TextView mTrackName;
	private TextView mArtistName;
	private ProgressBar mProgress;
	private long mPosOverride = -1;
	private boolean mFromTouch = false;
	private long mDuration;
	private int seekmethod;
	private boolean paused;

	private static final int REFRESH = 1;
	private static final int QUIT = 2;
	private static final int GET_ALBUM_ART = 3;
	private static final int ALBUM_ART_DECODED = 4;

	private void queueNextRefresh(long delay) {
		if (!paused) {
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {
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
		if (mService == null)
			return 500;
		try {
			long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
			long remaining = 1000 - (pos % 1000);
			if ((pos >= 0) && (mDuration > 0)) {
				mCurrentTime.setText(MusicUtils
						.makeTimeString(this, pos / 1000));

				if (mService.isPlaying()) {
					mCurrentTime.setTextColor(Color.WHITE);
					if (themeResources != null) {
						int textColorId = themeResources.getIdentifier(
								"current_time_color", "color", themePackage);
						if (textColorId != 0) {
							mCurrentTime.setTextColor(themeResources
									.getColor(textColorId));
						}
						int totalColor = themeResources.getIdentifier(
								"remaining_time_color", "color", themePackage);
						if (totalColor != 0) {
							mTotalTime.setTextColor(themeResources
									.getColor(totalColor));
						}
					}

				} else {

					// blink the counter
					int col = mCurrentTime.getCurrentTextColor();
					mCurrentTime
							.setTextColor(col == Color.WHITE ? getResources()
									.getColor(R.color.ics) : Color.WHITE);
					if (themeResources != null) {
						int textColorId = themeResources.getIdentifier(
								"current_time_color_paused", "color",
								themePackage);
						if (textColorId != 0) {
							mCurrentTime.setTextColor(themeResources
									.getColor(textColorId));
						}
						int totalColor = themeResources.getIdentifier(
								"remaining_time_color_paused", "color",
								themePackage);
						if (totalColor != 0) {
							mTotalTime.setTextColor(themeResources
									.getColor(totalColor));
						}
					}
					remaining = 500;
				}

				mProgress.setProgress((int) (1000 * pos / mDuration));
			} else {
				mCurrentTime.setText("--:--");
				mProgress.setProgress(1000);
			}
			// return the number of milliseconds until the next full second, so
			// the counter can be updated at just the right time
			return remaining;
		} catch (RemoteException ex) {
		}
		return 500;
	}

	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ALBUM_ART_DECODED:
				mAlbum.setImageBitmap((Bitmap) msg.obj);
				mAlbum.getDrawable().setDither(true);
				mAlbumDummy.setImageBitmap((Bitmap) msg.obj);
				mAlbumDummy.getDrawable().setDither(true);

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "0");
				if (np_animation_ui_db.equals("0")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_0));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "1");
				if (np_animation_ui_db.equals("1")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_1));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "2");
				if (np_animation_ui_db.equals("2")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_2));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "3");
				if (np_animation_ui_db.equals("3")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_3));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "4");
				if (np_animation_ui_db.equals("4")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_4));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "5");
				if (np_animation_ui_db.equals("5")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), R.anim.anim_5));
				}

				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "6");
				if (np_animation_ui_db.equals("6")) {
					mAlbum.startAnimation(AnimationUtils.loadAnimation(
							getBaseContext(), android.R.anim.fade_in));
				}
				np_animation_ui_db = mPreferences.getString(
						"np_animation_ui_db", "7");
				if (np_animation_ui_db.equals("7")) {
					mAlbum.clearAnimation();

				}
				break;

			case REFRESH:
				long next = refreshNow();
				queueNextRefresh(next);
				break;

			case QUIT:
				// This can be moved back to onCreate once the bug that prevents
				// Dialogs from being started from onCreate/onResume is fixed.
				new AlertDialog.Builder(MediaPlaybackActivity.this)
						.setTitle(R.string.service_start_error_title)
						.setMessage(R.string.service_start_error_msg)
						.setPositiveButton(R.string.service_start_error_button,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										finish();
									}
								}).setCancelable(false).show();
				break;

			default:
				break;
			}
		}
	};

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MediaPlaybackService.META_CHANGED)) {
				// redraw the artist/title info and
				// set new max for progress bar
				updateTrackInfo();
				setPauseButtonImage();
				queueNextRefresh(1);
			} else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
				setPauseButtonImage();
			}
		}
	};

	private static class AlbumSongIdWrapper {
		public long albumid;
		public long songid;

		AlbumSongIdWrapper(long aid, long sid) {
			albumid = aid;
			songid = sid;
		}
	}

	private void updateTrackInfo() {
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
		if (mService == null) {
			return;
		}
		try {
			String path = mService.getPath();
			if (path == null) {
				finish();
				return;
			}

			long songid = mService.getAudioId();
			if (songid < 0 && path.toLowerCase().startsWith("http://")) {
				// Once we can get album art and meta data from MediaPlayer, we
				// can show that info again when streaming.
				((View) mAlbumName.getParent()).setVisibility(View.INVISIBLE);
				mTrackName.setText(path);
				mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
				mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
						new AlbumSongIdWrapper(-1, -1)).sendToTarget();
			} else {
				((View) mAlbumName.getParent()).setVisibility(View.VISIBLE);
				String artistName = mService.getArtistName();
				if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
					artistName = getString(R.string.unknown_artist_name);
				}
				String albumName = mService.getAlbumName();
				long albumid = mService.getAlbumId();
				if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
					albumName = getString(R.string.unknown_album_name);
					albumid = -1;
				}
				try {
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						mAlbumName.setText(albumName);
						mArtistName.setText(artistName);
					} else {
						mAlbumName.setText(albumName + " by " + artistName);
					}
				} catch (NullPointerException e) {
				}
				mTrackName.setText(mService.getTrackName());
				mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
				mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
						new AlbumSongIdWrapper(albumid, songid)).sendToTarget();

				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					if (themeResources != null) {
						int trackColor = themeResources.getIdentifier(
								"track_color_land", "color", themePackage);
						if (trackColor != 0) {
							mTrackName.setTextColor(themeResources
									.getColor(trackColor));
						}
						int artistColor = themeResources.getIdentifier(
								"artist_color_land", "color", themePackage);
						if (artistColor != 0) {
							mArtistName.setTextColor(themeResources
									.getColor(artistColor));
						}
						int albumColor = themeResources.getIdentifier(
								"album_color_land", "color", themePackage);
						if (albumColor != 0) {
							mAlbumName.setTextColor(themeResources
									.getColor(albumColor));
						}
					}
				} else {
					if (themeResources != null) {
						int trackColor = themeResources.getIdentifier(
								"track_name_color", "color", themePackage);
						if (trackColor != 0) {
							mTrackName.setTextColor(themeResources
									.getColor(trackColor));
						}
						int artistColor = themeResources.getIdentifier(
								"artist_name_color", "color", themePackage);
						if (artistColor != 0) {
							mArtistName.setTextColor(themeResources
									.getColor(artistColor));
						}
						int albumColor = themeResources.getIdentifier(
								"album_name_color", "color", themePackage);
						if (albumColor != 0) {
							mAlbumName.setTextColor(themeResources
									.getColor(albumColor));
						}
					}
				}
			}
			mDuration = mService.duration();
			mTotalTime.setText(MusicUtils
					.makeTimeString(this, mDuration / 1000));
		} catch (RemoteException ex) {
			finish();
		}
	}

	public class AlbumArtHandler extends Handler {
		private long mAlbumId = -1;

		public AlbumArtHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
			long songid = ((AlbumSongIdWrapper) msg.obj).songid;
			if (msg.what == GET_ALBUM_ART
					&& (mAlbumId != albumid || albumid < 0)) {
				// while decoding the new image, show the default album art
				Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
				mHandler.removeMessages(ALBUM_ART_DECODED);
				mHandler.sendMessageDelayed(numsg, 300);
				// Don't allow default artwork here, because we want to fall
				// back to song-specific
				// album art if we can't find anything for the album.
				Bitmap bm = MusicUtils.getArtwork(MediaPlaybackActivity.this,
						songid, albumid, false);
				if (bm == null) {
					bm = MusicUtils.getArtwork(MediaPlaybackActivity.this,
							songid, -1);
					albumid = -1;
				}
				if (bm != null) {
					numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
					mHandler.removeMessages(ALBUM_ART_DECODED);
					mHandler.sendMessage(numsg);
				}
				mAlbumId = albumid;
			}
		}
	}

	private static class Worker implements Runnable {
		private final Object mLock = new Object();
		private Looper mLooper;

		/**
		 * Creates a worker thread with the given name. The thread then runs a
		 * {@link android.os.Looper}.
		 * 
		 * @param name
		 *            A name for the new thread
		 */
		Worker(String name) {
			Thread t = new Thread(null, this, name);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			synchronized (mLock) {
				while (mLooper == null) {
					try {
						mLock.wait();
					} catch (InterruptedException ex) {
					}
				}
			}
		}

		public Looper getLooper() {
			return mLooper;
		}

		public void run() {
			synchronized (mLock) {
				Looper.prepare();
				mLooper = Looper.myLooper();
				mLock.notifyAll();
			}
			Looper.loop();
		}

		public void quit() {
			mLooper.quit();
		}
	}

	@Override
	public void shakingStarted() {
		shake_actions_db = mPreferences.getString("shake_actions_db", "0");
		if (shake_actions_db.equals("0")) {
			// NONE
		}
		shake_actions_db = mPreferences.getString("shake_actions_db", "1");
		if (shake_actions_db.equals("1")) {
			doPauseResume();
		}
		shake_actions_db = mPreferences.getString("shake_actions_db", "2");
		if (shake_actions_db.equals("2")) {
			doNext();

		}
		shake_actions_db = mPreferences.getString("shake_actions_db", "3");
		if (shake_actions_db.equals("3")) {
			doPrev();

		}
		shake_actions_db = mPreferences.getString("shake_actions_db", "4");
		if (shake_actions_db.equals("4")) {
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
		shake_actions_db = mPreferences.getString("shake_actions_db", "5");
		if (shake_actions_db.equals("5")) {
			MusicUtils.togglePartyShuffle();

		}
	}

	@Override
	public void shakingStopped() {

	}

	@Override
	public void onBackPressed() {
		Intent intent;
		intent = new Intent();
		intent.setClass(this, MusicBrowserActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
		startActivity(intent);
		finish();
	}
}
