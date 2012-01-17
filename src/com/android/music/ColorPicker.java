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

public class ColorPicker extends Activity implements
		SeekBar.OnSeekBarChangeListener {
	private static final String LOG_TAG = "DeskClock";

	private int aColor;
	private int rColor;
	private int gColor;
	private int bColor;

	SeekBar aSeekBar;
	SeekBar rSeekBar;
	SeekBar gSeekBar;
	SeekBar bSeekBar;
	FrameLayout sampleLayout;
	TextView sampleText;

	SharedPreferences mPrefs;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color_picker);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		loadPreferences();

		sampleText = (TextView) findViewById(R.id.color_picker_sample);
		sampleLayout = (FrameLayout) findViewById(R.id.colorsampleLayout);

		aSeekBar = (SeekBar) findViewById(R.id.a_seekbar);
		aSeekBar.setProgress(aColor);
		aSeekBar.setOnSeekBarChangeListener(this);

		rSeekBar = (SeekBar) findViewById(R.id.r_seekbar);
		rSeekBar.setProgress(rColor);
		rSeekBar.setOnSeekBarChangeListener(this);

		gSeekBar = (SeekBar) findViewById(R.id.g_seekbar);
		gSeekBar.setProgress(gColor);
		gSeekBar.setOnSeekBarChangeListener(this);

		bSeekBar = (SeekBar) findViewById(R.id.b_seekbar);
		bSeekBar.setProgress(bColor);
		bSeekBar.setOnSeekBarChangeListener(this);

		updateColorInSample();
	}

	@Override
	public void onStop() {
		super.onStop();
		saveColors();
	}

	private void updateColorInSample() {
		sampleText.setTextColor(Color.argb(aColor, rColor, gColor, bColor));
	}

	private void loadPreferences() {
		aColor = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_ALPHA,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_ALPHA));
		rColor = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_RED,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_RED));
		gColor = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_GREEN,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_GREEN));
		bColor = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SCREENSAVER_COLOR_BLUE,
				MusicSettingsActivity.DEFAULT_SCREENSAVER_COLOR_BLUE));
	}

	private void saveColors() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_ALPHA, aColor);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_RED, rColor);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_GREEN, gColor);
		editor.putInt(MusicSettingsActivity.SCREENSAVER_COLOR_BLUE, bColor);
		editor.commit();
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (seekBar == aSeekBar)
			aColor = progress;
		else if (seekBar == rSeekBar)
			rColor = progress;
		else if (seekBar == gSeekBar)
			gColor = progress;
		else if (seekBar == bSeekBar)
			bColor = progress;
		else
			Log.d(LOG_TAG, "seekbar not found");

		updateColorInSample();
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}
