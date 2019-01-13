package org.hotteam67.firebaseviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

/**
 * Preferences activity to set the connection properties of the Firebase endpoint (url, api key, etc.)
 */
public class PreferencesActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * Constructor sets up user interface
     * @param savedInstanceState previous app state after sleep, ignored
     */
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
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setCustomView(finalView);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        }
        finalView.findViewById(R.id.backButton).setOnClickListener(v -> finish());


        prefs = new SimplePreferences();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container
                        , prefs).commit();
    }

    private SimplePreferences prefs;

    /**
     * Simple preferences just loads the resource from R.xml.preferences
     */
    public static class SimplePreferences extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    /**
     * Register shared preferences
     */
    protected void onResume()
    {
        super.onResume();
        prefs.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Unregister shared preferences
     */
    protected void onPause()
    {
        super.onPause();
        prefs.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Ignore the change - just here to implement interface
     * @param sharedPreferences prefs
     * @param key specific preference key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
    }
}