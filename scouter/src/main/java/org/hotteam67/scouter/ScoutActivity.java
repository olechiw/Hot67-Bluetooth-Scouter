package org.hotteam67.scouter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TableLayout;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ScoutActivity extends BluetoothActivity
{
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    ImageButton connectButton;
    ImageButton syncAllButton;

    FloatingActionButton nextMatchButton;
    FloatingActionButton prevMatchButton;

    EditText teamNumber;
    EditText matchNumber;

    Toolbar toolbar;

    GestureDetectorCompat gestureDetectorCompat;

    List<String> queuedMatchesToSend = new ArrayList<>();

    String matchValuesOnLoad;

    /*
    GridView scoutLayout;
    org.hotteam67.bluetoothscouter.ScoutInputAdapter scoutInputAdapter;
    */
    TableLayout inputTable;

    List<String> matches = new ArrayList<>();
    List<String> teams = new ArrayList<>();

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                confirmActivityEnd();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            android.util.Log.d(this.getClass().getName(), "back button pressed");
            confirmActivityEnd();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void confirmActivityEnd()
    {
        Constants.OnConfirm("Are you sure you want to quit?", this, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        setupPreBluetooth();

    }

    private void setupPostBluetooth()
    {
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        //ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        connectButton = (ImageButton) toolbar.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                l("Triggered Connect!");
                connectButton.setImageResource(R.drawable.ic_network_check);
                Connect();
            }
        });

        teamNumber = (EditText) toolbar.findViewById(R.id.teamNumberText);

        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                if (source != null && ",".contains(("" + source))) {
                    return "";
                }
                return null;
            }
        };

        matchNumber = (EditText) findViewById(R.id.matchNumberText);

        inputTable = (TableLayout) findViewById(R.id.scoutLayout);

        nextMatchButton = (FloatingActionButton) findViewById(R.id.nextMatchButton);
        prevMatchButton = (FloatingActionButton) findViewById(R.id.prevMatchButton);

        nextMatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadNextMatch();
            }
        });
        prevMatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadPreviousMatch();
            }
        });

        final Context c = this;
        syncAllButton = (ImageButton) findViewById(R.id.syncAllButton);
        syncAllButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // toast("Matches sending: " + matches.size());

                if (!(matches.size() > 1))
                    return;

                if (matches.size() == 1) {
                    bluetoothSendMatch(queuedMatchesToSend.get(0));
                    toast("Sent 1 match");
                }
                else
                {
                    queuedMatchesToSend = new ArrayList<>(matches.subList(1, matches.size() - 1));
                    bluetoothSendMatch(matches.get(0));
                    toast("Sent 1 match, queued " + queuedMatchesToSend.size() + " matches");
                }

            }
        });


        // Build the input table's rows and columns.
        SchemaHandler.Setup(
                inputTable, // Table
                SchemaHandler.LoadSchemaFromFile(), // Text schema
                this); // Context

        loadDatabase();

        matchNumber.clearFocus();
        matchNumber.setText("1");
        if (!matches.isEmpty())
        {
            l("Loading first match!");
            teamNumber.setText(teams.get(0));
            displayMatch(1);
        }

        matchNumber.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                if (getCurrentFocus() == matchNumber)
                    saveCurrentMatch();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                displayMatch(getCurrentMatchNumber(), false);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        gestureDetectorCompat = new GestureDetectorCompat(this, new GestureListener());

        ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        // scrollView.requestDisallowInterceptTouchEvent(true);
        scrollView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return gestureDetectorCompat.onTouchEvent(event);
            }
        });
    }

    private void loadNextMatch()
    {
        saveCurrentMatch();
        l("Loading Next Match");
        displayMatch(getCurrentMatchNumber() + 1);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    private void loadPreviousMatch()
    {
        saveCurrentMatch();
        if (getCurrentMatchNumber() > 1)
        {
            l("Loading Previous Match");
            displayMatch(getCurrentMatchNumber() - 1);
        }
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    int getCurrentMatchNumber()
    {
        try
        {
            int i = Integer.valueOf(matchNumber.getText().toString());
            if (i <= 0)
                return 1;
            return i;
        }
        catch (Exception e)
        {
            return 1;
        }
    }

    private void displayMatch(int match)
    {
        displayMatch(match, true);
    }
    private void displayMatch(int match, boolean changeMatchText)
    {
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
        if (matches.size() >= match) // Currently existing match
        {
            String val = matches.get(match - 1);
            matchValuesOnLoad = val;
            String[] vals = val.split(",");
            // List<String> subList = Arrays.asList(vals).subList(2, vals.length - 1);
            try {
                SchemaHandler.SetCurrentValues(inputTable, Arrays.asList(vals).subList(2, vals.length - 1));
            }
            catch (Exception e)
            {
                l("Failed to load match, corrupted, out of sync, or doesn't exist " + e.getMessage());
                // e.printStackTrace();
                l("Offending match: -->  " + matches.get(match - 1) + " <--");
            }

            if (teams.size() >= match)
                teamNumber.setText(teams.get(match - 1));
            else
                teamNumber.setText("0");
        }
        else if (matches.size() + 1 == match) // Last match
        {
            if (teams.size() >= match)
                teamNumber.setText(teams.get(match - 1));
            else
                teamNumber.setText("0");

            SchemaHandler.ClearCurrentValues(inputTable);
        }
        else // Other match, display new match 1 after last match
        {
            displayMatch(matches.size());
            return;
        }

        if (changeMatchText)
        {
            matchNumber.clearFocus();
            matchNumber.setText(String.valueOf(match));
        }
    }

    private String getCurrentTeamNumber()
    {
        String s = teamNumber.getText().toString();
        if (!s.trim().isEmpty())
            return s;
        else
            return "0";
    }

    private void clearMatchesDatabase()
    {
        try
        {
            FileHandler.Write(FileHandler.SCOUTER_DATABASE, "");
        }
        catch (Exception e)
        {
            l("Failed to clear file for re-write: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveCurrentMatch()
    {
        // Existing match
        if (matches.size() >= getCurrentMatchNumber())
        {
            // l("Setting value: " + getCurrentMatchValues());
            matches.set(getCurrentMatchNumber() - 1, getCurrentMatchValues());
            teams.set(getCurrentMatchNumber() - 1, getCurrentTeamNumber());
        }
        // New match
        else if (matches.size() + 1 == getCurrentMatchNumber())
        {
            matches.add(getCurrentMatchValues());
            teams.add(getCurrentTeamNumber());
        }

        // Store all matches locally
        clearMatchesDatabase();
        String output = "";
        int i = 1;
        for (String s : matches)
        {
            output += s;
            if (i < matches.size())
                output += "\n";
            i++;
        }
        // l("Writing output to matches file: " + output);
        FileHandler.Write(FileHandler.SCOUTER_DATABASE, output);

        // Check if something actually changed since the value was loaded
        if (matches.get(getCurrentMatchNumber() - 1).equals(matchValuesOnLoad))
            return;

        bluetoothSendMatch(matches.get(getCurrentMatchNumber() - 1));
    }

    private void bluetoothSendMatch(String match)
    {
        if (match == null || match.split(",").length <= 1) return;
        try
        {
            JSONObject outputObject = new JSONObject();

            List<String> values = new ArrayList<>(Arrays.asList(
                    match.split(",")));
            List<String> headers = new ArrayList<>(Arrays.asList(
                    SchemaHandler.GetHeader(
                            SchemaHandler.LoadSchemaFromFile()).split(",")));
            headers.removeAll(Arrays.asList("", null));
            headers.add(0, Constants.TEAM_NUMBER_JSON_TAG);
            headers.add(1, Constants.MATCH_NUMBER_JSON_TAG);
            if (headers.size() != values.size())
            {
                l("Failed to load schema into json, values out of sync!");
                l(String.valueOf(headers.size()));
                l(String.valueOf(values.size()));
            } else
            {
                try
                {
                    for (int i = 0; i < headers.size(); ++i)
                    {
                        outputObject.put(headers.get(i), values.get(i));
                    }

                    Write(outputObject.toString());

                    l("Output JSON: " + outputObject.toString());
                } catch (JSONException e)
                {
                    l("Exception raised in json addition:" + e.getMessage());
                    return;
                }
            }
        }
        catch (Exception e)
        {
            l("Failure to load and parse csv match to json: " + match);
            e.printStackTrace();
        }
    }

    private String getCurrentMatchValues()
    {
                /*
        l("Sending values:\n" + "67,1");
        */
        StringBuilder values = new StringBuilder();
        String div = ",";

        /*
        if (teamNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += teamNumber.getText().toString() + div;
            */
        values.append(getCurrentTeamNumber()).append(div);
/*
        if (matchNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += matchNumber.getText() + div;
            */
        values.append(getCurrentMatchNumber()).append(div);

        List<String> currentValues = SchemaHandler.GetCurrentValues(inputTable);
        for (int i = 0; i < currentValues.size(); ++i)
        {
            String s = currentValues.get(i);
            values.append(s);
            values.append(div);
        }


        if (values.length() > 0)
            values = new StringBuilder(values.substring(0, values.length() - 1));

        // l("Current Values: " + values);

        return values.toString();
    }

    private void setupPreBluetooth()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            setupPostBluetooth();
        }
        else
        {
            l("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            // l("onDown: " + event.toString());
            return false; // Allow scrollview work
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            float sens = 500;
            if (
                    Math.abs(velocityX) > Math.abs(velocityY)
                    && Math.abs(velocityX) > sens)
            {
                if (velocityX > 0)
                {
                    loadPreviousMatch();
                }
                else
                {
                    loadNextMatch();
                }
                return true;
            }
            return false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_ENABLE_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                l("Permission granted");
                setupPostBluetooth();
            }
            else
            {
                setupPreBluetooth();
            }
        }
    }


    private void loadDatabase()
    {
        try
        {
            BufferedReader r = FileHandler.GetReader(FileHandler.SCOUTER_DATABASE);
            String line = r.readLine();
            while (line != null)
            {
                matches.add(line);
                teams.add(line.split(",")[0]);
                line = r.readLine();
            }
            r.close();
        }
        catch (Exception e)
        {
            l("Failed to load contents of matches database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized void bluetoothProcessInput(Message msg)
    {
        try {
            final String message =
                    Constants.getScouterInputWithoutTag((String) msg.obj);
            final String tag =
                    Constants.getScouterInputTag((String) msg.obj);

            if (message.equals(Constants.SERVER_TEAMS_RECEIVED_TAG)
                    &&
                    (queuedMatchesToSend.size() > 0))
            {
                bluetoothSendMatch(queuedMatchesToSend.get(0));
                queuedMatchesToSend.remove(0);
            }

            switch (tag) {
                case Constants.SCOUTER_SCHEMA_TAG:
                    final Context c = this;
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new schema, clear local schema?", this, new Runnable() {
                        @Override
                        public void run() {
                            FileHandler.Write(FileHandler.SCHEMA, message);
                            SchemaHandler.Setup(
                                    inputTable, // Table to setup the new schema on
                                    SchemaHandler.LoadSchemaFromFile(), // Schema text
                                    c); // Context
                        }
                    });
                    break;
                case Constants.SCOUTER_TEAMS_TAG:
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new teams, clear local database?", this, new Runnable() {
                        @Override
                        public void run() {
                            teams = new ArrayList<>(Arrays.asList(message.split(",")));
                            matches = new ArrayList<>();
                            clearMatchesDatabase();
                            SchemaHandler.ClearCurrentValues(inputTable);
                            displayMatch(1);
                        }
                    });
                    break;
                default:
                    l("Received unknown tag: " + tag);
            }
        }
        catch (Exception e)
        {
            l("Failed to load processed input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT: // Input received through bluetooth
                bluetoothProcessInput(msg);
                break;
            case MESSAGE_CONNECTED: // A device has connected
                connectButton.setImageResource(R.drawable.ic_network_wifi);
                break;
            case MESSAGE_DISCONNECTED: // A device has disconnected
                connectButton.setImageResource(R.drawable.ic_network_off);
                break;
        }
    }
}
