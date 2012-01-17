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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider1x1 extends AppWidgetProvider {
	static final String TAG = "MusicAppWidgetProvider1x1";

	public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate1x1";

	private static MediaAppWidgetProvider1x1 sInstance;

	static synchronized MediaAppWidgetProvider1x1 getInstance() {
		if (sInstance == null) {
			sInstance = new MediaAppWidgetProvider1x1();
		}
		return sInstance;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		defaultAppWidget(context, appWidgetIds);

		// Send broadcast intent to any running MediaPlaybackService so it can
		// wrap around with an immediate update.
		Intent updateIntent = new Intent(MediaPlaybackService.SERVICECMD);
		updateIntent.putExtra(MediaPlaybackService.CMDNAME,
				MediaAppWidgetProvider1x1.CMDAPPWIDGETUPDATE);
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
				appWidgetIds);
		updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		context.sendBroadcast(updateIntent);
	}

	/**
	 * Initialize given widgets to default state, where we launch Music on
	 * default click and hide actions if service not running.
	 */
	private void defaultAppWidget(Context context, int[] appWidgetIds) {
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.album_appwidget1x1);

		views.setImageViewResource(R.id.albumart, View.GONE);

		linkButtons(context, views, false /* not playing */);
		pushUpdate(context, appWidgetIds, views);
	}

	private void pushUpdate(Context context, int[] appWidgetIds,
			RemoteViews views) {
		// Update specific list of appWidgetIds if given, otherwise default to
		// all
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			gm.updateAppWidget(appWidgetIds, views);
		} else {
			gm.updateAppWidget(new ComponentName(context, this.getClass()),
					views);
		}
	}

	/**
	 * Check against {@link AppWidgetManager} if there are any instances of this
	 * widget.
	 */
	private boolean hasInstances(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(context, this.getClass()));
		return (appWidgetIds.length > 0);
	}

	/**
	 * Handle a change notification coming over from
	 * {@link MediaPlaybackService}
	 */
	void notifyChange(MediaPlaybackService service, String what) {
		if (hasInstances(service)) {
			if (MediaPlaybackService.META_CHANGED.equals(what)
					|| MediaPlaybackService.PLAYSTATE_CHANGED.equals(what)
					|| MediaPlaybackService.REPEATMODE_CHANGED.equals(what)
					|| MediaPlaybackService.SHUFFLEMODE_CHANGED.equals(what)) {
				performUpdate(service, null);
			}
		}
	}

	/**
	 * Update all active widget instances by pushing changes
	 */
	void performUpdate(MediaPlaybackService service, int[] appWidgetIds) {
		final RemoteViews views = new RemoteViews(service.getPackageName(),
				R.layout.album_appwidget1x1);

		CharSequence titleName = service.getTrackName();
		CharSequence artistName = service.getArtistName();
		long albumId = service.getAlbumId();
		long songId = service.getAudioId();
		{
			// No error, so show normal titles and artwork
			views.setTextViewText(R.id.title, titleName);
			views.setTextViewText(R.id.artist, artistName);
			views.setViewVisibility(R.id.albumart, View.VISIBLE);
			// Set album art
			Uri uri = MusicUtils.getArtworkUri(service, songId, albumId);
			if (uri != null) {
				views.setImageViewUri(R.id.albumart, uri);
			} else {
				views.setImageViewResource(R.id.albumart,
						R.drawable.albumart_mp_unknown);
			}
		}
		// Set correct drawable for pause state
		final boolean playing = service.isPlaying();
		if (playing) {
			views.setImageViewResource(R.id.play_pause,
					R.drawable.appwidget_pause_normal);
		} else {
			views.setImageViewResource(R.id.play_pause,
					R.drawable.appwidget_play_normal);
		}

		// Link actions buttons to intents
		linkButtons(service, views, playing);

		pushUpdate(service, appWidgetIds, views);
	}

	/**
	 * Link up various button actions using {@link PendingIntents}.
	 * 
	 * @param playerActive
	 *            True if player is active in background, which means widget
	 *            click will launch {@link MediaPlaybackActivity}, otherwise we
	 *            launch {@link MusicBrowserActivity}.
	 */
	private void linkButtons(Context context, RemoteViews views,
			boolean playerActive) {
		// Connect up various buttons and touch events
		Intent intent;
		PendingIntent pendingIntent;

		final ComponentName serviceName = new ComponentName(context,
				MediaPlaybackService.class);

		intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
		intent.setComponent(serviceName);
		pendingIntent = PendingIntent.getService(context,
				0 /* no requestCode */, intent, 0 /* no flags */);
		views.setOnClickPendingIntent(R.id.albumart, pendingIntent);

	}
}
