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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.Timer;
import java.util.TimerTask;


/**
 * The main activity for the scouter, handles UI, user input, and the bluetooth input/output
 */
public class ScoutActivity extends BluetoothClientActivity
{
    /**
     * Activity request to enable the bluetooth
     */
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    /**
     * The state of sending matches for sync all
     */
    enum SendingState { SENDING, WAITING }
    private SendingState sendingState = SendingState.WAITING;

    private ImageView connectionStatus;
    private ImageButton sendAllButton;
    private ProgressBar sendAllProgress;

    private EditText teamNumber;
    private EditText matchNumber;
    private EditText notes;

    private int unlockCount = 0;

    /**
     * The matches that are queued to be sent to the server for sync all, send one more every time
     */
    private List<JSONObject> queuedMatchesToSend = new ArrayList<>();

    private TableLayout inputTable;

    private List<JSONObject> matches = new ArrayList<>();

    private String lastValuesBeforeChange = "";

    /**
    When the activity is born
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        CheckBluetooth();

    }

    /**
     * Check whether bluetooth permissions are setup, and request them if they arent
     */
    private void CheckBluetooth()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        Constants.Log("Permission granted");
            SetupAfterBluetooth();
        }
        else
        {
        Constants.Log("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
        }
    }

    /**
     * Setup the user interface and threads after bluetooth has been initialized
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
        sendAllButton = findViewById(R.id.sendAllButton);
        sendAllButton.setOnLongClickListener(v ->
                {
                    Constants.OnConfirm(
                            "Send All Matches?", c, this::SendAllMatches);
                    return true;
                });
        sendAllProgress = findViewById(R.id.indeterminateBar);


        // Build the input table's rows and columns.
        SchemaHandler.Setup(
                inputTable,
                SchemaHandler.LoadSchemaFromFile(),
                this);

        LoadMatches();

        matchNumber.clearFocus();
        matchNumber.setText("1");
        if (!matches.isEmpty()) {
        Constants.Log("Loading first match!");
            teamNumber.setText(GetMatchTeamNumber(GetDisplayedMatchNumber()));
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
                DisplayMatch(GetDisplayedMatchNumber(), false);
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
                teamNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                return true;
            }
            else
                return false;
        });
        teamNumber.setInputType(InputType.TYPE_NULL);
    }

    /**
     * When the next match is to be shown, save locally, send the current match, display the next one
     */
    private void OnNextMatch()
    {
        SaveCurrentMatch();
        SaveAllMatches();
        Constants.Log("Loading Next Match");
        DisplayMatch(GetDisplayedMatchNumber() + 1);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    /**
     * When the left button is clicked, save/send/show matches
     */
    private void OnPreviousMatch()
    {
        SaveCurrentMatch();
        SaveAllMatches();
        if (GetDisplayedMatchNumber() > 1)
        {
        Constants.Log("Loading Previous Match");
            DisplayMatch(GetDisplayedMatchNumber() - 1);
        }
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    /**
     * Get the currently <b>displayed</b> match number, not the INDEX
     * @return match number
     */
    private int GetDisplayedMatchNumber()
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

    /**
     * Show a match in the UI, changing the match number text
     * @param match the match number to show
     */
    private void DisplayMatch(int match)
    {
        DisplayMatch(match, true);
    }

    /**
     * Show a match in the UI
     * @param match the match number to show
     * @param changeMatchText whether to change matchNumber textview's value, set to false if this
     *                        was triggered by the user editing the text
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
                Constants.Log(e);
                Constants.Log("Offending match: " + matches.get(match - 1));
                SchemaHandler.ClearCurrentValues(inputTable);
            }

            teamNumber.setText(GetMatchTeamNumber(match));
            notes.setText(GetNotes(match - 1));
        } else // New match
        {
            // Save current matches (no bluetooth)
            SaveMatch(GetCurrentInputValues(), true, false);
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

    /**
     * Load the value of the notes from memory for given match number
     * @param matchNumber the match number to get notes for
     * @return the string value of the notes key, or "" if none are found/failure occurs
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
                Constants.Log(e);
            }
        return "";
    }

    /**
     * Get the team number from memory for a specific match
     * @param m the match number ot get
     * @return the team number in String format, but also an int
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
            Constants.Log(e);
            return "0";
        }
    }

    /**
     * Begin syncing all matches to the server, queuing them all and sending one
     */
    private void SendAllMatches()
    {
        if (matches.size() < 1 || sendingState == SendingState.SENDING)
            return;

        sendingState = SendingState.SENDING;
        sendAllButton.setVisibility(View.INVISIBLE);
        sendAllProgress.setVisibility(View.VISIBLE);
        queuedMatchesToSend = new ArrayList<>(matches.subList(1, matches.size() - 1));
        SendMatch(matches.get(0));

        Timer completeTimer = new Timer();
        // 15 second timeout
        completeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (queuedMatchesToSend.size() > 0)
                    MessageBox("Failed to send: " + queuedMatchesToSend.size() + " matches");
                sendAllButton.setVisibility(View.VISIBLE);
                sendAllProgress.setVisibility(View.INVISIBLE);
                sendingState = SendingState.WAITING;
            }
        }, 15000);
    }

    /**
     * Save all matches locally
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
        FileHandler.Write(FileHandler.Files.SCOUTER_FILE, output.toString());
    }

    /**
     * Save the current match locally and also send it, not saving duplicates
     */
    private void SaveCurrentMatch() { SaveMatch(GetCurrentInputValues(), false, false); }

    /**
     * Save the current match locally
     * @param localOnly whether to send the match or just save it locally
     * @param saveDuplicates whether to save even if the value has not been detected as changed
     */
    private void SaveMatch(JSONObject match, boolean localOnly, boolean saveDuplicates)
    {
        int matchNumber = -1;

        try
        {
            matchNumber  = Integer.valueOf((String)match.get(Constants.MATCH_NUMBER_JSON_TAG));
        }
        catch (Exception e)
        {
            Constants.Log(e);
            Constants.Log("Attempted to get match number of match in SaveMatch()");
        }

        // Check if something actually changed since the value was loaded
        if (match == null || match.toString().equals(lastValuesBeforeChange) && !saveDuplicates) {
            Constants.Log("Nothing changed, not saving: " + GetCurrentInputValues());
            return;
        }
        if (matchNumber < 0)
        {
            Constants.Log("Invalid Match Number in SaveMatch(): " + matchNumber);
            return;
        }

        // Existing match
        if (matchNumber <= matches.size())
        {
            matches.set(matchNumber - 1, GetCurrentInputValues());
        }
        // New match when we are on the last match or there are no matches yet
        else if (matchNumber + 1 == matches.size() || matches.size() == 0)
        {
            matches.add(match);
        }
        // Too large of a match number
        else
        {
            return;
        }
        // Write to bluetooth, after potentially saving match. Make sure actual match is there
        if (!localOnly && matches.size() >= matchNumber && matches.get(matchNumber - 1) != null)
        {
            SendMatch(matches.get(matchNumber - 1));
        }
        else
            Constants.Log("Saving local only");
    }

    /**
     * Send a match over bluetooth
     * @param match completed JSON object to send
     */
    private void SendMatch(JSONObject match)
    {
        // If currently doing send all don't allow any other activity
        if (sendingState == SendingState.SENDING || match == null) return;

        try
        {
            // Send the match over bluetooth
            BluetoothWrite(match.toString());
        Constants.Log("Output JSON: " + match);
        }
        catch (Exception e)
        {
        Constants.Log("Failure to send json match: " + match);
            Constants.Log(e);
        }
    }

    /**
     * Get the current values of user input
     * @return a JSON Object with values, notes, and team/match number
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
            Constants.Log(e);
            return null;
        }
    }

    /**
     * Load matches from the file into memory (Array<JSONObject> matches)
     */
    private void LoadMatches()
    {
        try
        {
            BufferedReader r = FileHandler.GetReader(FileHandler.Files.SCOUTER_FILE);
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
        Constants.Log("Failed to load contents of matches database: " + e.getMessage());
            Constants.Log(e);
        }
    }

    /**
     * Process given bluetooth input
     * @param msg the message sent to the main thread from a bluetooth thread
     *            Found in BluetoothClientActivity
     */
    private synchronized void ProcessBluetoothInput(Message msg)
    {
        try {
            final String message =
                    Constants.getScouterInputWithoutTag((String) msg.obj);
            final String tag =
                    Constants.getScouterInputTag((String) msg.obj);

            switch (tag) {
                case Constants.SERVER_TEAMS_RECEIVED_TAG:
                    if (queuedMatchesToSend.size() > 0 && sendingState == SendingState.SENDING)
                    {
                    Constants.Log("Server received last, sending again");
                        SendMatch(queuedMatchesToSend.get(0));
                        queuedMatchesToSend.remove(0);
                    }
                    else
                    {
                        sendingState = SendingState.WAITING;
                        sendAllButton.setVisibility(View.VISIBLE);
                        sendAllProgress.setVisibility(View.INVISIBLE);
                    }
                    break;
                case Constants.SCOUTER_SCHEMA_TAG:
                    final Context c = this;
                    // Show a confirmation dialog
                    Constants.OnConfirm("Received new schema, clear local schema?", this, () ->
                    {
                        FileHandler.Write(FileHandler.Files.SCHEMA_FILE, message);
                        SchemaHandler.Setup(
                                inputTable, // Table to setup the new schema on
                                SchemaHandler.LoadSchemaFromFile(), // Schema text
                                c); // Context
                        FileHandler.Write(FileHandler.Files.SCOUTER_FILE, "");
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
                                Constants.Log(e);
                            }
                        }
                        DisplayMatch(1);
                        SaveMatch(matches.get(GetDisplayedMatchNumber() - 1), true, true);
                        SaveAllMatches();
                    });
                    break;
                case Constants.SERVER_MESSAGE_TAG:
                    //Shows the message received from the server
                    MessageBox(message);
                    break;
                case Constants.SERVER_SUBMIT_TAG:
                    // If the match is still open show a countdown before sending
                    if (String.valueOf(GetDisplayedMatchNumber()).equals(message))
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
                            Constants.Log("Error processing match number request: " + message);
                            Constants.Log(e);
                        }
                    }
                    break;
                case Constants.SERVER_SEND_ALL_TAG:
                    SendAllMatches();
                    break;
                default:
                Constants.Log("Received unknown tag: " + tag);
            }
        }
        catch (Exception e)
        {
            Constants.Log(e);
        }
    }

    /**
     * Overriden from BluetoothClientActivity
     * @param msg the message sent to the thread
     */
    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MessageTypes.MESSAGE_INPUT: // Input received through bluetooth
                ProcessBluetoothInput(msg);
                break;
            case MessageTypes.MESSAGE_CONNECTED: // The device has connected
                connectionStatus.setImageResource(R.drawable.ic_network_wifi);
                break;
            case MessageTypes.MESSAGE_DISCONNECTED: // The device has disconnected
                connectionStatus.setImageResource(R.drawable.ic_network_off);
                break;
        }
    }


    /**
     * Count down for 15 seconds and then submit the current match, unless the dialog is canceled
     */
    private void SubmitCountDown()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        AlertDialog dialog;
        dialog = builder
                .setTitle("Auto submitting Soon...").setMessage("Submitting in 15 seconds")
                .setPositiveButton("Submit", (dialogInterface, i) ->
        {
            {
                // Submit
                SaveMatch(GetCurrentInputValues(), false, true);
                OnNextMatch();
                dialogInterface.dismiss();
            }
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
                    SaveMatch(GetCurrentInputValues(), false, true);
                    OnNextMatch();
                    dialog.dismiss();
                }
            }
        }.start();
    }

    /**
     * Trigger a confirmation box to prevent accidental home button click
     * @param item the android item that was selected
     * @return true if consumed. uses
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

    /**
     * Trigger a confirmation box to prevent certain disconnects - back button
     * @param keyCode the keycode, only checks for the back button
     * @param event the event data
     * @return true if the event was consumed
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

    /**
     * Run setup after bluetooth once bluetooth is setup
     * @param requestCode the permissions request code assigned when it ran
     * @param permissions the permissions requested list
     * @param grantResults the results of the granted permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_ENABLE_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            Constants.Log("Permission granted");
                SetupAfterBluetooth();
            }
            else
            {
                CheckBluetooth();
            }
        }
    }
}
