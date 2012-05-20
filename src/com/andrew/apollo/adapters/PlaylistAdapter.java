/**
 * 
 */

package com.andrew.apollo.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.list.fragments.PlaylistsFragment;
import com.andrew.apollo.views.ViewHolderList;

/**
 * @author Andrew Neal
 */
public class PlaylistAdapter extends SimpleCursorAdapter {

    private WeakReference<ViewHolderList> holderReference;

    public PlaylistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    /**
     * Used to quickly our the ContextMenu
     */
    private final View.OnClickListener showContextMenu = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.showContextMenu();
        }
    };

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        // ViewHolderList
        final ViewHolderList viewholder;

        if (view != null) {

            viewholder = new ViewHolderList(view);
            holderReference = new WeakReference<ViewHolderList>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderList)convertView.getTag();
        }

        String playlist_name = mCursor.getString(PlaylistsFragment.mPlaylistNameIndex);
        holderReference.get().mViewHolderLineOne.setText(playlist_name);

        // Helps center the text in the Playlist tab
        int left = mContext.getResources().getDimensionPixelSize(
                R.dimen.listview_items_padding_left_top);
        holderReference.get().mViewHolderLineOne.setPadding(left, 40, 0, 0);

        holderReference.get().mViewHolderImage.setVisibility(View.GONE);

        holderReference.get().mQuickContext.setOnClickListener(showContextMenu);
        return view;
    }

}
