package com.hotteam67.firebaseviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

public class PreferencesActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        View finalView = getLayoutInflater().inflate(R.layout.actionbar_preferences, null);
        finalView.setLayoutParams(new Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.MATCH_PARENT
        ));
        getSupportActionBar().setCustomView(finalView);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        finalView.findViewById(R.id.backButton).setOnClickListener(v -> finish());


        prefs = new SimplePreferences();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container
                        , prefs).commit();
    }

    SimplePreferences prefs;

    public static class SimplePreferences extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i)
            {
                Log.d("HotTeam67", "Loaded preference:" + i);
                Preference pref = getPreferenceScreen().getPreference(i);
                if (pref instanceof EditTextPreference)
                {
                    EditTextPreference etp = (EditTextPreference) pref;
                    //pref.setSummary(etp.getText());
                }
            }
        }
    }

    protected void onResume()
    {
        super.onResume();
        prefs.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    protected void onPause()
    {
        super.onPause();
        prefs.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        Preference pref = prefs.findPreference(key);
        if (pref instanceof EditTextPreference)
        {
            EditTextPreference etp = (EditTextPreference) pref;
            //pref.setSummary(etp.getText());
        }
    }
}