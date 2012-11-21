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

package com.andrew.apollo.ui.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.ui.fragments.ThemeFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.widgets.ColorSchemeDialog;

/**
 * Settings.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity {

    /**
     * Image cache
     */
    private ImageCache mImageCache;

    private PreferenceUtils mPreferences;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Get the preferences
        mPreferences = PreferenceUtils.getInstace(this);

        // Initialze the image cache
        mImageCache = ImageCache.getInstance(this);

        // UP
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Add the preferences
        addPreferencesFromResource(R.xml.settings);

        // Interface settings
        initInterface();
        // Date settings
        initData();
        // Removes the cache entries
        deleteCache();
        // About
        showOpenSourceLicenses();
        // Update the version number
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            findPreference("version").setSummary(packageInfo.versionName);
        } catch (final NameNotFoundException e) {
            findPreference("version").setSummary("?");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        MusicUtils.killForegroundService(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (MusicUtils.isPlaying() && ApolloUtils.isApplicationSentToBackground(this)) {
            MusicUtils.startBackgroundService(this);
        }
    }

    /**
     * Initializes the preferences under the "Interface" category
     */
    private void initInterface() {
        // Color scheme picker
        updateColorScheme();
        // Open the theme chooser
        openThemeChooser();
    }

    /**
     * Initializes the preferences under the "Data" category
     */
    private void initData() {
        // Only on Wi-Fi preference
        onlyOnWiFi();
        // Missing album art
        downloadMissingArtwork();
        // Missing artist images
        downloadMissingArtistImages();
        if (ApolloUtils.hasICS()) {
            // Lockscreen controls
            toggleLockscreenControls();
        }
    }

    /**
     * Shows the {@link ColorSchemeDialog} and then saves the changes.
     */
    private void updateColorScheme() {
        final Preference colorScheme = findPreference("color_scheme");
        colorScheme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ApolloUtils.showColorPicker(SettingsActivity.this);
                return true;
            }
        });
    }

    /**
     * Opens the {@link ThemeFragment}.
     */
    private void openThemeChooser() {
        final Preference themeChooser = findPreference("theme_chooser");
        themeChooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Intent themeChooserIntent = new Intent(SettingsActivity.this,
                        ThemesActivity.class);
                startActivity(themeChooserIntent);
                return true;
            }
        });
    }

    /**
     * Toggles the only on Wi-Fi preference
     */
    private void onlyOnWiFi() {
        final CheckBoxPreference onlyOnWiFi = (CheckBoxPreference)findPreference("only_on_wifi");
        onlyOnWiFi.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mPreferences.setOnlyOnWifi((Boolean)newValue);
                return true;
            }
        });
    }

    /**
     * Toggles the download missing album art preference
     */
    private void downloadMissingArtwork() {
        final CheckBoxPreference missingArtwork = (CheckBoxPreference)findPreference("album_images");
        missingArtwork.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mPreferences.setDownloadMissingArtwork((Boolean)newValue);
                return true;
            }
        });
    }

    /**
     * Toggles the download missing artist imagages preference
     */
    private void downloadMissingArtistImages() {
        final CheckBoxPreference missingArtistImages = (CheckBoxPreference)findPreference("artist_images");
        missingArtistImages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mPreferences.setDownloadMissingArtistImages((Boolean)newValue);
                return true;
            }
        });
    }

    /**
     * Toggles the lock screen controls
     */
    private void toggleLockscreenControls() {
        final CheckBoxPreference lockscreenControls = (CheckBoxPreference)findPreference("lockscreen_controls");
        lockscreenControls.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mPreferences.setLockscreenControls((Boolean)newValue);

                // Let the service know
                final Intent updateLockscreen = new Intent(SettingsActivity.this,
                        MusicPlaybackService.class);
                updateLockscreen.setAction(MusicPlaybackService.UPDATE_LOCKSCREEN);
                updateLockscreen
                        .putExtra(MusicPlaybackService.UPDATE_LOCKSCREEN, (Boolean)newValue);
                startService(updateLockscreen);
                return true;
            }
        });
    }

    /**
     * Removes all of the cache entries.
     */
    private void deleteCache() {
        final Preference deleteCache = findPreference("delete_cache");
        deleteCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new AlertDialog.Builder(SettingsActivity.this).setMessage(R.string.delete_warning)
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                mImageCache.clearCaches();
                            }
                        }).setNegativeButton(R.string.cancel, new OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
                return true;
            }
        });
    }

    /**
     * Show the open source licenses
     */
    private void showOpenSourceLicenses() {
        final Preference mOpenSourceLicenses = findPreference("open_source");
        mOpenSourceLicenses.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ApolloUtils.createOpenSourceDialog(SettingsActivity.this).show();
                return true;
            }
        });
    }
}
