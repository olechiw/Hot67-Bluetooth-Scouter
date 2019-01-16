package org.hotteam67.firebaseviewer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.sort.SortState;
import org.hotteam67.firebaseviewer.data.DataModel;
import org.hotteam67.firebaseviewer.data.MultiFilter;
import org.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import org.hotteam67.firebaseviewer.tableview.MainTableViewListener;
import org.hotteam67.firebaseviewer.tableview.MultiFilterTableView;

import org.hotteam67.common.Constants;
import org.hotteam67.common.DarkNumberPicker;
import org.hotteam67.common.InterceptAllLayout;
import org.hotteam67.common.TBAHandler;

import java.util.List;


/**
 * The Viewer's main activity, loads all of the user input, handles populating the data model and
 * calling all of the APIs like FireBase and TBA, and calls the functions to load/unload the data
 * from disk. Links to the other two activities and uses their results. Basically everything is put
 * together here.
 */
public class ViewerActivity extends AppCompatActivity {

    private MultiFilterTableView averagesTable;
    private MultiFilter averagesFilter;
    private MultiFilterTableView maximumsTable;
    private MultiFilter maximumsFilter;

    private ImageButton refreshButton;
    private ImageButton clearButton;
    private Button teamsGroupButton;

    private View teamsGroupView;

    private ProgressBar progressBar;

    private DarkNumberPicker teamsGroupInput;
    private Spinner teamsGroupType;

    /**
     * Result for the raw data activity, to show a specific match if requested
     * @param requestCode the key for the activity, should make sure it matches RawDataActivity
     * @param resultCode the result, to be checked for having a match number
     * @param data the other data attached to the intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == Constants.RawDataRequestCode)
        {
            if (data == null) return;
            try
            {
                Integer result = data.getIntExtra("Match Number", 0);
                teamsGroupInput.setValue(result);
                teamsGroupType.setSelection(0);
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }
        }
        else if (requestCode == Constants.PreferencesRequestCode)
        {
            RefreshConnectionProperties();
        }
    }

    /**
     * Construct the user interface, populate the calculated data if local values are found, and bind
     * event handlers
     * @param savedInstanceState ignored
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ActionBar bar = getSupportActionBar();

        View finalView = getLayoutInflater().inflate(
                R.layout.actionbar_main,
                null);
        finalView.setLayoutParams(new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT));
        if (bar != null)
        {
            bar.setCustomView(finalView);
            bar.setDisplayShowCustomEnabled(true);
        }


        progressBar = finalView.findViewById(R.id.indeterminateBar);

        ImageButton settingsButton = finalView.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> OnSettingsButton());

        finalView.findViewById(R.id.calculationButton).setOnClickListener(this::OnCalculationButton);

        refreshButton = finalView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(view -> UpdateUINetwork());

        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v ->
        {
            RemoveAlliances();
            teamsGroupButton.setText(getResources().getString(R.string.show_teams_button_label));
            RemoveAllFilters();
            teamsGroupInput.setValue(0);
            UpdateUI();
            averagesTable.sortColumn(0, SortState.ASCENDING);
            maximumsTable.sortColumn(0, SortState.ASCENDING);
            clearButton.setVisibility(View.INVISIBLE);
        });
        clearButton.setVisibility(View.INVISIBLE);

        teamsGroupView = findViewById(R.id.teamsGroupView);
        teamsGroupView.setVisibility(View.GONE);
        teamsGroupInput = findViewById(R.id.teamsGroupInput);
        teamsGroupInput.setOnValueChangedListener(this::UpdateTeamsGroup);
        teamsGroupType = findViewById(R.id.teamsGroupType);
        teamsGroupType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                UpdateTeamsGroup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        teamsGroupButton = findViewById(R.id.teamsGroupButton);
        teamsGroupButton.setOnClickListener(v -> {
            if (teamsGroupView.getVisibility() == View.GONE)
                expand(teamsGroupView);
            else
                collapse(teamsGroupView);
        });
        InterceptAllLayout tableViewFrame = findViewById(R.id.tableViewFrame);
        // Intercept all table touches when the teamsgroupview is shown
        tableViewFrame.setInterceptCondition(() -> (teamsGroupView.getVisibility() == View.VISIBLE));
        // Hide the view after the interception of a touch
        tableViewFrame.setInterceptEvent(() -> collapse(teamsGroupView));


        averagesTable = findViewById(R.id.averagesTableView);
        maximumsTable = findViewById(R.id.maximumsTableView);

        setupTableView(averagesTable);
        setupTableView(maximumsTable);

        maximumsFilter = new MultiFilter(maximumsTable);
        averagesFilter = new MultiFilter(averagesTable);

        averagesTable.SetSiblingTableView(maximumsTable);
        maximumsTable.SetSiblingTableView(averagesTable);

        RefreshConnectionProperties();

        if (ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
        {
            LoadLocal();
        }
        else
        {
            Log.d("HotTeam67", "Requesting Permissions");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.REQUEST_ENABLE_PERMISSION);
        }
    }

    /**
     * Remove the alliance highlights from the tableviews
     */
    private void RemoveAlliances()
    {
        MainTableAdapter adapter1 = (MainTableAdapter)averagesTable.getAdapter();
        MainTableAdapter adapter2 = (MainTableAdapter)maximumsTable.getAdapter();
        adapter1.clearAllianceHighlights();
        adapter2.clearAllianceHighlights();
    }

    /**
     * Setup the tableview with its adapter and the event listener
     * @param v the view to be setup
     */
    private void setupTableView(TableView v)
    {
        MainTableAdapter adapter = new MainTableAdapter(this);
        v.setAdapter(adapter);
        v.setTableViewListener(new MainTableViewListener(v, adapter));
    }

    /**
     * Do the expand animation on a view. Shamelessly copied From Stack Overflow
     * @param v the view to expand (also set to visible)
     */
    private static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * Do the collapse animation on a view. Shamelessly copied from StackOverflow
     * @param v the view to collapse (also set invisible)
     */
    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * Refresh the connection properties, redownload all raw data, and recalculate everything.
     */
    private void RefreshConnectionProperties()
    {
        DataModel.Setup(GetConnectionProperties(), new DataModel.DataLoadEvent()
        {
            @Override
            public void OnBeginProgress()
            {
                runOnUiThread(() -> StartProgressAnimation());
            }

            @Override
            public void OnCompleteProgress()
            {
                runOnUiThread(() -> {
                    EndProgressAnimation();
                    if (averagesTable != null && maximumsTable != null && DataModel.GetAverages() != null && DataModel.GetMaximums() != null)
                    {
                        ((MainTableAdapter) averagesTable.getAdapter()).setAllItems(DataModel.GetAverages());
                        ((MainTableAdapter) maximumsTable.getAdapter()).setAllItems(DataModel.GetMaximums());
                        UpdateUI();
                        averagesTable.sortColumn(0, SortState.ASCENDING);
                        maximumsTable.sortColumn(0, SortState.ASCENDING);
                    }
                });
            }
        });
    }

    /**
     * Update the user interface from the network, including recalculating everything etc.
     */
    private void UpdateUINetwork()
    {
        DataModel.RefreshTable(() -> runOnUiThread(() -> {
            if (averagesTable != null && maximumsTable != null && DataModel.GetAverages() != null && DataModel.GetMaximums() != null) {
                ((MainTableAdapter) averagesTable.getAdapter()).setAllItems(DataModel.GetAverages());
                ((MainTableAdapter) maximumsTable.getAdapter()).setAllItems(DataModel.GetMaximums());
                UpdateUI();
            }

        }));
    }

    /**
     * Simply clears all of the filters set
     */
    private void UpdateUI()
    {
        if (teamsGroupInput.getValue() == 0)
        {
            clearButton.setVisibility(View.INVISIBLE);
            teamsGroupButton.setText(getResources().getString(R.string.show_teams_button_label));
        }
        else
            clearButton.setVisibility(View.VISIBLE);

    }

    /**
     * Load the data from local files (serialized calculated and raw values)
     */
    private void LoadLocal()
    {
        DataModel.LoadSerializedTables();
        DataModel.LoadTBADataLocal();
    }

    /**
     * Update the view based on the teams group value, such as match/alliance number
     */
    @SuppressLint("SetTextI18n")
    private void UpdateTeamsGroup()
    {
        int id = teamsGroupInput.getValue();
        MainTableAdapter adapter1 = (MainTableAdapter)averagesTable.getAdapter();
        MainTableAdapter adapter2 = (MainTableAdapter)maximumsTable.getAdapter();
        adapter1.clearAllianceHighlights();
        adapter2.clearAllianceHighlights();
        switch (teamsGroupType.getSelectedItem().toString())
        {
            case Constants.ViewerTeamsGroupTypes.TEAM:
            {
                if (id != 0) {
                    RemoveAllFilters();
                    Filter(Constants.TEAM_NUMBER_COLUMN, String.valueOf(id), true);
                    teamsGroupButton.setText("Team: " + id);
                }
                else
                {
                    RemoveAllFilters();
                    averagesTable.sortColumn(0, SortState.ASCENDING);
                    maximumsTable.sortColumn(0, SortState.ASCENDING);
                }
                UpdateUI();
                break;
            }
            case Constants.ViewerTeamsGroupTypes.MATCH:
                if (id == 0)
                {
                    RemoveAllFilters();
                    break;
                }
                teamsGroupButton.setText("Qual: " + id);
                TBAHandler.Match m = DataModel.GetMatch(id);

                if (m == null)
                {
                    RemoveAllFilters();
                    averagesTable.sortColumn(0, SortState.ASCENDING);
                    maximumsTable.sortColumn(0, SortState.ASCENDING);
                    break;
                }

                averagesFilter.removeFilter(Constants.TEAM_NUMBER_COLUMN);
                maximumsFilter.removeFilter(Constants.TEAM_NUMBER_COLUMN);

                for (String red : m.redTeams)
                {
                    Filter(Constants.TEAM_NUMBER_COLUMN, red);
                }
                for (String blue : m.blueTeams)
                {
                    Filter(Constants.TEAM_NUMBER_COLUMN, blue);
                }
                adapter1.setAllianceHighlights(m);
                adapter2.setAllianceHighlights(m);


                break;
            case Constants.ViewerTeamsGroupTypes.ALLIANCE:
                if (id != 0)
                {
                    teamsGroupButton.setText("Alliance: " + id);
                    RemoveAllFilters();
                    List<String> alliance = DataModel.GetAlliance(id - 1);
                    if (alliance.size() > 0)
                    {
                        RemoveAllFilters();
                        averagesTable.sortColumn(0, SortState.ASCENDING);
                        maximumsTable.sortColumn(0, SortState.ASCENDING);
                    }
                    for (String t : alliance)
                        Filter(Constants.TEAM_NUMBER_COLUMN, t);
                }
                else
                {
                    RemoveAllFilters();
                    averagesTable.sortColumn(0, SortState.ASCENDING);
                    maximumsTable.sortColumn(0, SortState.ASCENDING);
                }
                break;
        }
        UpdateUI();
    }

    /**
     * Clear all of the filters on the table views
     */
    private synchronized void RemoveAllFilters()
    {
        averagesFilter.removeFilter(Constants.TEAM_NUMBER_COLUMN);
        maximumsFilter.removeFilter(Constants.TEAM_NUMBER_COLUMN);
    }

    /**
     * Add a filter to both tables
     * @param column the column index to filter on
     * @param s the value to look for
     */
    private synchronized void Filter(int column, String s)
    {
        Filter(column, s, false);
    }

    /**
     * Add a filter to both tables, with a boolean to check whether to doContains or not
     * @param column the column index
     * @param s the string to filter on
     * @param doContains whether to doContains - whether to use .contains() or .equals()
     */
    private synchronized void Filter(int column, String s, boolean doContains)
    {
        averagesFilter.set(column, s, doContains);
        maximumsFilter.set(column, s, doContains);
    }

    /**
     * When the calculation button is pressed - show loading and recalculate everything
     * @param v the calculation button view
     */
    private synchronized void OnCalculationButton(View v)
    {


        ((Button)v).setText((((Button) v).getText().toString().equals("MAX")) ?
                "AVG" : "MAX");

        View active = GetActiveTable();
        TableView inActive = GetInactiveTable();
        active.setVisibility(View.GONE);
        inActive.setVisibility(View.VISIBLE);
        inActive.scrollToRowPosition(0);
//         UpdateUI();
    }

    /**
     * Get the active table, either averages or maximums, as these are cycled between
     * @return the active TableView
     */
    private synchronized TableView GetActiveTable()
    {
        return (averagesTable.getVisibility() == View.VISIBLE) ? averagesTable : maximumsTable;
    }

    /**
     * Get the inactive table, either averages or maximums, as these are cycled between
     * @return the inactive TableView
     */
    private synchronized TableView GetInactiveTable()
    {
        return (averagesTable.getVisibility() == View.VISIBLE) ? maximumsTable : averagesTable;
    }

    /**
     * Event handler to show the settings activity when the button is clicked
     */
    private void OnSettingsButton()
    {
        Intent settingsIntent = new Intent(this, PreferencesActivity.class);
        startActivityForResult(settingsIntent, Constants.PreferencesRequestCode);
    }

    /**
     * When disk permissions are requested, check whether they are granted and we can load locally
     * @param requestCode the request code, which should be linked to request enable permissions
     * @param permissions the permissions that were requested
     * @param grantResults the results for requested permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == Constants.REQUEST_ENABLE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                LoadLocal();
            }
        }
    }

    /**
     * Get the preferences values for connection properties like Firebase url
     * @return the connection properties, in order of appearance in XML
     */
    private String[] GetConnectionProperties()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String connectionString = (String) prefs.getAll().get("pref_connectionString");
        if (connectionString == null)
            return null;
        String[] values = connectionString.split(";");
        if (values.length != 4)
        {
            return null;
        }

        return values;
    }

    /**
     * Spin the refresh button around, by hiding it and showing an indeterminate progress bar
     */
    private void StartProgressAnimation()
    {
        refreshButton.setEnabled(false);
        refreshButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Stop spinning the refresh button around, by showing it again and hiding the indeterminate progress
     * bar
     */
    private void EndProgressAnimation()
    {
        refreshButton.setEnabled(true);
        refreshButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
}
