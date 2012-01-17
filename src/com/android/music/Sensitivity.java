package com.android.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class Sensitivity extends Activity implements
		SeekBar.OnSeekBarChangeListener {

	private int shakeChange;
	private int flipChange;
	private int bool;

	SeekBar shake;
	SeekBar flip;
	TextView mProgressText;
	ImageView mFlip;
	ImageView mShake;

	SharedPreferences mPrefs;

	AlertDialog alert;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensitive);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		loadPreferences();

		shake = (SeekBar) findViewById(R.id.shake_sens);
		flip = (SeekBar) findViewById(R.id.flip_sens);
		mShake = (ImageView) findViewById(R.id.iv_shake_sens);
		mFlip = (ImageView) findViewById(R.id.iv_flip_sens);

		mShake.setImageResource(R.drawable.ic_shake);
		mFlip.setImageResource(R.drawable.ic_flip);

		shake.setOnSeekBarChangeListener(this);
		flip.setOnSeekBarChangeListener(this);
		shake.setProgress(shakeChange);
		flip.setProgress(flipChange);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Restart Required");
		builder.setIcon(R.drawable.ic_dialog_alert_holo_dark);
		builder.setMessage(
				"Music will restart after you make your changes to use them immeditately.")
				.setCancelable(false)
				.setPositiveButton("Okay",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						});
		alert = builder.create();
		alert.show();
	}

	@Override
	public void onStop() {
		super.onStop();
		saveSens();
		System.exit(0);

	}

	private void saveSens() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(MusicSettingsActivity.SHAKE_SENSITIVITY, shakeChange);
		editor.putInt(MusicSettingsActivity.FLIP_SENSITIVITY, flipChange);
		editor.commit();
	}

	private void loadPreferences() {
		shakeChange = new Integer(mPrefs.getInt(
				MusicSettingsActivity.SHAKE_SENSITIVITY,
				(int) (MusicSettingsActivity.DEFAULT_SHAKE_SENS)));
		flipChange = new Integer(mPrefs.getInt(
				MusicSettingsActivity.FLIP_SENSITIVITY,
				MusicSettingsActivity.DEFAULT_FLIP_SENS));
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (seekBar == shake)
			shakeChange = progress;
		else if (seekBar == flip)
			flipChange = progress;

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

}
