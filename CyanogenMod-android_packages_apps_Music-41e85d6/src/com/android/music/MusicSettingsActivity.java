/*
 * Copyright (C) 2011 The CyanogenMod Project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MusicSettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceChangeListener {

	static final String KEY_ENABLE_FOCUS_LOSS_DUCKING = "enable_focus_loss_ducking";
	static final String KEY_DUCK_ATTENUATION_DB = "duck_attenuation_db";
	static final String KEY_BACK_BUTTON_DB = "back_button_db";
	static final String KEY_ANIMATION_UI_DB = "animation_ui_db";
	static final String KEY_ENABLE_GESTURES = "enable_gestures";
	static final String KEY_ENABLE_HAPTIC_FEEDBACK = "enable_haptic_feedback";
	static final String KEY_HAS_CUSTOM_GESTURES = "has_custom_gestures";
	static final String KEY_ENABLE_SEARCH_BUTTON = "cbSearch";
	static final String KEY_ENABLE_PLAY_BUTTON = "cbPlay";
	static final String KEY_ENABLE_NEXT_BUTTON = "cbNext";
	static final String KEY_ENABLE_PREV_BUTTON = "cbPrev";
	static final String KEY_ENABLE_NEW_PLAYLIST_BUTTON = "cbPlaylist";
	static final String KEY_ENABLE_ALBUM_ART = "cbArt";
	static final String KEY_ENABLE_SONG_TEXT = "tvLine1";
	static final String KEY_ENABLE_ARTIST_TEXT = "tvLine2";
	static final String KEY_ENABLE_ALBUM_TEXT = "tvLine3";
	static final String KEY_ENABLE_BACKGROUND_SHAKE_ACTIONS = "cbShake";
	static final String KEY_COLOR_PREFERENCE_KEY = "color";
	static final String KEY_ENABLE_STATUS_PLAY_BUTTON = "cbStatusPlay";
	static final String KEY_ENABLE_SHARE_BUTTON = "cbShare";
	static final String KEY_ENABLE_PROGRESS_BAR = "cbProgress";
	static final String KEY_ENABLE_OVER_FLOW = "cbFlow";

	static final String KEY_ENABLE_STATUS_NEXT_BUTTON = "cbStatusNext";
	static final String KEY_ENABLE_STATUS_PREV_BUTTON = "cbStatusPrev";
	static final String KEY_ENABLE_STATUS_ALBUM_ART = "cbStatusArt";
	static final String KEY_ENABLE_STATUS_SONG_TEXT = "tvStatusLine1";
	static final String KEY_ENABLE_STATUS_ARTIST_TEXT = "tvStatusLine2";
	static final String KEY_ENABLE_STATUS_ALBUM_TEXT = "tvStatusLine3";
	static final String KEY_ENABLE_MARKET_SERACH = "cbMarket";
	static final String KEY_ENABLE_STATUS_COLLAPSE = "cbStatusCollapse";
	static final String SCREENSAVER_COLOR = "screensaver_color";
	static final String SCREENSAVER_COLOR_ALPHA = "screensaver_color_alpha";
	static final String SCREENSAVER_COLOR_RED = "screensaver_color_red";
	static final String SCREENSAVER_COLOR_GREEN = "screensaver_color_green";
	static final String SCREENSAVER_COLOR_BLUE = "screensaver_color_blue";
	static final String KEY_ENTER_FULL_NOW_PLAYING = "cbEnterNowPlaying";
	static final String KEY_ENABLE_STATUS_TEXT_COLOR = "tvStatusColor";
	static final String KEY_ENABLE_HOME_ART = "cbHomeAlbumArt";
	static final String KEY_LOCK = "cbLock";
	public static final String THEME_DEFAULT = "Music";
	static final String THEME_KEY = "themePackageName";

	// This key has the gesture entry name (E.g. PAUSE) appended to it before
	// use
	static final String KEY_HAS_CUSTOM_GESTURE_XXX = "has_custom_gesture_";

	static final String DEFAULT_DUCK_ATTENUATION_DB = "8";
	static final String DEFAULT_BACK_BUTTON_ACTION_DB = "0";

	static final String ACTION_ENABLE_GESTURES_CHANGED = "com.android.music.enablegestureschanged";
	static final String ACTION_GESTURES_CHANGED = "com.android.music.gestureschanged";

	static final String PREFERENCES_FILE = "settings";
	static final String SCREENSAVER_COLOR_GESTURE = "screensaver_color_gesture";
	static final String SCREENSAVER_COLOR_ALPHA_GESTURE = "screensaver_color_alpha_gesture";
	static final String SCREENSAVER_COLOR_RED_GESTURE = "screensaver_color_red_gesture";
	static final String SCREENSAVER_COLOR_GREEN_GESTURE = "screensaver_color_green_gesture";
	static final String SCREENSAVER_COLOR_BLUE_GESTURE = "screensaver_color_blue_gesture";
	static final String SCREENSAVER_COLOR_NP = "screensaver_color_np";
	static final String SCREENSAVER_COLOR_ALPHA_NP = "screensaver_color_alpha_np";
	static final String SCREENSAVER_COLOR_RED_NP = "screensaver_color_red_np";
	static final String SCREENSAVER_COLOR_GREEN_NP = "screensaver_color_green_np";
	static final String SCREENSAVER_COLOR_BLUE_NP = "screensaver_color_blue_np";
	public static final String KEY_ENABLE_STATUS_NONYA = "cbStatusNonya";
	public static final String KEY_BUILD_VERSION = "build";
	public static final String KEY_SOUND_EFFECT = "eqEffects";
	public static final String KEY_FEEDBACK = "feedback";
	public static final String KEY_FLIP = "cbFlip";
	public static final String KEY_TICK = "cbStatusTicker";

	long[] mHits = new long[3];
	private static final String LOG_TAG = "EasterEgg";
	private static final int EFFECTS_PANEL = 0;

	// Color to use for text & graphics in screen saver mode.
	// private final int SCREEN_SAVER_COLOR = 0xFF00C0FF;
	// private final int SCREEN_SAVER_COLOR_DIM = 0xFF004880;
	static final int DEFAULT_SCREENSAVER_COLOR_ALPHA = 230;
	static final int DEFAULT_SCREENSAVER_COLOR_RED = 0;
	static final int DEFAULT_SCREENSAVER_COLOR_GREEN = 192;
	static final int DEFAULT_SCREENSAVER_COLOR_BLUE = 255;
	static final String BG_PHOTO_FILE = "home_art";
	static final String TEMP_PHOTO_FILE = "home";
	private Bitmap bgBitmap = null;

	public AlertDialog themeAlert;
	public CheckBoxPreference cp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFERENCES_FILE);
		addPreferencesFromResource(R.xml.settings);

		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		PreferenceScreen screen;
		screen = getPreferenceScreen();

		cp = (CheckBoxPreference) screen.findPreference("cbHomeAlbumArt");

		final CheckBoxPreference lk = (CheckBoxPreference) screen
				.findPreference("cbLock");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set Wallpaper");
		builder.setIcon(android.R.drawable.ic_menu_crop);
		builder.setMessage(
				"You should select a wallpaper to use when your music is paused.")
				.setCancelable(false)
				.setPositiveButton("Okay",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								pickImage();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								cp.setChecked(false);
							}
						});
		final AlertDialog alert = builder.create();

		AlertDialog.Builder lock = new AlertDialog.Builder(this);
		lock.setTitle("Requires Restart");
		lock.setIcon(R.drawable.ic_dialog_alert_holo_dark);
		lock.setMessage(
				"Music needs to stop completely and restart to let the changes take effect")
				.setCancelable(false)
				.setPositiveButton("Okay",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// This isn't a good practice, but we need to
								// restart the
								// service completely to see the change
								// immediately
								System.exit(0);
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (lk.isChecked()) {
									lk.setChecked(false);
								} else {
									lk.setChecked(true);
								}
							}
						});
		final AlertDialog lockAlert = lock.create();

		cp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				CheckBoxPreference cbp = (CheckBoxPreference) preference;
				if (cbp.isChecked()) {
					alert.show();
				} else {
					// We don't want the wall paper to remain the album art if
					// they aren't using this.
					setCustomBackground();
				}
				return true;
			}
		});

		lk.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				lockAlert.show();
				return true;
			}
		});

		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			findPreference(KEY_BUILD_VERSION).setSummary(
					Build.VERSION.RELEASE + " - " + packageInfo.versionName);
		} catch (NameNotFoundException e) {
			findPreference(KEY_BUILD_VERSION).setSummary("?");
		}
		// ADW: theme settings
		SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		final String themePackage = sp.getString(THEME_KEY, THEME_DEFAULT);
		ListPreference themeLp = (ListPreference) findPreference(THEME_KEY);
		themeLp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
				themePreview.setTheme(newValue.toString());
				return false;
			}
		});

		Intent intent = new Intent("com.andrew.music.THEMES");
		intent.addCategory("android.intent.category.DEFAULT");
		PackageManager pm = getPackageManager();
		List<ResolveInfo> themes = pm.queryIntentActivities(intent, 0);
		String[] entries = new String[themes.size() + 1];
		String[] values = new String[themes.size() + 1];
		entries[0] = MusicSettingsActivity.THEME_DEFAULT;
		values[0] = MusicSettingsActivity.THEME_DEFAULT;
		for (int i = 0; i < themes.size(); i++) {
			String appPackageName = (themes.get(i)).activityInfo.packageName
					.toString();
			String themeName = (themes.get(i)).loadLabel(pm).toString();
			entries[i + 1] = themeName;
			values[i + 1] = appPackageName;
		}
		themeLp.setEntries(entries);
		themeLp.setEntryValues(values);
		PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
		themePreview.setTheme(themePackage);
	}

	private void pickImage() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");

		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
		intent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri());
		intent.putExtra("noFaceDetection", true);

		startActivityForResult(intent, 0);
	}

	private Uri getTempUri() {
		return Uri.fromFile(getTempFile());
	}

	private File getTempFile() {
		if (isSDCARDMounted()) {

			File f = new File(Environment.getExternalStorageDirectory(),
					TEMP_PHOTO_FILE);
			// try {
			// f.createNewFile();
			// } catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// Toast.makeText(this, "Something Fucked Up",
			// Toast.LENGTH_LONG).show();
			// }
			return f;
		} else {
			return null;
		}
	}

	private boolean isSDCARDMounted() {
		String status = Environment.getExternalStorageState();

		if (status.equals(Environment.MEDIA_MOUNTED))
			return true;
		return false;
	}

	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();

		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {

			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				try {

					File src = getTempFile();
					File dst = new File(getFilesDir(), BG_PHOTO_FILE);
					copyFile(src, dst);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// Set Custom Background Image
	public void setCustomBackground() {

		SharedPreferences preferences = getSharedPreferences(
				MusicSettingsActivity.PREFERENCES_FILE, MODE_PRIVATE);

		preferences
				.getBoolean(MusicSettingsActivity.KEY_ENABLE_HOME_ART, false);

		// First clean our old data
		if (bgBitmap != null) {
			bgBitmap.recycle();
			bgBitmap = null;
			System.gc();
		}
		// now load the proper bg
		String BG_FILE = getFilesDir().toString() + File.separator
				+ MusicSettingsActivity.BG_PHOTO_FILE;
		bgBitmap = BitmapFactory.decodeFile(BG_FILE);

		try {
			WallpaperManager.getInstance(this).setBitmap(bgBitmap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void applyTheme(View v) {
		PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
		String packageName = themePreview.getValue().toString();
		// this time we really save the themepackagename
		SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		final SharedPreferences.Editor editor = sp.edit();
		editor.putString("themePackageName", packageName);
		// and update the preferences from the theme
		// TODO:ADW maybe this should be optional for the user
		if (!packageName.equals(MusicSettingsActivity.THEME_DEFAULT)) {
			Resources themeResources = null;
			try {
				themeResources = getPackageManager()
						.getResourcesForApplication(packageName.toString());
			} catch (NameNotFoundException e) {
				// e.printStackTrace();
			}
		} else {

		}
		AlertDialog.Builder theme = new AlertDialog.Builder(this);
		theme.setTitle("Requires Restart");
		theme.setIcon(R.drawable.ic_dialog_alert_holo_dark);
		theme.setMessage(
				"Music needs to stop completely and restart to let the changes take effect")
				.setCancelable(false)
				.setPositiveButton("Okay",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								editor.commit();
								// This isn't a good practice, but we need to
								// restart the
								// service completely to see the change
								// immediately
								System.exit(0);
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		themeAlert = theme.create();
		themeAlert.show();
	}

	public void getThemes(View v) {
		// TODO:warn theme devs to use "MusicTheme" as keyword.
		Uri marketUri = Uri.parse("market://search?q=MusicTheme");
		Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);
		try {
			startActivity(marketIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
			Log.e("Music", "Get themes", e);
		}
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_ENABLE_GESTURES)) {
			Intent intent = new Intent(ACTION_ENABLE_GESTURES_CHANGED);
			sendBroadcast(intent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		try {
			if (preference.getKey().equals(KEY_BUILD_VERSION)) {
				System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
				mHits[mHits.length - 1] = SystemClock.uptimeMillis();
				if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
					Toast ImageToast = new Toast(getBaseContext());
					LinearLayout toastLayout = new LinearLayout(
							getBaseContext());
					toastLayout.setOrientation(LinearLayout.HORIZONTAL);
					ImageView image = new ImageView(getBaseContext());
					image.setImageResource(R.drawable.easter_egg);
					toastLayout.addView(image);
					ImageToast.setView(toastLayout);
					ImageToast.setDuration(Toast.LENGTH_SHORT);
					ImageToast.show();
				}
			}
		} catch (NullPointerException ee) {

		}
		try {
			if (preference.getKey().equals(KEY_SOUND_EFFECT)) {
				Intent i = new Intent(
						AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
				try {
					i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
							MusicUtils.sService.getAudioSessionId());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				startActivityForResult(i, EFFECTS_PANEL);
			}
		} catch (NullPointerException ee) {

		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
