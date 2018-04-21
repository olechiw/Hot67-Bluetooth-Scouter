package com.hotteam67.firebaseviewer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.annimon.stream.Stream;
import com.evrencoskun.tableview.TableView;
import com.hotteam67.firebaseviewer.data.DataTableBuilder;
import com.hotteam67.firebaseviewer.data.ColumnSchema;
import com.hotteam67.firebaseviewer.data.DataTable;
import com.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import com.hotteam67.firebaseviewer.tableview.MainTableViewListener;
import com.hotteam67.firebaseviewer.data.Sort;
import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;
import com.hotteam67.firebaseviewer.web.FirebaseHandler;
import com.hotteam67.firebaseviewer.web.TBAHandler;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainTableAdapter tableAdapter;

    private ImageButton refreshButton;
    private Button teamsGroupButton;

    private EditText teamSearchView;

    // State for which calculation is currently in the UI
    int calculationState = DataTableBuilder.Calculation.AVERAGE;
    DataTable rawData;

    // Both tables loaded into memory, meaning faster switching but slower loading
    DataTableBuilder calculatedDataAverages;
    DataTableBuilder calculatedDataMaximums;

    // TBA-Pulled data, Rankings, Nicknames, and Schedule in sequential order
    private JSONObject teamNumbersRanks;
    private JSONObject teamNumbersNames;

    List<List<String>> alliances = new ArrayList<>();

    // Handler for the teams group filtering
    TeamsGroupHandler teamsGroupHandler;

    List<String> redTeams = new ArrayList<>();
    List<String> blueTeams = new ArrayList<>();

    public MainActivity() {}

    /*
    Result for raw data activity, load the match number if one was selected
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == Constants.RawDataRequestCode)
        {
            if (data == null) return;
            try
            {
                String result = data.getStringExtra("Match Number");
                teamsGroupHandler.SetId(Integer.valueOf(result));
                teamsGroupHandler.SetType(TeamsGroupHandler.TEAM_GROUP_QUALS);
                UpdateTeamsGroup();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /*
    Construct UI
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
        bar.setCustomView(finalView);
        bar.setDisplayShowCustomEnabled(true);


        ImageButton settingsButton = finalView.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> OnSettingsButton());

        finalView.findViewById(R.id.calculationButton).setOnClickListener(this::OnCalculationButton);

        refreshButton = finalView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(view -> RefreshTable());

        ImageButton clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v ->
        {
            SetTeamNumberFilter(Constants.EMPTY);
            teamSearchView.setText("");
            teamsGroupHandler.SetId(0);
            teamsGroupHandler.SetType(TeamsGroupHandler.TEAM_GROUP_QUALS);
            UpdateTeamsGroup();
        });

        teamSearchView = finalView.findViewById(R.id.teamNumberSearch);
        teamSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    SetTeamNumberFilter(editable.toString());
                    ShowActiveTable();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        teamsGroupButton = findViewById(R.id.teamsGroupButton);
        teamsGroupHandler = new TeamsGroupHandler(this);
        teamsGroupButton.setOnClickListener(v -> {
            View dialogView = teamsGroupHandler.GetView();
            new AlertDialog.Builder(this, android.R.style.Theme_Material_NoActionBar_Fullscreen)
                    .setTitle("Team Groups").setOnDismissListener(dialogInterface ->
            {
                // ShowActiveTable based on contents when the view disappears
                View view = ((AlertDialog) dialogInterface).findViewById(R.id.teamsGroupLayout);
                teamsGroupHandler.LoadFromView(view);
                UpdateTeamsGroup();
            }).setView(dialogView).show();
        });

                TableView tableView = findViewById(R.id.mainTableView);

        // Create TableView Adapter
        tableAdapter = new MainTableAdapter(this);
        tableView.setAdapter(tableAdapter);

        // Create listener
        tableView.setTableViewListener(new MainTableViewListener(tableView));

        if (ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
        {
            LoadSerializedTables();
            LoadTBADataLocal();
        }
        else
        {
            Log.d("HotTeam67", "Requesting Permissions");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.REQUEST_ENABLE_PERMISSION);
        }
    }

    private void UpdateTeamsGroup()
    {
        String groupType = teamsGroupHandler.GetType();
        Integer groupId = teamsGroupHandler.GetId();

        switch (groupType)
        {
            case TeamsGroupHandler.TEAM_GROUP_QUALS:
                teamsGroupButton.setText("Q" + groupId + " TEAMS");
                ShowMatch(groupId);
                break;
            case TeamsGroupHandler.TEAM_GROUP_ELIMS:
                teamsGroupButton.setText("A" + groupId + " TEAMS");
                ShowAlliance(groupId);
                break;
            case TeamsGroupHandler.TEAM_GROUP_CUSTOM:
                teamsGroupButton.setText("C Teams");
                FileHandler.Write(FileHandler.CUSTOM_TEAMS_FILE,
                        TextUtils.join("\n", teamsGroupHandler.GetCustomTeams()));
                ShowTeams(teamsGroupHandler.GetCustomTeams());
                break;
        }

    }

    /*
    Show a designated alliance's teams
     */
    private void ShowAlliance(Integer seatNumber)
    {
        try
        {
            if (seatNumber <= 0 || seatNumber > alliances.size())
            {
                SetTeamNumberFilter();
                ShowActiveTable();
                return;
            }

            List<String> alliance = alliances.get(seatNumber - 1);
            SetTeamNumberFilter(Stream.of(alliance.toArray()).toArray(String[]::new));
            ShowActiveTable();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    Show Custom teams
     */
    private void ShowTeams(List<String> teams)
    {
        try
        {
            if (teams == null || teams.size() == 0)
            {
                SetTeamNumberFilter();
                ShowActiveTable();
                return;
            }

            SetTeamNumberFilter(Stream.of(teams.toArray()).toArray(String[]::new));
            ShowActiveTable();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    When the match search text changes
     */
    private synchronized void ShowMatch(Integer matchNumber)
    {
        try
        {
            if (matchNumber <= 0)
            {
                SetTeamNumberFilter(Constants.EMPTY);
                ShowActiveTable();
                return;
            }
            if (matchNumber <= redTeams.size() && matchNumber <= blueTeams.size())
            {
                List<String> red = new ArrayList<>(
                        Arrays.asList(redTeams.get(matchNumber - 1).split(",")));
                List<String> blue = new ArrayList<>(
                        Arrays.asList(blueTeams.get(matchNumber - 1).split(",")));

                List<String> filters = new ArrayList<>();
                filters.addAll(red);
                filters.addAll(blue);

                List<RowHeaderModel> rows = new ArrayList<>();
                List<List<CellModel>> cells = new ArrayList<>();
                List<ColumnHeaderModel> columns = new ArrayList<>(GetActiveTable().GetColumns());

                for (String team : filters)
                {
                    SetTeamNumberFilter(team);
                    rows.addAll(GetActiveTable().GetRowHeaders());

                    for (List<CellModel> cell : GetActiveTable().GetCells())
                    {
                        List<CellModel> newRow = new ArrayList<>(cell);
                        cells.add(newRow);
                    }
                }

                DataTable processor = new DataTable(columns, cells, rows);
                processor.SetTeamNumberFilter(Constants.EMPTY);

                List<ColumnHeaderModel> columnHeaderModels = processor.GetColumns();
                columnHeaderModels.add(0, new ColumnHeaderModel(Constants.ALLIANCE));

                List<List<CellModel>> outputCells = processor.GetCells();
                for (int i = 0; i < outputCells.size(); ++i)
                {
                    String teamNumber = processor.GetRowHeaders().get(i).getData();

                    if (red.contains(teamNumber))
                    {
                        outputCells.get(i).add(0, new CellModel(i + "_00", Constants.RED));
                    }
                    else {
                        outputCells.get(i).add(0, new CellModel(i + "_00", Constants.BLUE));
                        blue.remove(teamNumber);
                    }
                    red.remove(teamNumber);
                    blue.remove(teamNumber);
                }

                List<RowHeaderModel> rowHeaders = processor.GetRowHeaders();

                int firstRowSize = 0;
                if (outputCells.size() > 0)
                {
                    firstRowSize = outputCells.get(0).size() - 1; // -1 for alliance
                }
                for (String team : red)
                {
                    List<CellModel> row = new ArrayList<>();
                    row.add(new CellModel("0", Constants.RED));

                    for (int i = 0; i < firstRowSize; ++i)
                    {
                        row.add(new CellModel("0", Constants.N_A));
                    }


                    outputCells.add(row);
                    rowHeaders.add(new RowHeaderModel(team));
                }
                for (String team : blue)
                {
                    List<CellModel> row = new ArrayList<>();
                    row.add(new CellModel("0", Constants.BLUE));

                    for (int i = 0; i < firstRowSize; ++i)
                    {
                        row.add(new CellModel("0", Constants.N_A));
                    }

                    outputCells.add(row);
                    rowHeaders.add(new RowHeaderModel(team));
                }

                DataTable newProcessor = new DataTable(columnHeaderModels, outputCells, rowHeaders);

                //Sort by alliance
                tableAdapter.setAllItems(Sort.SortByColumn(newProcessor, 0, false), rawData);
            }
            else
            {
                SetTeamNumberFilter(Constants.EMPTY);
                ShowActiveTable();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    Calculation button event handler
     */
    private synchronized void OnCalculationButton(View v)
    {
        switch (calculationState)
        {
            case DataTableBuilder.Calculation.AVERAGE:
                calculationState = DataTableBuilder.Calculation.MAXIMUM;
                ((Button)v).setText(Constants.AVG);
                ShowActiveTable();
                UpdateTeamsGroup();
                break;
            case DataTableBuilder.Calculation.MAXIMUM:
                calculationState = DataTableBuilder.Calculation.AVERAGE;
                ((Button)v).setText(Constants.MAX);
                ShowActiveTable();
                UpdateTeamsGroup();
                break;
        }
    }

    /*
    Settings button event handler
     */
    private void OnSettingsButton()
    {
        Intent settingsIntent = new Intent(this, PreferencesActivity.class);
        startActivity(settingsIntent);
    }

    /*
    Once disk permission is obtained
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == Constants.REQUEST_ENABLE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                LoadSerializedTables();
                LoadTBADataLocal();
            }
        }
    }

    /*
    Serialize the three datatables and write them to disk
     */
    private synchronized void SerializeTables()
    {
        @SuppressLint("StaticFieldLeak") AsyncTask serializeTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] objects)
            {
                FileHandler.Serialize(calculatedDataMaximums, FileHandler.MAXIMUMS_CACHE);
                FileHandler.Serialize(calculatedDataAverages, FileHandler.AVERAGES_CACHE);
                FileHandler.Serialize(rawData, FileHandler.RAW_CACHE);
                runOnUiThread(() -> EndProgressAnimation());
                return null;
            }
        };
        serializeTask.execute();

    }

    /*
    Load tables from disk into memory (raw, both calculated tables)
     */
    private void LoadSerializedTables()
    {
        StartProgressAnimation();
        @SuppressLint("StaticFieldLeak") AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                calculatedDataAverages = (DataTableBuilder)
                        FileHandler.DeSerialize(FileHandler.AVERAGES_CACHE);
                calculatedDataMaximums = (DataTableBuilder)
                        FileHandler.DeSerialize(FileHandler.MAXIMUMS_CACHE);
                rawData = (DataTable)
                        FileHandler.DeSerialize(FileHandler.RAW_CACHE);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                String customTeams = FileHandler.LoadContents(FileHandler.CUSTOM_TEAMS_FILE);
                if (customTeams != null && !customTeams.trim().isEmpty())
                {
                    teamsGroupHandler.SetCustomTeams(new ArrayList<>(Arrays.asList(customTeams.split("\n"))));
                }

                ShowActiveTable();
                EndProgressAnimation();
            }
        };
        task.execute();
    }

    /*
    Re-download all scouting data + TBA data, then refresh
     */
    private void RefreshTable()
    {
        StartProgressAnimation();

        teamSearchView.setText(Constants.EMPTY);

        LoadTBAData();

        String[] values = GetConnectionProperties();

        if (values == null)
        {
            Log.d("HotTeam67", "Couldn't load connection string");
            EndProgressAnimation();
            return;
        }

        String databaseUrl = values[0];
        String eventName = values[1];
        String apiKey = values[2];

        final FirebaseHandler model = new FirebaseHandler(
                databaseUrl, eventName, apiKey);

        // Null child to get all raw data
        model.Download(() -> {

            rawData = new DataTable(model.getResult(), ColumnSchema.CalculatedColumnsRawNames(), ColumnSchema.SumColumns());

            RunCalculations();

            return null;
        });
    }

    /*
    Get Preferences for web values
     */
    private String[] GetConnectionProperties()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String connectionString = (String) prefs.getAll().get("pref_connectionString");
        String[] values = connectionString.split(";");
        if (values.length != 4)
        {
            runOnUiThread(this::EndProgressAnimation);
            return null;
        }

        return values;
    }

    /*
    Re-run all calculations with the current raw data
     */
    private void RunCalculations()
    {
        // Runs
        @SuppressLint("StaticFieldLeak") AsyncTask averagesTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataTableBuilder averages = new DataTableBuilder(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        ColumnSchema.OutlierAdjustedColumns(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataTableBuilder.Calculation.AVERAGE);
                SetCalculatedDataAverages(averages);
                UpdateIfLoaded();

                return null;
            }
        };
        @SuppressLint("StaticFieldLeak") AsyncTask maximumsTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataTableBuilder maximums = new DataTableBuilder(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        ColumnSchema.OutlierAdjustedColumns(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataTableBuilder.Calculation.MAXIMUM);
                SetCalculatedDataMaximums(maximums);
                UpdateIfLoaded();

                return null;
            }
        };
        averagesTask.execute();
        maximumsTask.execute();
    }

    private synchronized void SetCalculatedDataAverages(DataTableBuilder table)
    {
        calculatedDataAverages = table;
    }

    private synchronized void SetCalculatedDataMaximums(DataTableBuilder table)
    {
        calculatedDataMaximums = table;
    }

    private synchronized void UpdateIfLoaded()
    {
        if (calculatedDataMaximums != null && calculatedDataAverages != null)
        {
            runOnUiThread(() ->
            {
                ShowActiveTable();
                SerializeTables();
            });
        }
    }

    /*
    Load TBA data from the API v3
     */
    private synchronized void LoadTBAData()
    {
        String[] values = GetConnectionProperties();
        if (values == null || values.length != 4)
        {
            new AlertDialog.Builder(this).setTitle("Invalid connection string!")
                    .setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss())
                    .create().show();
            return;
        }
        String eventKey = values[3];

        try
        {
            StringBuilder s = new StringBuilder();

            // Call api and load into csv
            TBAHandler.Matches(eventKey, matches -> {
                try {
                    for (List<List<String>> m : matches) {
                        List<String> redTeams = m.get(0);
                        List<String> blueTeams = m.get(1);
                        for (String t : redTeams) {
                            s.append(t.replace("frc", Constants.EMPTY)).append(",");
                        }
                        for (int t = 0; t < blueTeams.size(); ++t) {
                            s.append(blueTeams.get(t).replace("frc", Constants.EMPTY));
                            if (t + 1 != blueTeams.size())
                                s.append(",");
                        }
                        s.append("\n");
                    }
                    FileHandler.Write(FileHandler.VIEWER_MATCHES_FILE, s.toString());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });

            // Load into json
            try {
                TBAHandler.TeamNames(eventKey, teamNames -> {
                    FileHandler.Write(FileHandler.TEAM_NAMES_FILE, teamNames.toString());
                    teamNumbersNames = teamNames;
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Rankings(eventKey, rankings ->
                {
                    FileHandler.Write(FileHandler.RANKS_FILE, rankings.toString());
                    teamNumbersRanks = rankings;
                }
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Alliances(eventKey, a ->
                {
                    alliances = a;
                    StringBuilder alliancesString = new StringBuilder();
                    for (List<String> alliance : alliances)
                    {
                        alliancesString.append(TextUtils.join(",", alliance));
                        alliancesString.append("\n");
                    }
                    FileHandler.Write(FileHandler.ALLIANCES_FILE, alliancesString.toString());
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            LoadTBADataLocal();
        }
        catch (Exception e)
        {
            Log.e("HotTeam67", "Failed to get event: " + e.getMessage(), e);
        }
    }

    /*
    Load TBA data from files
     */
    private void LoadTBADataLocal()
    {
        redTeams = new ArrayList<>();
        blueTeams = new ArrayList<>();

        String content = FileHandler.LoadContents(FileHandler.VIEWER_MATCHES_FILE);
        if (content == null || content.trim().isEmpty())
            return;
        List<String> contents = Arrays.asList(content.split("\n"));

        for (String match : contents)
        {
            List<String> teams = Arrays.asList(match.split(","));
            // red teams first
            try
            {
                StringBuilder red = new StringBuilder();
                for (int i = 0; i < 3; ++i)
                {
                    red.append(teams.get(i));
                    if (i + 1 != 3)
                        red.append(",");
                }
                redTeams.add(red.toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                StringBuilder blue = new StringBuilder();
                for (int i = 3; i < 6; ++i)
                {
                    blue.append(teams.get(i));
                    if (i + 1 != 6)
                        blue.append(",");
                }
                blueTeams.add(blue.toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        try {
            teamNumbersNames = new JSONObject(FileHandler.LoadContents(FileHandler.TEAM_NAMES_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            teamNumbersRanks = new JSONObject(FileHandler.LoadContents(FileHandler.RANKS_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            alliances = new ArrayList<>();
            String[] alliancesFile = FileHandler.LoadContents(FileHandler.ALLIANCES_FILE)
                    .split("\n");
            for (String value : alliancesFile)
            {
                if (value.trim().isEmpty())
                    continue;
                String[] teams = value.split(",");
                if (teams.length == 1)
                    continue;
                alliances.add(new ArrayList<>(Arrays.asList(teams)));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    ShowActiveTable the UI with the currently active table
     */
    private synchronized void ShowActiveTable()
    {
        if (calculatedDataMaximums == null || calculatedDataAverages == null)
            return;

        if (calculationState == DataTableBuilder.Calculation.MAXIMUM)
            tableAdapter.setAllItems(calculatedDataMaximums.GetTable(), rawData);
        else
            tableAdapter.setAllItems(calculatedDataAverages.GetTable(), rawData);
    }

    /*
    Get the active datatable
     */
    private synchronized DataTable GetActiveTable()
    {
        if (calculationState == DataTableBuilder.Calculation.MAXIMUM)
            return calculatedDataMaximums.GetTable();
        else
            return calculatedDataAverages.GetTable();
    }

    /*
    Set the team number filter on the active table
     */
    private synchronized void SetTeamNumberFilter(String... s)
    {
        if (calculatedDataAverages == null || calculatedDataMaximums == null)
            return;

        calculatedDataMaximums.GetTable().SetTeamNumberFilter(s);
        calculatedDataAverages.GetTable().SetTeamNumberFilter(s);
    }

    /*
    Get a jsonobject of team numbers and team names
     */
    public JSONObject GetTeamNumbersNames() { return teamNumbersNames; }

    /*
    Spin the refresh button around, and disable it
     */
    private void StartProgressAnimation()
    {
        RotateAnimation anim = (RotateAnimation)
                AnimationUtils.loadAnimation(this, R.anim.rotate);
        refreshButton.setAnimation(anim);
        refreshButton.setEnabled(false);
    }

    /*
    Stop refresh button animation and enable it
     */
    private void EndProgressAnimation()
    {
        refreshButton.clearAnimation();
        refreshButton.setEnabled(true);
    }
}
