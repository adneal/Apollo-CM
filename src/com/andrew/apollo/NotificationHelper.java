/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.andrew.apollo.utils.ApolloUtils;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressLint("NewApi")
public class NotificationHelper {

    /**
     * Notification ID
     */
    private static final int APOLLO_MUSIC_SERVICE = 1;

    /**
     * NotificationManager
     */
    private final NotificationManager mNotificationManager;

    /**
     * Context
     */
    private final MusicPlaybackService mService;

    /**
     * Custom notification layout
     */
    private RemoteViews mNotificationTemplate;

    /**
     * The Notification
     */
    private Notification mNotification = null;

    /**
     * API 16+ bigContentView
     */
    private RemoteViews mExpandedView;

    /**
     * Constructor of <code>NotificationHelper</code>
     * 
     * @param service The {@link Context} to use
     */
    public NotificationHelper(final MusicPlaybackService service) {
        mService = service;
        mNotificationManager = (NotificationManager)service
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification(final String albumName, final String artistName,
            final String trackName, final Long albumId, final Bitmap albumArt) {

        // Default notfication layout
        mNotificationTemplate = new RemoteViews(mService.getPackageName(),
                R.layout.notification_template_base);

        // Set up the content view
        initCollapsedLayout(trackName, artistName, albumArt);

        if (ApolloUtils.hasHoneycomb()) {
            // Notification Builder
            mNotification = new NotificationCompat.Builder(mService)
                    .setSmallIcon(R.drawable.stat_notify_music)
                    .setContentIntent(getPendingIntent())
                    .setPriority(Notification.PRIORITY_DEFAULT).setContent(mNotificationTemplate)
                    .build();
            // Control playback from the notification
            initPlaybackActions();
            if (ApolloUtils.hasJellyBean()) {
                // Expanded notifiction style
                mExpandedView = new RemoteViews(mService.getPackageName(),
                        R.layout.notification_template_expanded_base);
                mNotification.bigContentView = mExpandedView;
                // Control playback from the notification
                initExpandedPlaybackActions();
                // Set up the expanded content view
                initExpandedLayout(trackName, albumName, artistName, albumArt);
            }
            mService.startForeground(APOLLO_MUSIC_SERVICE, mNotification);
        } else {
            // FIXME: I do not understand why this happens, but the
            // NotificationCompat
            // API does not work on Gingerbread. Specifically, {@code
            // #mBuilder.setContent()} won't apply the custom RV in Gingerbread.
            // So,
            // until this is fixed I'll just use the old way.
            mNotification = new Notification();
            mNotification.contentView = mNotificationTemplate;
            mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
            mNotification.icon = R.drawable.stat_notify_music;
            mNotification.contentIntent = getPendingIntent();
            mService.startForeground(APOLLO_MUSIC_SERVICE, mNotification);
        }
    }

    /**
     * Changes the playback controls in and out of a paused state
     * 
     * @param isPlaying True if music is playing, false otherwise
     */
    public void goToIdleState(final boolean isPlaying) {
        if (mNotification == null || mNotificationManager == null) {
            return;
        }
        if (ApolloUtils.hasHoneycomb() && mNotificationTemplate != null) {
            mNotificationTemplate.setImageViewResource(R.id.notification_base_play,
                    isPlaying ? R.drawable.btn_playback_play : R.drawable.btn_playback_pause);
        }

        if (ApolloUtils.hasJellyBean() && mExpandedView != null && ApolloUtils.hasJellyBean()) {
            mExpandedView.setImageViewResource(R.id.notification_expanded_base_play,
                    isPlaying ? R.drawable.btn_playback_play : R.drawable.btn_playback_pause);
        }
        try {
            mNotificationManager.notify(APOLLO_MUSIC_SERVICE, mNotification);
        } catch (final IllegalStateException e) {
            Log.e("NotificationHelper", "goToIdleState - " + e);
            // FIXME Every so often an ISE is throw reading
            // "can't parcel recycled Bitmap". Figure out and understand why
            // this is happening, then prevent it.
        }
    }

    /**
     * Open to the now playing screen
     */
    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(mService, 0, new Intent("com.andrew.apollo.AUDIO_PLAYER")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions() {
        // Play and pause
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play,
                retreivePlaybackActions(1));

        // Skip tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next,
                retreivePlaybackActions(2));

        // Previous tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous,
                retreivePlaybackActions(3));

        // Stop and collapse the notification
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse,
                retreivePlaybackActions(4));

        // Update the play button image
        mExpandedView.setImageViewResource(R.id.notification_expanded_base_play,
                R.drawable.btn_playback_pause);
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions() {
        // Play and pause
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_play,
                retreivePlaybackActions(1));

        // Skip tracks
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_next,
                retreivePlaybackActions(2));

        // Previous tracks
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_previous,
                retreivePlaybackActions(3));

        // Stop and collapse the notification
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_collapse,
                retreivePlaybackActions(4));

        // Update the play button image
        mNotificationTemplate.setImageViewResource(R.id.notification_base_play,
                R.drawable.btn_playback_pause);
    }

    /**
     * @param which Which {@link PendingIntent} to return
     * @return A {@link PendingIntent} ready to control playback
     */
    private final PendingIntent retreivePlaybackActions(final int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(mService, MusicPlaybackService.class);
        switch (which) {
            case 1:
                // Play and pause
                action = new Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 1, action, 0);
                return pendingIntent;
            case 2:
                // Skip tracks
                action = new Intent(MusicPlaybackService.NEXT_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 2, action, 0);
                return pendingIntent;
            case 3:
                // Previous tracks
                action = new Intent(MusicPlaybackService.PREVIOUS_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 3, action, 0);
                return pendingIntent;
            case 4:
                // Stop and collapse the notification
                action = new Intent(MusicPlaybackService.STOP_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 4, action, 0);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void initCollapsedLayout(final String trackName, final String artistName,
            final Bitmap albumArt) {
        // Track name (line one)
        mNotificationTemplate.setTextViewText(R.id.notification_base_line_one, trackName);
        // Artist name (line two)
        mNotificationTemplate.setTextViewText(R.id.notification_base_line_two, artistName);
        // Album art
        mNotificationTemplate.setImageViewBitmap(R.id.notification_base_image, albumArt);
    }

    /**
     * Sets the track name, album name, artist name, and album art in the
     * expanded layout
     */
    private void initExpandedLayout(final String trackName, final String artistName,
            final String albumName, final Bitmap albumArt) {
        // Track name (line one)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_one, trackName);
        // Album name (line two)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_two, albumName);
        // Artist name (line three)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_three, artistName);
        // Album art
        mExpandedView.setImageViewBitmap(R.id.notification_expanded_base_image, albumArt);
    }

}
