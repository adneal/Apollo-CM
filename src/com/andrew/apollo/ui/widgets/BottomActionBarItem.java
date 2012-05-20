/**
 * 
 */

package com.andrew.apollo.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.andrew.apollo.preferences.SettingsHolder;
import com.andrew.apollo.tasks.FetchAlbumImages;
import com.andrew.apollo.tasks.FetchArtistImages;
import com.andrew.apollo.utils.MusicUtils;

/**
 * @author Andrew Neal
 */
public class BottomActionBarItem extends ImageButton implements OnLongClickListener,
        OnClickListener, OnMenuItemClickListener {

    private final Context mContext;

    private static final int EFFECTS_PANEL = 0;

    public BottomActionBarItem(Context context) {
        super(context);
        mContext = context;
    }

    public BottomActionBarItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnLongClickListener(this);
        setOnClickListener(this);
        mContext = context;
    }

    public BottomActionBarItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    public boolean onLongClick(View v) {
        Toast.makeText(getContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_action_bar_item_one:
                MusicUtils.toggleFavorite();
                MusicUtils.setFavoriteImage(this);
                break;
            case R.id.bottom_action_bar_item_two:
                ((Activity)mContext).onSearchRequested();
                break;
            case R.id.bottom_action_bar_item_three:
                showPopup(v);
                break;
            default:
                break;
        }
    }

    /**
     * @param v
     */
    private void showPopup(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.overflow_library);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                mContext.startActivity(new Intent(mContext, SettingsHolder.class));
                break;
            case R.id.equalizer:
                Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getCurrentAudioId());
                ((Activity)mContext).startActivityForResult(i, EFFECTS_PANEL);
                break;
            case R.id.shuffle_all:
                // TODO Only shuffle the tracks that are shown
                shuffleAll();
                break;
            // case R.id.fetch_artwork:
            // initAlbumImages();
            // break;
            // case R.id.fetch_artist_images:
            // initArtistImages();
            // break;
            default:
                break;
        }
        return false;
    }

    /**
     * Manually re-fetch artist imgages. Maybe the user wants to update them or
     * something went wrong the first time around.
     */
    public void initArtistImages() {
        FetchArtistImages getArtistImages = new FetchArtistImages(mContext, 1);
        getArtistImages.runTask();
    }

    /**
     * Manually fetch all of the album art.
     */
    public void initAlbumImages() {
        FetchAlbumImages getAlbumImages = new FetchAlbumImages(mContext, 1);
        getAlbumImages.runTask();
    }

    /**
     * Shuffle all the tracks
     */
    public void shuffleAll() {
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] {
            BaseColumns._ID
        };
        String selection = AudioColumns.IS_MUSIC + "=1";
        String sortOrder = Audio.Media.DEFAULT_SORT_ORDER;
        Cursor cursor = MusicUtils.query(mContext, uri, projection, selection, null, sortOrder);
        if (cursor != null) {
            MusicUtils.shuffleAll(mContext, cursor);
            cursor.close();
            cursor = null;
        }
    }
}
