/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.widget.TextView;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Gesture;
import com.android.music.R;

public class CustomizeGestureActivity extends Activity {
    private static final float THRESHOLD = 120.0f;

    private Gesture mGesture;
    private String mEntryName;
    private View mDoneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.customize_gesture);

        Bundle bundle = getIntent().getExtras();
        mEntryName = bundle.getString("ENTRYNAME");

        TextView actionName = ((TextView) findViewById(R.id.action_name));
        actionName.setText(bundle.getString("TITLE"));

        mDoneButton = findViewById(R.id.done);

        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGesture != null) {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });

            mDoneButton.setEnabled(true);
        }
    }

    public void cancelGesture(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void replaceGesture(View v) {
        SharedPreferences preferences = getSharedPreferences(MusicSettingsActivity.PREFERENCES_FILE,
                MODE_PRIVATE);
        GestureLibrary gestureLibrary;
        if (preferences.getBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURES, false)) {
            gestureLibrary = EditGesturesActivity.getGestureLibrary();
            gestureLibrary.removeEntry(mEntryName);
            gestureLibrary.addGesture(mEntryName, mGesture);
        } else {
            String fileName = EditGesturesActivity.LIBRARY_FILENAME;
            gestureLibrary = GestureLibraries.fromPrivateFile(this, fileName);
            GestureLibrary defaultGestureLibrary = EditGesturesActivity.getGestureLibrary();
            for (String entryName : defaultGestureLibrary.getGestureEntries()) {
                if (entryName.equals(mEntryName)){
                    gestureLibrary.addGesture(mEntryName, mGesture);
                } else {
                    gestureLibrary.addGesture(entryName,
                            defaultGestureLibrary.getGestures(entryName).get(0));
                }
            }
        }
        gestureLibrary.save();
        Editor editor = preferences.edit();
        editor.putBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURES, true);
        editor.putBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURE_XXX + mEntryName, true);
        editor.apply();
        setResult(RESULT_OK);
        finish();
    }

    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mDoneButton.setEnabled(false);
            mGesture = null;
        }

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            //Ignore short strokes and horizontal swipes (for compatibility with playback controls)
            if (mGesture.getLength() < THRESHOLD ||
                    mGesture.getBoundingBox().height() < THRESHOLD) {
                overlay.clear(false);
            } else {
                mDoneButton.setEnabled(true);
            }
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }
    }
}
