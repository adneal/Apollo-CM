
package com.andrew.apollo.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Constants;
import com.andrew.apollo.R;
import com.andrew.apollo.list.fragments.GenresFragment;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.views.ViewHolderList;

/**
 * @author Andrew Neal
 */
public class GenreAdapter extends SimpleCursorAdapter implements Constants {

    private WeakReference<ViewHolderList> holderReference;

    private final int left;

    public GenreAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        // Helps center the text in the Genres tab
        left = mContext.getResources().getDimensionPixelSize(
                R.dimen.listview_items_padding_left_top);
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

        // Genre name
        String genreName = mCursor.getString(GenresFragment.mGenreNameIndex);
        holderReference.get().mViewHolderLineOne.setText(MusicUtils.parseGenreName(mContext,
                genreName));

        holderReference.get().mViewHolderLineOne.setPadding(left, 40, 0, 0);

        holderReference.get().mViewHolderImage.setVisibility(View.GONE);
        holderReference.get().mViewHolderLineTwo.setVisibility(View.GONE);

        holderReference.get().mQuickContext.setOnClickListener(showContextMenu);
        return view;
    }
}
