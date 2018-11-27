package com.hotteam67.firebaseviewer;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.evrencoskun.tableview.TableView;
import com.hotteam67.firebaseviewer.data.DataModel;
import com.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import com.hotteam67.firebaseviewer.tableview.MainTableViewListener;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private MainTableAdapter tableAdapter;

    private ImageButton refreshButton;
    private ImageButton clearButton;
    private Button teamsGroupButton;

    private EditText teamSearchView;

    private ProgressBar progressBar;

    private TeamsGroupHandler teamsGroupHandler = new TeamsGroupHandler(this);

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
                Integer result = data.getIntExtra("Match Number", 0);
                teamsGroupHandler.SetId(result);
                teamsGroupHandler.SetType(TeamsGroupHandler.TEAM_GROUP_QUALS);
                UpdateTeamsGroup();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (requestCode == Constants.PreferencesRequestCode)
        {
            RefreshConnectionProperties();
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

        progressBar = finalView.findViewById(R.id.indeterminateBar);

        ImageButton settingsButton = finalView.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> OnSettingsButton());

        finalView.findViewById(R.id.calculationButton).setOnClickListener(this::OnCalculationButton);

        refreshButton = finalView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(view -> UpdateUINetwork());

        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v ->
        {
            if (!teamSearchView.getText().toString().trim().isEmpty())
                teamSearchView.setText("");
            teamsGroupButton.setText("TEAMS");
            DataModel.ClearFilters();
            teamsGroupHandler.SetId(0);
            UpdateUI();
            clearButton.setVisibility(View.INVISIBLE);
        });
        clearButton.setVisibility(View.INVISIBLE);

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
                    DataModel.ClearFilters();
                    DataModel.SetTeamNumberFilter(editable.toString());
                    UpdateUI();
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
            AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_NoActionBar_Fullscreen)
                    .setTitle("Team Groups").setOnDismissListener(dialogInterface ->
            {
                // ShowActiveTable based on contents when the view disappears
                View view = ((AlertDialog) dialogInterface).findViewById(R.id.teamsGroupLayout);
                teamsGroupHandler.LoadFromView(view);
                UpdateTeamsGroup();
            }).setView(dialogView).show();
            dialogView.findViewById(R.id.okButton).setOnClickListener(x -> dialog.dismiss());
        });

        TableView tableView = findViewById(R.id.mainTableView);

        // Create TableView Adapter
        tableAdapter = new MainTableAdapter(this);
        tableView.setAdapter(tableAdapter);

        // Create listener
        tableView.setTableViewListener(new MainTableViewListener(tableView, tableAdapter));

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

    private void RefreshConnectionProperties()
    {
        DataModel.Setup(GetConnectionProperties(), new DataModel.ProgressEvent()
        {
            @Override
            public void BeginProgress()
            {
                runOnUiThread(() -> StartProgressAnimation());
            }

            @Override
            public void EndProgress()
            {
                runOnUiThread(() -> {
                    EndProgressAnimation();
                    UpdateUI();
                });
            }
        });
    }

    private void UpdateUINetwork()
    {
        DataModel.RefreshTable(() ->
                runOnUiThread(this::UpdateUI));
    }

    public void UpdateUI()
    {
        if (DataModel.GetTable() != null)
        {
            if (teamsGroupHandler.GetId() == 0
                    && teamSearchView.getText().toString().trim().isEmpty()
                    && !teamsGroupHandler.GetType().equals(TeamsGroupHandler.TEAM_GROUP_CUSTOM))
            {
                clearButton.setVisibility(View.INVISIBLE);
                teamsGroupButton.setText("TEAMS");
            }
            else
                clearButton.setVisibility(View.VISIBLE);
            tableAdapter.setAllItems(DataModel.GetTable());
        }
    }

    private void LoadLocal()
    {
        String customTeams = FileHandler.LoadContents(FileHandler.CUSTOM_TEAMS_FILE);
        if (customTeams != null && !customTeams.trim().isEmpty())
        {
            teamsGroupHandler.SetCustomTeams(new ArrayList<>(Arrays.asList(customTeams.split("\n"))));
        }
        DataModel.LoadSerializedTables();
        DataModel.LoadTBADataLocal();
    }

    @SuppressLint("SetTextI18n")
    private void UpdateTeamsGroup()
    {
        String groupType = teamsGroupHandler.GetType();
        Integer groupId = teamsGroupHandler.GetId();

        switch (groupType)
        {
            case TeamsGroupHandler.TEAM_GROUP_QUALS:
                if (groupId != 0)
                {
                    teamsGroupButton.setText("Q" + groupId + " TEAMS");
                    DataModel.ShowMatch(groupId);
                }
                else
                {
                    DataModel.ClearFilters();
                    UpdateUI();
                }
                break;
            case TeamsGroupHandler.TEAM_GROUP_ELIMS:
                teamsGroupButton.setText("A" + groupId + " TEAMS");
                DataModel.ShowAlliance(groupId);
                break;
            case TeamsGroupHandler.TEAM_GROUP_CUSTOM:
                teamsGroupButton.setText("C Teams");
                FileHandler.Write(FileHandler.CUSTOM_TEAMS_FILE,
                        TextUtils.join("\n", teamsGroupHandler.GetCustomTeams()));
                DataModel.ShowTeams(teamsGroupHandler.GetCustomTeams());
                break;
        }
        UpdateUI();
    }

    /*
    Calculation button event handler
     */
    private synchronized void OnCalculationButton(View v)
    {

        ((Button)v).setText((((Button) v).getText().toString().equals("MAX")) ?
                "AVG" : "MAX");

        DataModel.SwitchCalculation();
        UpdateUI();
    }

    /*
    Settings button event handler
     */
    private void OnSettingsButton()
    {
        Intent settingsIntent = new Intent(this, PreferencesActivity.class);
        startActivityForResult(settingsIntent, Constants.PreferencesRequestCode);
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
                LoadLocal();
            }
        }
    }

    /*
    Get Preferences for web values
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

    /*
    Spin the refresh button around, and disable it
     */
    private void StartProgressAnimation()
    {
        refreshButton.setEnabled(false);
        refreshButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /*
    Stop refresh button animation and enable it
     */
    private void EndProgressAnimation()
    {
        refreshButton.setEnabled(true);
        refreshButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
}
