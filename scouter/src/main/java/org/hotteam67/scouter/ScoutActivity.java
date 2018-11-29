package org.hotteam67.scouter;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;


public class ScoutActivity extends BluetoothClientActivity
{
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    private ImageView connectionStatus;
    private ImageButton syncAllButton;

    private EditText teamNumber;
    private EditText matchNumber;
    private EditText notes;

    private int unlockCount = 0;

    private List<JSONObject> queuedMatchesToSend = new ArrayList<>();

    private TableLayout inputTable;

    private List<JSONObject> matches = new ArrayList<>();

    private String lastValuesBeforeChange = "";

    /*
    When the activity is born
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        CheckBluetooth();

    }

    /*
    Setup operations before bluetooth is turned on/given permission etc
     */
    private void CheckBluetooth()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            SetupAfterBluetooth();
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
    private void SetupAfterBluetooth()
    {
        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayShowTitleEnabled(false);

        connectionStatus = toolbar.findViewById(R.id.connectionStatus);

        teamNumber = toolbar.findViewById(R.id.teamNumberText);

        matchNumber = findViewById(R.id.matchNumberText);

        inputTable = findViewById(R.id.scoutLayout);

        FloatingActionButton nextMatchButton = findViewById(R.id.nextMatchButton);
        FloatingActionButton prevMatchButton = findViewById(R.id.prevMatchButton);

        notes = findViewById(R.id.notesText);

        nextMatchButton.setOnClickListener(v -> OnNextMatch());
        prevMatchButton.setOnClickListener(v -> OnPreviousMatch());

        final Context c = this;
        syncAllButton = findViewById(R.id.syncAllButton);
        syncAllButton.setOnClickListener(v -> Constants.OnConfirm(
                "Send All Matches?", c, this::SendAllMatches));


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
            teamNumber.setText(GetMatchTeamNumber(GetCurrentMatchNumber()));
            DisplayMatch(1);
        }

        matchNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (getCurrentFocus() == matchNumber) {
                    SaveCurrentMatch();
                    SaveAllMatches();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                DisplayMatch(GetCurrentMatchNumber(), false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Button locks syncing and team-number changing, three long clicks to unlock
        Button unlockButton = findViewById(R.id.unlockButton);
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
        SaveCurrentMatch();
        SaveAllMatches();
        l("Loading Next Match");
        DisplayMatch(GetCurrentMatchNumber() + 1);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    /*
    When the left button is clicked
     */
    private void OnPreviousMatch()
    {
        SaveCurrentMatch();
        SaveAllMatches();
        if (GetCurrentMatchNumber() > 1)
        {
            l("Loading Previous Match");
            DisplayMatch(GetCurrentMatchNumber() - 1);
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
    private void DisplayMatch(int match)
    {
        DisplayMatch(match, true);
    }

    /*
    Display a match in the UI
    changeMatchText: whether to update match number also
     */
    private void DisplayMatch(int match, boolean changeMatchText)
    {
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);

        if (matches.size() >= match) // Currently existing match
        {
            try {
                // Load the match and display it
                JSONObject val = matches.get(match - 1);
                if (val != null)
                    SchemaHandler.SetCurrentValues(inputTable, val);
            } catch (Exception e) {
                l("Failed to load match, corrupted, out of sync, or doesn't exist " + e.getMessage());
                l("Offending match: " + matches.get(match - 1));
                SchemaHandler.ClearCurrentValues(inputTable);
            }

            teamNumber.setText(GetMatchTeamNumber(match));
            notes.setText(GetNotes(match - 1));
        } else // New match
        {
            // Save current matches (no bluetooth)
            SaveCurrentMatch(true, false);
            SaveAllMatches();

            // Load the new match
            teamNumber.setText(GetMatchTeamNumber(match));
            notes.setText("");
            SchemaHandler.ClearCurrentValues(inputTable);
            matches.add(GetCurrentInputValues());
        }

        if (changeMatchText) {
            matchNumber.clearFocus();
            matchNumber.setText(String.valueOf(match));
        }

        JSONObject currentValues = GetCurrentInputValues();
        if (currentValues != null)
            lastValuesBeforeChange = currentValues.toString();
    }

    /*
    Get the value of notes from memory for the current match
     */
    private String GetNotes(int matchNumber)
    {
        if (matches.size() > matchNumber)
            try
            {
                JSONObject match = matches.get(matchNumber);
                if (match != null && match.has(Constants.NOTES_JSON_TAG)) return match.getString(Constants.NOTES_JSON_TAG);
                else return "";
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        return "";
    }

    /*
    Get the team number for a specific match number
     */
    private String GetMatchTeamNumber(int m)
    {
        try
        {
            return matches.get(m - 1)
                    .getString(Constants.TEAM_NUMBER_JSON_TAG);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "0";
        }
    }

    private void SendAllMatches()
    {
        if (!(matches.size() > 1))
            return;

        queuedMatchesToSend = new ArrayList<>(matches.subList(1, matches.size() - 1));
        SendMatch(matches.get(0));
        MessageBox("Sent Matches");
    }

    /*
    Save all matches and send the current one over bluetooth
    Local only: No sending. SaveDuplicates: Save/send even if nothing changed
     */
    private void SaveAllMatches()
    {
        // Store all matches locally
        StringBuilder output = new StringBuilder();
        int i = 1;
        for (JSONObject s : matches)
        {
            output.append(s.toString());
            if (i < matches.size())
                output.append("\n");
            i++;
        }
        // l("Writing output to matches file: " + output);
        FileHandler.Write(FileHandler.SCOUTER_FILE, output.toString());
    }

    /*
    Save match locally
     */
    private void SaveCurrentMatch() { SaveCurrentMatch(false, false); }

    /*
    Save match locally
     */
    private void SaveCurrentMatch(boolean localOnly, boolean saveDuplicates)
    {
        // Check if something actually changed since the value was loaded
        JSONObject currentMatch = GetCurrentInputValues();
        if (currentMatch == null || currentMatch.toString().equals(lastValuesBeforeChange) && !saveDuplicates) {
            l("nothing changed, not saving: " + GetCurrentInputValues());
            return;
        }

        // Existing match
        if (GetCurrentMatchNumber() <= matches.size())
        {
            matches.set(GetCurrentMatchNumber() - 1, GetCurrentInputValues());
        }
        // New match when we are on the last match or there are no matches yet
        else if (GetCurrentMatchNumber() + 1 == matches.size() || matches.size() == 0)
        {
            matches.add(GetCurrentInputValues());
        }
        // Too large of a match number
        else
        {
            return;
        }
        // Write to bluetooth
        if (!localOnly)
        {
            // Make sure actual match is there
            if (matches.size() >= GetCurrentMatchNumber() && matches.get(GetCurrentMatchNumber() - 1) != null)
                SendMatch(matches.get(GetCurrentMatchNumber() - 1));
            else
                l("Saving local only due to missing match data");
        }
    }

    /*
    Send a match over bluetooth
     */
    private void SendMatch(JSONObject match)
    {
        if (match == null) return;

        try
        {
            // Send the match over bluetooth
            BluetoothWrite(match.toString());
            l("Output JSON: " + match);
        }
        catch (Exception e)
        {
            l("Failure to send json match: " + match);
            e.printStackTrace();
        }
    }

    /*
    Get current match values as JSON string
     */
    private JSONObject GetCurrentInputValues()
    {
        JSONObject currentValues = SchemaHandler.GetCurrentValues(inputTable);


        // Experimenting without sanitation now that everything is JSON
        String notesText = notes.getText().toString();//.replace(",", "");
        /*
        notesText = notesText.replace("\r\n", "");
        notesText = notesText.replace("\n", "");
        */
        try
        {
            /*
            Add three values outside of schema-input fields: Team #, Match #, and Notes
             */
            currentValues.put(Constants.NOTES_JSON_TAG, notesText);
            currentValues.put(Constants.TEAM_NUMBER_JSON_TAG, teamNumber.getText().toString());
            currentValues.put(Constants.MATCH_NUMBER_JSON_TAG, matchNumber.getText().toString());
            return currentValues;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /*
    Load matches from the file into memory
     */
    private void LoadMatches()
    {
        try
        {
            BufferedReader r = FileHandler.GetReader(FileHandler.SCOUTER_FILE);
            String line = null;
            if (r != null)
            {
                line = r.readLine();
            }
            while (line != null)
            {
                matches.add(new JSONObject(line));
                line = r.readLine();
            }
            if (r != null)
            {
                r.close();
            }
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
                        DisplayMatch(1);
                    });
                    break;
                case Constants.SCOUTER_TEAMS_TAG:
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new teams, clear local database?", this, () ->
                    {
                        matches = new ArrayList<>();
                        String[] teamNumbers = message.split(",");
                        // Create a json object for each of the CSV teams
                        for (int i = 0;i < teamNumbers.length; ++i)
                        {
                            try
                            {
                                if (teamNumbers[i] != null && !teamNumbers[i].trim().isEmpty())
                                {
                                    JSONObject matchObject = new JSONObject();
                                    matchObject.put(Constants.TEAM_NUMBER_JSON_TAG, teamNumbers[i]);
                                    matchObject.put(Constants.MATCH_NUMBER_JSON_TAG, String.valueOf(i - 1));
                                    matches.add(matchObject);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        DisplayMatch(1);
                        SaveCurrentMatch(true, true);
                        SaveAllMatches();
                    });
                    break;
                case Constants.SERVER_MESSAGE_TAG:
                    //Shows the message received from the server
                    MessageBox(message);
                    break;
                case Constants.SERVER_SUBMIT_TAG:
                    // If the match is still open show a countdown before sending
                    if (String.valueOf(GetCurrentMatchNumber()).equals(message))
                        SubmitCountDown();
                    else
                    {
                        try
                        {
                            // If match was not current one, but exists, just send it right away
                            if (Integer.valueOf(message) < matches.size())
                            {
                                SendMatch(matches.get(Integer.valueOf(message)));
                            }
                        }
                        catch (Exception e)
                        {
                            l("Error processing match number request: " + message);
                            e.printStackTrace();
                        }
                    }
                    break;
                case Constants.SERVER_SYNCALL_TAG:
                    SendAllMatches();
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
            case MESSAGE_CONNECTED: // The device has connected
                connectionStatus.setImageResource(R.drawable.ic_network_wifi);
                break;
            case MESSAGE_DISCONNECTED: // The device has disconnected
                connectionStatus.setImageResource(R.drawable.ic_network_off);
                break;
        }
    }


    /*
    Count down for 15 seconds and then send the current match over bluetooth
     */
    private void SubmitCountDown()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        dialog = builder
                .setTitle("Auto submitting Soon...").setMessage("Submitting in 15 seconds")
                .setPositiveButton("Submit", (dialogInterface, i) ->
        {
            // Submit
            OnNextMatch();
        }).setNegativeButton("Cancel", (dialogInterface, i) ->
        {
            // Cancelled
        }).create();
        dialog.show();

        // Start a countdown timer
        new CountDownTimer(16000, 1000)
        {
            int time = 16;
            @Override
            public void onTick(long l)
            {
                time -= 1;
                // Change dialog text every tick
                ((TextView)dialog.findViewById(android.R.id.message))
                        .setText("Submitting in " + time + " seconds!");
            }

            @Override
            public void onFinish()
            {
                // Dialog has not disappeared/been canceled
                if (dialog.isShowing())
                {
                    // Submit
                    OnNextMatch();
                    dialog.dismiss();
                }
            }
        }.start();
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
        // Confirm when the back button is pressed
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
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
                SetupAfterBluetooth();
            }
            else
            {
                CheckBluetooth();
            }
        }
    }
}
