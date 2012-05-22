/**
 * 
 */

package com.andrew.apollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.andrew.apollo.service.ApolloService;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

/**
 * @author Andrew Neal
 */
public class BottomActionBarControlsFragment extends Fragment {

    private ImageButton mRepeat, mPrev, mPlay, mNext, mShuffle;

    private ImageView mDivider;

    // Notify if repeat or shuffle changes
    private Toast mToast;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bottom_action_bar_controls, container, false);

        mRepeat = (ImageButton)root.findViewById(R.id.bottom_action_bar_repeat);
        mRepeat.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cycleRepeat();
            }
        });

        mPrev = (ImageButton)root.findViewById(R.id.bottom_action_bar_previous);
        mPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (MusicUtils.mService == null)
                    return;
                try {
                    if (MusicUtils.mService.position() < 2000) {
                        MusicUtils.mService.prev();
                    } else {
                        MusicUtils.mService.seek(0);
                        MusicUtils.mService.play();
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        mPlay = (ImageButton)root.findViewById(R.id.bottom_action_bar_play);
        mPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doPauseResume();
            }
        });

        mNext = (ImageButton)root.findViewById(R.id.bottom_action_bar_next);
        mNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (MusicUtils.mService == null)
                    return;
                try {
                    MusicUtils.mService.next();
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        mShuffle = (ImageButton)root.findViewById(R.id.bottom_action_bar_shuffle);
        mShuffle.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                toggleShuffle();
            }
        });

        mDivider = (ImageView)root.findViewById(R.id.bottom_action_bar_control_divider);
        // Theme chooser
        ThemeUtils.setImageButton(getActivity(), mPrev, "apollo_previous");
        ThemeUtils.setImageButton(getActivity(), mNext, "apollo_next");
        ThemeUtils.setBackgroundColor(getActivity(), mDivider, "bottom_action_bar_info_divider");
        return root;
    }

    /**
     * Update everything as the meta or playstate changes
     */
    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPauseButtonImage();
            setShuffleButtonImage();
            setRepeatButtonImage();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(ApolloService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mStatusListener);
    }

    /**
     * Cycle repeat states
     */
    private void cycleRepeat() {
        if (MusicUtils.mService == null) {
            return;
        }
        try {
            int mode = MusicUtils.mService.getRepeatMode();
            if (mode == ApolloService.REPEAT_NONE) {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_ALL);
                ApolloUtils.showToast(R.string.repeat_all, mToast, getActivity());
            } else if (mode == ApolloService.REPEAT_ALL) {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_CURRENT);
                if (MusicUtils.mService.getShuffleMode() != ApolloService.SHUFFLE_NONE) {
                    MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NONE);
                    setShuffleButtonImage();
                }
                ApolloUtils.showToast(R.string.repeat_one, mToast, getActivity());
            } else {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_NONE);
                ApolloUtils.showToast(R.string.repeat_off, mToast, getActivity());
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Play and pause music
     */
    private void doPauseResume() {
        try {
            if (MusicUtils.mService != null) {
                if (MusicUtils.mService.isPlaying()) {
                    MusicUtils.mService.pause();
                } else {
                    MusicUtils.mService.play();
                }
            }
            setPauseButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the shuffle mode
     */
    private void toggleShuffle() {
        if (MusicUtils.mService == null) {
            return;
        }
        try {
            int shuffle = MusicUtils.mService.getShuffleMode();
            if (shuffle == ApolloService.SHUFFLE_NONE) {
                MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NORMAL);
                if (MusicUtils.mService.getRepeatMode() == ApolloService.REPEAT_CURRENT) {
                    MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_ALL);
                    setRepeatButtonImage();
                }
                ApolloUtils.showToast(R.string.shuffle_on, mToast, getActivity());
            } else if (shuffle == ApolloService.SHUFFLE_NORMAL
                    || shuffle == ApolloService.SHUFFLE_AUTO) {
                MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NONE);
                ApolloUtils.showToast(R.string.shuffle_off, mToast, getActivity());
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the repeat images
     */
    private void setRepeatButtonImage() {
        if (MusicUtils.mService == null)
            return;
        try {
            switch (MusicUtils.mService.getRepeatMode()) {
                case ApolloService.REPEAT_ALL:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_all);
                    break;
                case ApolloService.REPEAT_CURRENT:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_one);
                    break;
                default:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_normal);
                    // Theme chooser
                    ThemeUtils.setImageButton(getActivity(), mRepeat, "apollo_repeat_normal");
                    break;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the shuffle images
     */
    private void setShuffleButtonImage() {
        if (MusicUtils.mService == null)
            return;
        try {
            switch (MusicUtils.mService.getShuffleMode()) {
                case ApolloService.SHUFFLE_NONE:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_normal);
                    // Theme chooser
                    ThemeUtils.setImageButton(getActivity(), mShuffle, "apollo_shuffle_normal");
                    break;
                case ApolloService.SHUFFLE_AUTO:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_on);
                    break;
                default:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_on);
                    break;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the play and pause image
     */
    private void setPauseButtonImage() {
        try {
            if (MusicUtils.mService != null && MusicUtils.mService.isPlaying()) {
                mPlay.setImageResource(R.drawable.apollo_holo_light_pause);
                // Theme chooser
                ThemeUtils.setImageButton(getActivity(), mPlay, "apollo_pause");
            } else {
                mPlay.setImageResource(R.drawable.apollo_holo_light_play);
                // Theme chooser
                ThemeUtils.setImageButton(getActivity(), mPlay, "apollo_play");
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

}
