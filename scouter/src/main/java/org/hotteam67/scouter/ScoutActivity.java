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
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Button;
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


public class ScoutActivity extends BluetoothClientActivity
{
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    ImageButton connectButton;
    ImageButton syncAllButton;

    FloatingActionButton nextMatchButton;
    FloatingActionButton prevMatchButton;

    EditText teamNumber;
    EditText matchNumber;
    EditText notes;

    Toolbar toolbar;

    Button unlockButton;
    int unlockCount = 0;

    List<String> queuedMatchesToSend = new ArrayList<>();

    TableLayout inputTable;

    List<String> matches = new ArrayList<>();

    String lastValuesBeforeChange = "";

    /*
    When the activity is born
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        SetupPreBluetooth();

    }

    /*
    Setup operations before bluetooth is turned on/given permission etc
     */
    private void SetupPreBluetooth()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            SetupPostBluetooth();
        }
        else
        {
            l("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
        }
    }

    /*
    Setup operations after bluetooth is turned on/given permission etc
     */
    private void SetupPostBluetooth()
    {
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayShowTitleEnabled(false);

        connectButton = toolbar.findViewById(R.id.connectButton);

        teamNumber = toolbar.findViewById(R.id.teamNumberText);

        matchNumber = findViewById(R.id.matchNumberText);

        inputTable = findViewById(R.id.scoutLayout);

        nextMatchButton = findViewById(R.id.nextMatchButton);
        prevMatchButton = findViewById(R.id.prevMatchButton);

        notes = findViewById(R.id.notesText);

        nextMatchButton.setOnClickListener(v -> OnNextMatch());
        prevMatchButton.setOnClickListener(v -> OnPreviousMatch());

        final Context c = this;
        syncAllButton = findViewById(R.id.syncAllButton);
        syncAllButton.setOnClickListener(v -> {

            Constants.OnConfirm("Send All Matches?", c, () -> {
                if (!(matches.size() > 1))
                    return;

                queuedMatchesToSend = new ArrayList<>(matches.subList(1, matches.size() - 1));
                SendMatch(matches.get(0));
                MessageBox("Sent Matches");
            });
        });


        // Build the input table's rows and columns.
        SchemaHandler.Setup(
                inputTable,
                SchemaHandler.LoadSchemaFromFile(),
                this);

        LoadMatches();

        matchNumber.clearFocus();
        matchNumber.setText("1");
        if (!matches.isEmpty()) {
            l("Loading first match!");
            teamNumber.setText(GetCurrentTeamNumber());
            ShowMatch(1);
        }

        matchNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (getCurrentFocus() == matchNumber)
                    SaveAllMatches();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ShowMatch(GetCurrentMatchNumber(), false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Button locks syncing and team-number changing, three long clicks to unlock
        unlockButton = findViewById(R.id.unlockButton);
        unlockButton.setOnLongClickListener(v -> {
            unlockCount++;
            if (unlockCount >= 2) {
                syncAllButton.setEnabled(true);
                teamNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                return true;
            }
            else
                return false;
        });
        syncAllButton.setEnabled(false);
        teamNumber.setInputType(InputType.TYPE_NULL);
    }

    /*
    When the right button is clicked
     */
    private void OnNextMatch()
    {
        SaveAllMatches();
        l("Loading Next Match");
        ShowMatch(GetCurrentMatchNumber() + 1);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    /*
    When the left button is clicked
     */
    private void OnPreviousMatch()
    {
        SaveAllMatches();
        if (GetCurrentMatchNumber() > 1)
        {
            l("Loading Previous Match");
            ShowMatch(GetCurrentMatchNumber() - 1);
        }
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    /*
    Get the current matchNumber
     */
    private int GetCurrentMatchNumber()
    {
        try {
            int i = Integer.valueOf(matchNumber.getText().toString());
            if (i <= 0)
                return 1;
            return i;
        } catch (Exception e) {
            return 1;
        }
    }

    /*
    Display a match in the UI
     */
    private void ShowMatch(int match)
    {
        ShowMatch(match, true);
    }

    /*
    Display a match in the UI
    changeMatchText: whether to update match number also
     */
    private void ShowMatch(int match, boolean changeMatchText)
    {
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);

        if (matches.size() >= match) // Currently existing match
        {
            String val = matches.get(match - 1);
            String[] vals = val.split(",");


            try {
                SchemaHandler.SetCurrentValues(inputTable, Arrays.asList(vals).subList(2, vals.length));
            } catch (Exception e) {
                l("Failed to load match, corrupted, out of sync, or doesn't exist " + e.getMessage());
                // e.printStackTrace();
                l("Offending match: -->  " + matches.get(match - 1) + " <--");
                SchemaHandler.ClearCurrentValues(inputTable);
            }

            teamNumber.setText(GetMatchTeamNumber(match));
            notes.setText(GetNotes(match - 1));
        } else // New match
        {
            SaveAllMatches(true, false);
            teamNumber.setText(GetMatchTeamNumber(match));
            notes.setText("");
            SchemaHandler.ClearCurrentValues(inputTable);
        }

        if (changeMatchText) {
            matchNumber.clearFocus();
            matchNumber.setText(String.valueOf(match));
        }

        lastValuesBeforeChange = GetCurrentMatchValuesCSV();
    }

    /*
    Get the value of notes from memory for the current match
     */
    private String GetNotes(int i)
    {
        if (matches.size() > i)
        {
            // Split doesn't catch the end if there are no notes, so check for no notes
            if (matches.get(i).endsWith(","))
                return "";
            else if (matches.get(i).split(",").length < 2)
                return "";
            else
                try {
                    String[] match = matches.get(i).split(",");
                    return match[match.length - 1];
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return "";
    }

    /*
    Get the team number for this match number
     */
    private String GetCurrentTeamNumber()
    {
        return GetMatchTeamNumber(GetCurrentMatchNumber());
    }

    /*
    Get the team number for a specific match number
     */
    private String GetMatchTeamNumber(int m)
    {
        try
        {
            return matches.get(m - 1).split(",")[0];
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "0";
        }
    }

    /*
    Save all matches and send the current one over bluetooth
    Localonly: No sending. SaveDuplicates: Save/send even if nothing changed
     */
    private void SaveAllMatches()
    {
        SaveAllMatches(false, false);
    }

    /*
    Save all matches and send the current one over bluetooth
    Localonly: No sending. SaveDuplicates: Save/send even if nothing changed
     */
    private void SaveAllMatches(boolean localOnly, boolean saveDuplicates)
    {
        // Check if something actually changed since the value was loaded
        if (GetCurrentMatchValuesCSV().equals(lastValuesBeforeChange) && !saveDuplicates) {
            l("nothing changed, not saving: " + GetCurrentMatchValuesCSV());
            return;
        }

        // Existing match
        if (matches.size() >= GetCurrentMatchNumber())
        {
            // l("Setting value: " + GetCurrentMatchValuesCSV());
            matches.set(GetCurrentMatchNumber() - 1, GetCurrentMatchValuesCSV());
        }
        // New match
        else if (matches.size() + 1 == GetCurrentMatchNumber())
        {
            matches.add(GetCurrentMatchValuesCSV());
        }

        // Store all matches locally
        StringBuilder output = new StringBuilder();
        int i = 1;
        for (String s : matches)
        {
            output.append(s);
            if (i < matches.size())
                output.append("\n");
            i++;
        }
        // l("Writing output to matches file: " + output);
        FileHandler.Write(FileHandler.SCOUTER_FILE, output.toString());

        // Write to bluetooth
        if (!localOnly)
        {
            // Make sure actual match is there
            if (matches.get(GetCurrentMatchNumber() - 1).split(",").length > 1)
                SendMatch(matches.get(GetCurrentMatchNumber() - 1));
            else
                l("Saving local only due to missing match data");
        }
    }

    /*
    Send a match over bluetooth
     */
    private void SendMatch(String match)
    {
        if (match == null || match.split(",").length <= 1) return;

        try
        {
            JSONObject outputObject = new JSONObject();

            List<String> values = new ArrayList<>(Arrays.asList(
                    match.split(",")));

            // Add blank notes because split doesn't catch those
            if (match.endsWith(",")) values.add("");

            List<String> jsonTags = new ArrayList<>(Arrays.asList(
                    SchemaHandler.GetHeader(
                            SchemaHandler.LoadSchemaFromFile()).split(",")));
            jsonTags.removeAll(Arrays.asList("", null));
            jsonTags.add(0, Constants.TEAM_NUMBER_JSON_TAG);
            jsonTags.add(1, Constants.MATCH_NUMBER_JSON_TAG);
            jsonTags.add(Constants.NOTES_JSON_TAG);
            if (jsonTags.size() != values.size())
            {
                l("Failed to load schema into json, values/tags out of sync!");
                l(String.valueOf(jsonTags.size()));
                l(String.valueOf(values.size()));
            } else
            {
                try
                {
                    // Everything including notes
                    for (int i = 0; i < jsonTags.size(); ++i)
                    {
                        outputObject.put(jsonTags.get(i), values.get(i));
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

    /*
    Get current match values as csv
     */
    private String GetCurrentMatchValuesCSV()
    {
        StringBuilder values = new StringBuilder();
        String div = ",";


        values.append(teamNumber.getText().toString()).append(div);


        values.append(GetCurrentMatchNumber()).append(div);

        List<String> currentValues = SchemaHandler.GetCurrentValues(inputTable);
        for (int i = 0; i < currentValues.size(); ++i)
        {
            String s = currentValues.get(i);
            values.append(s);
            values.append(div);
        }


        String notesText = notes.getText().toString().replace(",", "");
        notesText = notesText.replace("\r\n", "");
        notesText = notesText.replace("\n", "");
        values.append(notesText);

        // l("Current Values: " + values);

        return values.toString();
    }

    /*
    Load matches from the file into memory
     */
    private void LoadMatches()
    {
        try
        {
            BufferedReader r = FileHandler.GetReader(FileHandler.SCOUTER_FILE);
            String line = r.readLine();
            while (line != null)
            {
                matches.add(line);
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

    /*
    Decide what to do with received bluetooth message
     */
    private synchronized void ProcessBluetoothInput(Message msg)
    {
        try {
            final String message =
                    Constants.getScouterInputWithoutTag((String) msg.obj);
            final String tag =
                    Constants.getScouterInputTag((String) msg.obj);

            if (tag.equals(Constants.SERVER_TEAMS_RECEIVED_TAG)
                    &&
                    (queuedMatchesToSend.size() > 0))
            {
                l("Server received last, sending again");
                SendMatch(queuedMatchesToSend.get(0));
                queuedMatchesToSend.remove(0);
            }

            switch (tag) {
                case Constants.SCOUTER_SCHEMA_TAG:
                    final Context c = this;
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new schema, clear local schema?", this, () ->
                    {
                        FileHandler.Write(FileHandler.SCHEMA_FILE, message);
                        SchemaHandler.Setup(
                                inputTable, // Table to setup the new schema on
                                SchemaHandler.LoadSchemaFromFile(), // Schema text
                                c); // Context
                        FileHandler.Write(FileHandler.SCOUTER_FILE, "");
                        matches = new ArrayList<>();
                        ShowMatch(1);
                    });
                    break;
                case Constants.SCOUTER_TEAMS_TAG:
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new teams, clear local database?", this, () ->
                    {
                        matches = new ArrayList<>(Arrays.asList(message.split(",")));
                        ShowMatch(1);
                        SaveAllMatches(true, true);
                    });
                    break;
                case Constants.SERVER_MESSAGE_TAG:
                    //Shows the message received from the server
                    MessageBox(message);
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

    /*
    Handle messages from other threads
     */
    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT: // Input received through bluetooth
                ProcessBluetoothInput(msg);
                break;
            case MESSAGE_CONNECTED: // A device has connected
                connectButton.setImageResource(R.drawable.ic_network_wifi);
                break;
            case MESSAGE_DISCONNECTED: // A device has disconnected
                connectButton.setImageResource(R.drawable.ic_network_off);
                break;
        }
    }

    /*
    Confirmation box to prevent unnecessary disconnects
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                Constants.OnConfirm("Are you sure you want to quit?", this, this::finish);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    Confirmation box to prevent unnecessary disconnects
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            android.util.Log.d(this.getClass().getName(), "back button pressed");
            Constants.OnConfirm("Are you sure you want to quit?", this, this::finish);
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
    Run Post-Bluetooth setup once perms are authorized
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_ENABLE_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                l("Permission granted");
                SetupPostBluetooth();
            }
            else
            {
                SetupPreBluetooth();
            }
        }
    }
}
