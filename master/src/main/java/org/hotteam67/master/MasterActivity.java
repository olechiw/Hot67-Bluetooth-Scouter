package org.hotteam67.master;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.OnDownloadResultListener;
import org.hotteam67.common.SchemaHandler;
import org.hotteam67.common.TBAHandler;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server activity, handles the connections to all of the devices and input
 */
public class MasterActivity extends BluetoothServerActivity {

    private static final int REQUEST_PREFERENCES = 2;
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    /**
     * Log tag
     */
    private static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";

    private String lastMatchNumber = "0";
    /**
     * List of team numbers for the last match, used to check if a match was received already
     */
    private List<String> lastMatchTeamNumbers = new ArrayList<>();
    private int matchesReceived = 0;

    /**
     * All of the received matches, stored in json, written/read from local database
     */
    private JSONObject jsonDatabase;

    /**
     * Show a messagebox on the Server
     * @param text the text to display in the message box
     */
    private void MessageBox(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            Constants.Log(e);
        }
    }

    private Button connectButton;
    private EditText serverLogText;

    /**
     * Setup UI and the handler for communicating with bluetooth threads
     * @param savedInstanceState saved instance state is ignored
     */
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        SetHandler(new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                handle(msg);
            }
        });

        setupPermissions();

        loadJsonDatabase();
    }

    /**
     * Load options menu from xml
     * @param menu the menu to populate
     * @return true, menu was created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    /**
     * Get a string input, and run an oncomplete event
     * @param prompt the text prompt the user will see when asked to input a string
     * @param defaultValue the default value of the string, if any
     * @param onInput the event to run when input is received, assuming it isn't canceled
     */
    private void GetString(final String prompt, final String defaultValue, final Constants.StringInputEvent onInput) {
        final EditText input = new EditText(this);
        input.setText(defaultValue);

        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            dlg.setTitle("");
            dlg.setMessage(prompt);
            dlg.setView(input);
            dlg.setPositiveButton("Ok", (dialog, which) ->
                    onInput.Run(input.getText().toString()));
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
        Constants.Log("Failed to create dialog: " + e.getMessage());
        }
    }

    /**
     * Handle all of the menu items, such as setup schema or send schema
     * @param item the menu item selected, from the xml list
     * @return true, event was consumed
     */
    @Override
    public synchronized boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menuItemSetupSchema:
            {
                final Context c = this;
                Intent launchSchemaActivityIntent = new Intent(c, SchemaActivity.class);
                startActivity(launchSchemaActivityIntent);
                break;
            }
            case R.id.menuItemSendSchema:
            {
                Constants.OnConfirm("Send Schema?", this, () ->
                {
                    try
                    {
                        // Obtain schema
                        String schema = SchemaHandler.LoadSchemaFromFile().toString();

                        int devices = WriteAllDevices((Constants.SCOUTER_SCHEMA_TAG + schema).getBytes());
                        VisualLog("Wrote schema to " + devices + " devices");
                    }
                    catch (Exception e)
                    {
                        VisualLog("Failed to send schema to devices: " + e.getMessage());
                        Constants.Log(e);
                    }
                });
                break;
            }
            case R.id.menuItemSendMatches:
            {
                sendEventMatches();
                break;
            }
            case R.id.menuItemClearDatabase:
            {
                Constants.OnConfirm("Clear Local Database?", this, () ->
                {
                    jsonDatabase = new JSONObject();
                    try
                    {
                        FileHandler.Write(FileHandler.Files.SERVER_FILE, "");
                    }
                    catch (Exception e)
                    {
                        Constants.Log(e);
                    }
                });
            }
        }

        return true;
    }

    /**
     * Setup the user interface and event handlers once bluetooth has been confirmed as active
     */
    private void setupUI()
    {
        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(view ->
                Constants.OnConfirm("Disconnect all existing devices?",
                        this, this::Connect));

        serverLogText = findViewById(R.id.serverLog);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayShowTitleEnabled(false);

        ImageButton configureButton = toolbar.findViewById(R.id.configureButton);
        configureButton.setOnClickListener(view -> configurePreferences());

        ImageButton downloadMatchesButton = findViewById(R.id.matchesDownloadButton);
        downloadMatchesButton.setOnClickListener(view -> downloadEventMatches());


        // Classic useless feature
        downloadMatchesButton.setOnLongClickListener(view ->
        {

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (v != null)
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }else
            {
                //deprecated in API 26
                if (v != null)
                    v.vibrate(500);
            }

            sendEventMatches();

            return true;
        });

        findViewById(R.id.messageButton)
                .setOnClickListener(view -> GetString("Enter Message:", "",
                input -> WriteAllDevices((Constants.SERVER_MESSAGE_TAG + input).getBytes())));

        findViewById(R.id.submitButton)
                .setOnClickListener(view ->
                        GetString("Get Match Number", lastMatchNumber, (input) ->
                                WriteAllDevices((Constants.SERVER_SUBMIT_TAG + input).getBytes())));

        findViewById(R.id.syncButton)
                .setOnClickListener(view -> Constants.OnConfirm("Sync All Matches?",
                        this, () ->
                        saveJsonObject(jsonDatabase, true)));
    }


    /**
     * Download the event matches given an event key from user input
     */
    private void downloadEventMatches()
    {
        GetString("Enter Event Key:", "", eventKey ->
                TBAHandler.Matches(eventKey, new OnDownloadResultListener<List<TBAHandler.Match>>() {
                    @Override
                    public void onComplete(List<TBAHandler.Match> result) {
                        try
                        {
                            StringBuilder matchesBuilder = new StringBuilder();

                            for (TBAHandler.Match match : result)
                            {
                                List<String> redTeams = match.redTeams;
                                List<String> blueTeams = match.blueTeams;
                                StringBuilder rowBuilder = new StringBuilder();

                                for (int t = 0; t < redTeams.size(); ++t)
                                {
                                    rowBuilder.append(redTeams.get(t));
                                    rowBuilder.append(",");
                                }
                                for (int t = 0; t < blueTeams.size(); ++t)
                                {
                                    rowBuilder.append(blueTeams.get(t));
                                    if (t + 1 < blueTeams.size())
                                        rowBuilder.append(",");
                                }
                                rowBuilder.append("\n");
                                matchesBuilder.append(rowBuilder.toString());
                            }
                            MessageBox("Downloaded Matches: " +
                                    matchesBuilder.toString().split("\n").length);
                            FileHandler.Write(FileHandler.Files.MATCHES_FILE, matchesBuilder.toString());
                        }
                        catch (Exception e)
                        {
                            Constants.Log(e);
                        }
                    }

                    @Override
                    public void onFail() {
                        MessageBox("Failed to download matches with key: " + eventKey);
                    }
                }));
    }


    /**
     * Splits all of the event schedule into six, and sends all of the teams to each device, in the
     * order they are found in the CSV match schedule file
     */
    private void sendEventMatches()
    {
        Constants.OnConfirm("Send Matches?", this, () ->
        {
            try {
                // Starting value for each piece of information is the tag.
                List<String> deviceTeams = new ArrayList<>(Arrays.asList(
                        Constants.SCOUTER_TEAMS_TAG,
                        Constants.SCOUTER_TEAMS_TAG,
                        Constants.SCOUTER_TEAMS_TAG,
                        Constants.SCOUTER_TEAMS_TAG,
                        Constants.SCOUTER_TEAMS_TAG,
                        Constants.SCOUTER_TEAMS_TAG
                ));

                List<String> matches =
                        new ArrayList<>(
                                Arrays.asList(
                                        FileHandler.LoadContents(FileHandler.Files.MATCHES_FILE)
                                                .split("\n")
                                )
                        );

                int failed = 0;
                VisualLog("Sending Teams");
                for (int m = 0; m < matches.size(); ++m) {
                    // Six teams for
                    String[] teams = matches.get(m).split(",");
                    // Has to have six teams
                    if (teams.length != deviceTeams.size()) {
                        //VisualLog("Dropping match: " + matches.get(m));
                        ++failed;
                    } else {
                        for (int i = 0; i < deviceTeams.size(); ++i) {
                            // Append to keep the match tag, and all previous iterations
                            deviceTeams.set(i, deviceTeams.get(i) + teams[i]);
                            // If not the last match, add a comma
                            if (m + 1 < matches.size())
                                deviceTeams.set(i, deviceTeams.get(i) + ",");
                        }
                    }
                }

                int devices = 0;
                for (int i = 0; i < deviceTeams.size(); ++i) {
                    devices = WriteDevice((deviceTeams.get(i).getBytes()), i);
                }


                String s;

                if (devices == 1)
                    s = "Sent to 1 device!";
                else
                    s = "Sent to " + devices + " devices!";

                if (failed > 0)
                    s += " Dropped " + failed + " matches";
                MessageBox(s);
            }
            catch (Exception e)
            {
            Constants.Log("Failed to send matches from stored file!");
                Constants.Log(e);
            }
        });
    }

    /**
     * Setup the permissions if bluetooth is not setup, otherwise setup the user interface
     */
    private void setupPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        Constants.Log("Permission granted");
            setupBluetooth(this::setupUI);
        }
        else
        {
        Constants.Log("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
        }
    }


    /**
     * When permissions are returned, check if bluetooth was enabled
     * @param requestCode permissions request code assigned at request time
     * @param permissions the permissions requested
     * @param grantResults the results of the requested permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_ENABLE_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            Constants.Log("Permission granted");
                setupBluetooth(this::setupUI);
            }
            else
            {
                setupPermissions();
            }
        }
    }

    /**
     * Configure the preferences by starting the preferences activity
     */
    private void configurePreferences()
    {
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivityForResult(intent, REQUEST_PREFERENCES);
    }

    private int currentLog = 1;
    // Log to the end user about things like connected and disconnected devices
    private void VisualLog(String text)
    {
        serverLogText.append(currentLog + ": " + text + "\n");
        currentLog++;
    }

    // Handle an input message from one of the bluetooth threads
    /**
     * Handle, the function that is attached to the handler of subclass BluetoothServerActivity and
     * receives messages about connections and bluetooth input
     */
    @SuppressLint("SetTextI18n")
    private synchronized void handle(Message msg) {

        switch (msg.what) {
            case Messages.MESSAGE_INPUT:

                String message = (String) msg.obj;
                if (message == null || message.trim().isEmpty())
                    return;

                int id = msg.arg2; // The id of the thread received from
                // Send a "message received" in the form of a match tag
                try
                {
                    // Send on the connected thread
                    WriteDevice(Constants.SERVER_TEAMS_RECEIVED_TAG.getBytes(), id - 1);
                }
                catch (IndexOutOfBoundsException e)
                {
                    Constants.Log(e);
                    Constants.Log("Failed to find connected thread: " + id + " was it disposed?");
                }
                catch (Exception e)
                {
                    Constants.Log(e);
                    Constants.Log("Exception occured in trying to write team received tag: " + e.getMessage());
                }
                //m_sendButton.setText(message);

                try {
                    saveJsonObject(new JSONObject(message));
                } catch (Exception e) {
                    Constants.Log("Failed to load and send input json, most likely not logged in:" + message);
                    Constants.Log(e);
                }

                break;
            case Messages.MESSAGE_CONNECTING:
                String name = (String)msg.obj;
                VisualLog("Attempting Connection with " + name);

                break;
            case Messages.MESSAGE_CONNECTION_FAILED:
                VisualLog("Connection Timed Out");
                break;
            case Messages.MESSAGE_CONNECTED:
            Constants.Log("Received Connect");
            Constants.Log("Size of connected threads: " + GetDevices());
                VisualLog("Device Connected!");
                connectButton.setText("Connected Devices: " + String.valueOf(GetDevices()));

                break;
            case Messages.MESSAGE_DISCONNECTED:
                //MessageBox("DISCONNECTED FROM DEVICE");
            Constants.Log("Received Disconnect");
                VisualLog("Device Disconnected!");
            Constants.Log("Size of connected threads: " + GetDevices());
                connectButton.setText("Connected Devices: " + String.valueOf(GetDevices()));
                break;
            default:
            Constants.Log("Received Message: " + msg.what);
        }
    }

    /**
     * Load the local database into the json object in memory, or handle exceptions
     */
    private void loadJsonDatabase()
    {
        String fileContents = FileHandler.LoadContents(FileHandler.Files.SERVER_FILE);
        if (fileContents.trim().isEmpty()) {
            jsonDatabase = new JSONObject();
        }
        else
        {
            try
            {
                jsonDatabase = new JSONObject(fileContents);
            }
            catch (Exception e)
            {
                jsonDatabase = new JSONObject();
            }
        }
    }

    /**
     * Save the JSON object from memory to disk
     */
    private void saveJsonDatabase()
    {
        FileHandler.Write(FileHandler.Files.SERVER_FILE, jsonDatabase.toString());
    }

    /**
     * Save the json object from memory to the firebase database, as a sub-object of the event name
     * tag, as opposed to writing to the root url and overwriting all data
     * @param json the object to send
     */
    private void saveJsonObject(JSONObject json)
    {
        saveJsonObject(json, false);
    }

    /**
     * Write a json object to firebase. Either write to rootUrl and overwrite everything with the object,
     * or determine its tag from teamNumber_matchNumber and write it as a subtag of the configured
     * event name
     * @param json the object to write
     * @param useRootUrl whether to overwrite everything or write as a sub object
     */
    private void saveJsonObject(final JSONObject json, boolean useRootUrl)
    {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String eventName = (String) prefs.getAll().get(Constants.PREF_EVENTNAME);
            String eventUrl = (String) prefs.getAll().get(Constants.PREF_DATABASEURL);
            String apiKey = (String) prefs.getAll().get(Constants.PREF_APIKEY);
            if (eventName == null || eventUrl == null || apiKey == null)
                return;
            if (!eventName.endsWith("/"))
                eventUrl += "/";
            if (!eventUrl.startsWith("https://"))
                try {
                    eventUrl = eventUrl.replace("http://", "https://");
                } catch (Exception e) {
                    Constants.Log(e);
                }

            String finalUrl = eventUrl + eventName;

            // Oncomplete event only used for sync-all to notify completion
            Runnable onComplete = () -> {};
            if (useRootUrl)
            {
                finalUrl += ".json?auth=" + apiKey;
                onComplete = () -> this.VisualLog("Synced All!");
            }
            else {
                String tag =
                        json.get(Constants.TEAM_NUMBER_JSON_TAG).toString() + "_" +
                                json.get(Constants.MATCH_NUMBER_JSON_TAG).toString();
                finalUrl += "/" + tag + ".json?auth=" + apiKey;
            }


            AsyncUploadTask uploadTask = new AsyncUploadTask(finalUrl, onComplete);
            uploadTask.execute(json);

            if (useRootUrl)
                return;
            // Save locally
            try {
                String matchNumber = (String) json.get(Constants.MATCH_NUMBER_JSON_TAG);

                if (matchNumber.equals(lastMatchNumber))
                {
                    //noinspection SuspiciousMethodCalls
                    if (!lastMatchTeamNumbers.contains(json.get(Constants.TEAM_NUMBER_JSON_TAG)))
                    {
                        matchesReceived++;
                        lastMatchTeamNumbers.add((String) json.get(Constants.TEAM_NUMBER_JSON_TAG));
                    }
                    serverLogText.setText(
                            "Last Match: " + lastMatchNumber + " Received: " + matchesReceived + "\n"
                    );
                }
                else
                {
                    lastMatchNumber = matchNumber;
                    matchesReceived = 1;
                    lastMatchTeamNumbers = new ArrayList<>();
                    lastMatchTeamNumbers.add((String) json.get(Constants.TEAM_NUMBER_JSON_TAG));
                    serverLogText.setText(
                            "Last Match: " + matchNumber + " Received: " + matchesReceived + "\n"
                    );
                }
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }
            String tag =
                    json.get(Constants.TEAM_NUMBER_JSON_TAG).toString() + "_" +
                            json.get(Constants.MATCH_NUMBER_JSON_TAG).toString();
            jsonDatabase.put(tag, json);
            saveJsonDatabase();
        }
        catch (Exception e)
        {
            Constants.Log(e);
        }
    }

    private final Context context = this;

    /**
     * Upload task to run that uploads the given JSONObject and handles issues
     */
    private class AsyncUploadTask extends AsyncTask<JSONObject, Void, JSONObject> {

        private final String uploadUrl;
        private final Runnable onCompleteEvent;
        AsyncUploadTask(String url, Runnable onComplete)
        {
            uploadUrl = url;
            onCompleteEvent = onComplete;
        }

        protected JSONObject doInBackground(JSONObject... json)
        {
            try {
                // Each individual match gets a tag, with the team number then match number,
                // so unique for every team's match. For instance:
                //
                // 67_1 for team 67 match one,
                // or 2048_15 for team 2048 match 15
                //
                // This is simply to make sure no duplicate matches are recorded for any team
                if (json != null && json.length > 0)
                {
                    String jsonString = json[0].toString();
                    Log.d("BluetoothScouter", "Outputting json: " + jsonString);

                    HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setDoOutput(true);

                    Log.d("BLUETOOTH_SCOUTER", "Sending request to url: "
                            + uploadUrl);
                    try {
                        conn.getOutputStream().write(jsonString.getBytes());
                    } catch (Exception e) {
                        Constants.Log(e);
                    }

                    Log.d("BLUETOOTH_SCOUTER", "Received response code:" + conn.getResponseCode());

                    String resp = conn.getResponseMessage();
                    Log.d("BLUETOOTH_SCOUTER", "Response: " + resp);
                }
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }

            if (json != null && json.length > 0)
                return json[0];
            else
                return null;
        }

        protected void onPostExecute(JSONObject j)
        {
            onCompleteEvent.run();
        }
    }


    /**
     * Save the database when the activity ends, just in case.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();


        saveJsonDatabase();
    }
}
