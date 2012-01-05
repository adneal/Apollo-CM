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

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFERENCES_FILE);
		addPreferencesFromResource(R.xml.settings);
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
		final String themePackage = sp.getString(THEME_KEY,
				MusicSettingsActivity.THEME_DEFAULT);
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

	public void applyTheme(View v) {
		PreviewPreference themePreview = (PreviewPreference) findPreference("themePreview");
		String packageName = themePreview.getValue().toString();
		// this time we really save the themepackagename
		SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("themePackageName", packageName);
		// and update the preferences from the theme
		// TODO:ADW maybe this should be optional for the user
		if (!packageName.equals(MusicSettingsActivity.THEME_DEFAULT)) {
			// Add stuff
		} else {
			// Add stuff
		}
		editor.commit();
		finish();
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
				} catch (RemoteException e) {
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
