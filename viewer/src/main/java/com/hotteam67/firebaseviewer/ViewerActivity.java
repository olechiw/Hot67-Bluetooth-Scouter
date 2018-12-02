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
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.evrencoskun.tableview.TableView;
import com.hotteam67.firebaseviewer.data.DataModel;
import com.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import com.hotteam67.firebaseviewer.tableview.MainTableViewListener;

import org.hotteam67.common.Constants;
import org.hotteam67.common.DarkNumberPicker;
import org.hotteam67.common.InterceptAllLayout;

public class ViewerActivity extends AppCompatActivity {

    private MainTableAdapter tableAdapter;

    private ImageButton refreshButton;
    private ImageButton clearButton;
    private Button teamsGroupButton;

    private View teamsGroupView;

    private EditText teamSearchView;

    private ProgressBar progressBar;

    private DarkNumberPicker teamsGroupInput;
    private Spinner teamsGroupType;

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
                teamsGroupInput.setValue(result);
                teamsGroupType.setSelection(0);
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
            teamsGroupButton.setText("Show Teams");
            DataModel.ClearFilters();
            teamsGroupInput.setValue(0);
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

    public static void expand(final View v) {
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

    public static void collapse(final View v) {
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
                    DataModel.ClearFilters(); // Clears and sorts
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
            if (teamsGroupInput.getValue() == 0
                    && teamSearchView.getText().toString().trim().isEmpty())
            {
                clearButton.setVisibility(View.INVISIBLE);
                teamsGroupButton.setText("Show Teams");
            }
            else
                clearButton.setVisibility(View.VISIBLE);
            tableAdapter.setAllItems(DataModel.GetTable());
        }
    }

    private void LoadLocal()
    {
        DataModel.LoadSerializedTables();
        DataModel.LoadTBADataLocal();
    }

    @SuppressLint("SetTextI18n")
    private void UpdateTeamsGroup()
    {
        int id = teamsGroupInput.getValue();
        switch (teamsGroupType.getSelectedItem().toString())
        {
            case Constants.ViewerTeamsGroupTypes.MATCH:
                if (id != 0)
                {
                    teamsGroupButton.setText("Q" + id + " Teams");
                    DataModel.ShowMatch(id);
                }
                else
                {
                    DataModel.ClearFilters();
                    UpdateUI();
                }
                break;
            case Constants.ViewerTeamsGroupTypes.ALLIANCE:
                if (id != 0)
                {
                    teamsGroupButton.setText("A" + id + " Teams");
                    DataModel.ShowAlliance(id);
                }
                else
                {
                    DataModel.ClearFilters();
                    UpdateUI();
                }
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
