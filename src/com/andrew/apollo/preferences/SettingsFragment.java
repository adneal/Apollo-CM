
package com.andrew.apollo.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.andrew.apollo.Constants;
import com.andrew.apollo.R;

public class SettingsFragment extends PreferenceFragment implements Constants {

    public SettingsFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Load settings XML
        int preferencesResId = R.xml.settings;
        addPreferencesFromResource(preferencesResId);
        super.onActivityCreated(savedInstanceState);
    }
}
