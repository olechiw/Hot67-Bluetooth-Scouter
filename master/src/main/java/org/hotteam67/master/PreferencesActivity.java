package org.hotteam67.master;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

/**
 * Simple preferences activity, loads preferences from xml and saves them to shared preferences
 * to be accessed in MasterActivity
 */
public class PreferencesActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{

    /**
     * Makes the back button work
     *
     * @param item the android item selected, only checks if it is home
     * @return true, the event was consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
        }
        return true;
    }

    /**
     * Constructor adds a back button and loads simple UI
     *
     * @param savedInstanceState saved state is ignored
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        prefs = new SimplePreferences();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container
                        , prefs).commit();
    }

    private SimplePreferences prefs;

    /**
     * Preferences fragment with an editText that shows in the summary, for easier use
     */
    public static class SimplePreferences extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i)
            {
                Log.d("BLUETOOTH_SCOUTER_DEBUG", "Loaded preference:" + i);
                Preference pref = getPreferenceScreen().getPreference(i);
                if (pref instanceof EditTextPreference)
                {
                    EditTextPreference etp = (EditTextPreference) pref;
                    pref.setSummary(etp.getText());
                }
            }
        }
    }

    /**
     * Register change listener
     */
    protected void onResume()
    {
        super.onResume();
        prefs.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Unregister change listener
     */
    protected void onPause()
    {
        super.onPause();
        prefs.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Set the summary on change, for ease of use with edittext
     *
     * @param sharedPreferences the preferences shared between activities in the app
     * @param key               the key for the pref that changed
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        Preference pref = prefs.findPreference(key);
        if (pref instanceof EditTextPreference)
        {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        }
    }
}
