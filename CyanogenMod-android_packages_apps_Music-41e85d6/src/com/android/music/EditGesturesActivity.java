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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.R;

public class EditGesturesActivity extends ListActivity {
    private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_CANCELLED = 1;
    private static final int STATUS_NOT_LOADED = 2;
    private static final int STATUS_LIBRARY_ERROR = 3;

    private static final int MENU_ID_CUSTOMIZE = 0;
    private static final int MENU_ID_RESET = 1;

    private static final int CUSTOMIZE_GESTURE = 0;

    private static final int DIALOG_RESET = 0;

    //These are the expected gesture entry names to be loaded from the library
    static final String[] GESTURE_NAMES = {"PAUSE", "NEXT", "PREV", "SHUFFLE", "REPEAT"};

    static final String LIBRARY_FILENAME = "music_gestures";

    private static GestureLibrary mGestureLibrary;
    private GesturesArrayAdapter mGesturesArrayAdapter;
    private GesturesLoadTask mTask;
    private TextView mEmpty;
    private SharedPreferences mPreferences;
    private boolean mHasCustomGestures;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gestures_list);

        mGesturesArrayAdapter = new GesturesArrayAdapter(this);
        setListAdapter(mGesturesArrayAdapter);
        mEmpty = (TextView) findViewById(android.R.id.empty);

        mPreferences = getSharedPreferences(MusicSettingsActivity.PREFERENCES_FILE,
                MODE_PRIVATE);
        loadGestures();

        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        GesturePreview gestureThumbnail = (GesturePreview) info.targetView.getTag();
        TextView title = (TextView) info.targetView.findViewById(R.id.title);
        menu.setHeaderTitle(title.getText());

        menu.add(0, MENU_ID_CUSTOMIZE, 0, R.string.gestures_customize);
        if (mPreferences.getBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURE_XXX
                + gestureThumbnail.entryName, false)) {
            menu.add(0, MENU_ID_RESET, 0, R.string.gestures_reset);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo();
        final GesturePreview gesturePreview = (GesturePreview) menuInfo.targetView.getTag();

        switch (item.getItemId()) {
            case MENU_ID_CUSTOMIZE:
                Intent customizeIntent = new Intent(this, CustomizeGestureActivity.class);
                customizeIntent.putExtra("TITLE", gesturePreview.title);
                customizeIntent.putExtra("ENTRYNAME", gesturePreview.entryName);
                startActivityForResult(customizeIntent, CUSTOMIZE_GESTURE);
                return true;

            case MENU_ID_RESET:
                GestureLibrary defaultGestureLibrary = GestureLibraries.fromRawResource(this,
                        R.raw.gestures);
                if (defaultGestureLibrary.load()) {
                    mGestureLibrary.removeEntry(gesturePreview.entryName);
                    mGestureLibrary.addGesture(gesturePreview.entryName,
                            defaultGestureLibrary.getGestures(gesturePreview.entryName).get(0));
                    mGestureLibrary.save();

                    Editor editor = mPreferences.edit();
                    editor.remove(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURE_XXX
                            + gesturePreview.entryName);
                    editor.apply();

                    boolean stillHasCustomGestures = false;
                    for (String entryName : GESTURE_NAMES) {
                        if (mPreferences.getBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURE_XXX
                                + entryName, false)) {
                            stillHasCustomGestures = true;
                        }
                    }
                    if (!stillHasCustomGestures) {
                        editor.putBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURES, false);
                        editor.apply();
                    }

                    notifyGestureChanges();
                    loadGestures();
                } else {
                    Toast.makeText(this, R.string.gestures_error_loading,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESET) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.gestures_reset_all_alert).setCancelable(false)
                   .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Editor editor = mPreferences.edit();
                            editor.putBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURES, false);
                            for (String entryName : GESTURE_NAMES) {
                                editor.remove(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURE_XXX
                                        + entryName);
                            }
                            editor.apply();
                            notifyGestureChanges();
                            loadGestures();
                        }
                    })
                    .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            return builder.create();
        }
        return super.onCreateDialog(id);
    }

    static GestureLibrary getGestureLibrary() {
        return mGestureLibrary;
    }

    public void reloadGestures(View v) {
        loadGestures();
    }

    public void resetGestures(View v) {
        showDialog(DIALOG_RESET);
    }

    private void notifyGestureChanges() {
        Intent intent = new Intent(MusicSettingsActivity.ACTION_GESTURES_CHANGED);
        sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CUSTOMIZE_GESTURE) {
            Editor editor = mPreferences.edit();
            editor.apply();
            notifyGestureChanges();
            loadGestures();
        }
    }

    private void loadGestures() {
        mHasCustomGestures = mPreferences.getBoolean(MusicSettingsActivity.KEY_HAS_CUSTOM_GESTURES,
                false);
        if (mHasCustomGestures) {
            mGestureLibrary = GestureLibraries.fromPrivateFile(this, LIBRARY_FILENAME);
        } else {
            mGestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
        }

        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = (GesturesLoadTask) new GesturesLoadTask().execute(this);
    }

    private class GesturesLoadTask extends AsyncTask<Context, GesturePreview, Integer> {
        private int mThumbnailSize;
        private int mThumbnailInset;
        private int mPathColor;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mPathColor = 0xFFFFFF00;
            final float scale = getResources().getDisplayMetrics().density;
            mThumbnailInset = (int) (8 * scale + 0.5f);
            mThumbnailSize = (int) (64 * scale + 0.5f);

            findViewById(R.id.resetButton).setEnabled(false);
            findViewById(R.id.reloadButton).setEnabled(false);

            mGesturesArrayAdapter.setNotifyOnChange(false);
            mGesturesArrayAdapter.clear();
        }

        @Override
        protected Integer doInBackground(Context... context) {
            if (isCancelled()) return STATUS_CANCELLED;

            final GestureLibrary gestureLibrary = mGestureLibrary;
            if (gestureLibrary.load()) {
                final Resources resources = context[0].getResources();
                final String[] titles = resources.getStringArray(R.array.gesture_titles);
                final String[] summaries = resources.getStringArray(R.array.gesture_summaries);
                int position = 0;
                for (String name : GESTURE_NAMES) {
                    if (isCancelled()) break;

                    ArrayList<Gesture> gesturesArray = gestureLibrary.getGestures(name);
                    if (gesturesArray == null) return STATUS_LIBRARY_ERROR;
                    Gesture gesture = gesturesArray.get(0);
                    final GesturePreview gesturePreview = new GesturePreview();
                    gesturePreview.entryName = name;
                    gesturePreview.title = titles[position];
                    gesturePreview.summary = summaries[position];
                    gesturePreview.thumbnail = gesture.toBitmap(mThumbnailSize, mThumbnailSize,
                            mThumbnailInset, mPathColor);
                    publishProgress(gesturePreview);
                    position++;
                }

                return STATUS_SUCCESS;
            }

            return STATUS_NOT_LOADED;
        }

        @Override
        protected void onProgressUpdate(GesturePreview... values) {
            super.onProgressUpdate(values);

            final GesturesArrayAdapter adapter = mGesturesArrayAdapter;
            adapter.setNotifyOnChange(false);

            for (GesturePreview value : values) {
                adapter.add(value);
            }

            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            switch (result) {
                case STATUS_NOT_LOADED:
                    getListView().setVisibility(View.GONE);
                    mEmpty.setVisibility(View.VISIBLE);
                    mEmpty.setText(R.string.gestures_error_loading);
                    findViewById(R.id.reloadButton).setEnabled(true);
                    break;

                case STATUS_LIBRARY_ERROR:
                    getListView().setVisibility(View.GONE);
                    mEmpty.setVisibility(View.VISIBLE);
                    mEmpty.setText(R.string.gestures_error_library);
                    findViewById(R.id.resetButton).setEnabled(true);
                    break;

                default:
                    findViewById(R.id.reloadButton).setEnabled(true);
                    if (mHasCustomGestures) findViewById(R.id.resetButton).setEnabled(true);
                    break;
            }
        }
    }

    static class GesturePreview {
        String entryName;
        String title;
        String summary;
        Bitmap thumbnail;
    }

    private class GesturesArrayAdapter extends ArrayAdapter<GesturePreview> {
        private final LayoutInflater mInflater;

        public GesturesArrayAdapter(Context context) {
            super(context, R.layout.gestures_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.gestures_item, parent, false);
            } else {
                view = convertView;
            }

            TextView title = (TextView) view.findViewById(R.id.title);
            TextView summary = (TextView) view.findViewById(R.id.summary);
            ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);

            GesturePreview gesturePreview = getItem(position);
            title.setText(gesturePreview.title);
            summary.setText(gesturePreview.summary);
            thumbnail.setImageBitmap(gesturePreview.thumbnail);
            view.setTag(gesturePreview);

            return view;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
    }
}
