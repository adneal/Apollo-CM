/**
 * 
 */

package com.andrew.apollo.preferences;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.Constants;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.activities.AudioPlayerHolder;
import com.andrew.apollo.activities.MusicLibrary;
import com.andrew.apollo.service.ApolloService;
import com.andrew.apollo.service.ServiceToken;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.androidquery.AQuery;

/**
 * @author Andrew Neal FIXME - Work on the IllegalStateException thrown when
 *         using PreferenceFragment and theme chooser
 */
@SuppressWarnings("deprecation")
public class SettingsHolder extends PreferenceActivity implements ServiceConnection, Constants {

    // Service
    private ServiceToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This should be called first thing
        super.onCreate(savedInstanceState);

        // Load settings XML
        int preferencesResId = R.xml.settings;
        addPreferencesFromResource(preferencesResId);

        // Load the theme chooser
        initThemeChooser();

        // ActionBar
        initActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    /**
     * Update the ActionBar as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the ActionBar
            initActionBar();
        }

    };

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        filter.addAction(ApolloService.QUEUE_CHANGED);
        filter.addAction(ApolloService.PLAYSTATE_CHANGED);

        registerReceiver(mMediaStatusReceiver, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);

        unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

    /**
     * Update the ActionBar
     */
    public void initActionBar() {
        // Custom ActionBar layout
        View view = getLayoutInflater().inflate(R.layout.custom_action_bar, null);
        // Show the ActionBar
        getActionBar().setCustomView(view);
        getActionBar().setTitle(R.string.settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        ImageView mAlbumArt = (ImageView)view.findViewById(R.id.action_bar_album_art);
        TextView mTrackName = (TextView)view.findViewById(R.id.action_bar_track_name);
        TextView mAlbumName = (TextView)view.findViewById(R.id.action_bar_album_name);

        String url = ApolloUtils.getImageURL(MusicUtils.getAlbumName(), ALBUM_IMAGE, this);
        AQuery aq = new AQuery(this);
        mAlbumArt.setImageBitmap(aq.getCachedImage(url));

        mTrackName.setText(MusicUtils.getTrackName());
        mAlbumName.setText(MusicUtils.getAlbumName());

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                context.startActivity(new Intent(context, AudioPlayerHolder.class));
                finish();
            }
        });
    }

    /**
     * @param v
     */
    public void applyTheme(View v) {
        ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
        String packageName = themePreview.getValue().toString();
        ThemeUtils.setThemePackageName(this, packageName);
        Intent intent = new Intent();
        intent.setClass(this, MusicLibrary.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * @param v
     */
    public void getThemes(View v) {
        Uri marketUri = Uri
                .parse("https://market.android.com/search?q=ApolloThemes&c=apps&featured=APP_STORE_SEARCH");
        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);
        startActivity(marketIntent);
        finish();
    }

    /**
     * Set up the theme chooser
     */
    public void initThemeChooser() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        String themePackage = sp.getString(THEME_PACKAGE_NAME, APOLLO);
        ListPreference themeLp = (ListPreference)findPreference(THEME_PACKAGE_NAME);
        themeLp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
                themePreview.setTheme(newValue.toString());
                return false;
            }
        });

        Intent intent = new Intent("com.andrew.apollo.THEMES");
        intent.addCategory("android.intent.category.DEFAULT");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> themes = pm.queryIntentActivities(intent, 0);
        String[] entries = new String[themes.size() + 1];
        String[] values = new String[themes.size() + 1];
        entries[0] = APOLLO;
        values[0] = APOLLO;
        for (int i = 0; i < themes.size(); i++) {
            String appPackageName = (themes.get(i)).activityInfo.packageName.toString();
            String themeName = (themes.get(i)).loadLabel(pm).toString();
            entries[i + 1] = themeName;
            values[i + 1] = appPackageName;
        }
        themeLp.setEntries(entries);
        themeLp.setEntryValues(values);
        ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
        themePreview.setTheme(themePackage);
    }
}
