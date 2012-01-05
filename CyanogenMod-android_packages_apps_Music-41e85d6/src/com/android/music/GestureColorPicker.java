package com.android.music;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class GestureColorPicker extends Activity implements
		SeekBar.OnSeekBarChangeListener {
	private static final String LOG_TAG = "DeskClock";

	private int aColor_gesture;
	private int rColor_gesture;
	private int gColor_gesture;
	private int bColor_gesture;

	SeekBar aSeekBar_gesture;
	SeekBar rSeekBar_gesture;
	SeekBar gSeekBar_gesture;
	SeekBar bSeekBar_gesture;
	FrameLayout sampleLayout_gesture;
	TextView sampleText_gesture;

	SharedPreferences mPrefs;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gesture_color_picker);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		loadPreferences();

		sampleText_gesture = (TextView) findViewById(R.id.color_picker_sample_gesture);
		sampleLayout_gesture = (FrameLayout) findViewById(R.id.colorsampleLayout_gesture);

		aSeekBar_gesture = (SeekBar) findViewById(R.id.a_seekbar_gesture);
		aSeekBar_gesture.setProgress(aColor_gesture);
		aSeekBar_gesture.setOnSeekBarChangeListener(this);

		rSeekBar_gesture = (SeekBar) findViewById(R.id.r_seekbar_gesture);
		rSeekBar_gesture.setProgress(rColor_gesture);
		rSeekBar_gesture.setOnSeekBarChangeListener(this);

		gSeekBar_gesture = (SeekBar) findViewById(R.id.g_seekbar_gesture);
		gSeekBar_gesture.setProgress(gColor_gesture);
		gSeekBar_gesture.setOnSeekBarChangeListener(this);

		bSeekBar_gesture = (SeekBar) findViewById(R.id.b_seekbar_gesture);
		bSeekBar_gesture.setProgress(bColor_gesture);
		bSeekBar_gesture.setOnSeekBarChangeListener(this);

		updateColorInSample();
	}

	@Override
	public void onStop() {
		super.onStop();
		saveColors();
	}

	private void updateColorInSample() {
		sampleText_gesture.setTextColor(Color.argb(aColor_gesture,
				rColor_gesture, gColor_gesture, bColor_gesture));
	}

	private void loadPreferences() {
		aColor_gesture = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_ALPHA_GESTURE,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_ALPHA));
		rColor_gesture = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_RED_GESTURE,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_RED));
		gColor_gesture = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_GREEN_GESTURE,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_GREEN));
		bColor_gesture = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_BLUE_GESTURE,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_BLUE));
	}

	private void saveColors() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_ALPHA_GESTURE,
				aColor_gesture);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_RED_GESTURE,
				rColor_gesture);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_GREEN_GESTURE,
				gColor_gesture);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_BLUE_GESTURE,
				bColor_gesture);
		editor.commit();
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (seekBar == aSeekBar_gesture)
			aColor_gesture = progress;
		else if (seekBar == rSeekBar_gesture)
			rColor_gesture = progress;
		else if (seekBar == gSeekBar_gesture)
			gColor_gesture = progress;
		else if (seekBar == bSeekBar_gesture)
			bColor_gesture = progress;
		else
			Log.d(LOG_TAG, "seekbar not found");

		updateColorInSample();
	}

	public void onStartTrackingTouch(SeekBar seekBar_gesture) {
	}

	public void onStopTrackingTouch(SeekBar seekBar_gesture) {
	}
}
