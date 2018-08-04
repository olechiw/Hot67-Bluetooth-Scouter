package org.hotteam67.bluetoothserver;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.hotteam67.common.TBAHandler;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class ServerActivity extends BluetoothServerActivity {

    public static final int REQUEST_PREFERENCES = 2;
    public static final int REQUEST_ENABLE_PERMISSION = 3;

    // Simple log function
    protected void l(String s)
    {
        Log.d(TAG, s);
    }

    // The log tag
    public static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";

    String lastMatchNumber = "0";
    List<String> lastMatchTeamNumbers = new ArrayList<>();
    int lastMatchReceived = 0;

    // Current database in json format
    private JSONObject jsonDatabase;

    // Display a popup box (not a MessageBox, LOL)
    protected void MessageBox(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            l("Failed to create dialog: " + e.getMessage());
        }
    }

    Button connectButton;
    EditText serverLogText;

    ImageButton configureButton;
    ImageButton downloadMatchesButton;

    // When the app is initialized, setup the UI and the bluetooth adapter
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    private void GetString(final String prompt, final SchemaActivity.StringInputEvent onInput) {
        final EditText input = new EditText(this);

        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
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
            l("Failed to create dialog: " + e.getMessage());
        }
    }

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
                    // Obtain schema
                    String schema = SchemaHandler.LoadSchemaFromFile();

                    try
                    {
                        int devices = WriteAllDevices((Constants.SCOUTER_SCHEMA_TAG + schema).getBytes());
                        VisualLog("Wrote schema to " + devices + " devices");
                    }
                    catch (Exception e)
                    {
                        VisualLog("Failed to send schema to devices: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                break;
            }
            case R.id.menuItemSendMatches:
            {
                sendEventMatches();
                break;
            }
            case R.id.menuItemSendMessage:
            {
                GetString("Enter Message:", input ->
                {
                    WriteAllDevices((Constants.SERVER_MESSAGE_TAG + input).getBytes());
                });
                break;
            }
            case R.id.menuItemSyncAll:
            {
                Constants.OnConfirm("Sync All Matches?", this, () ->
                        saveJsonObject(jsonDatabase, true));
                break;
            }
            case R.id.menuItemClearDatabase:
            {
                Constants.OnConfirm("Clear Local Database?", this, () ->
                {
                    jsonDatabase = new JSONObject();
                    try
                    {
                        FileHandler.Write(FileHandler.SERVER_FILE, "");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });
            }
        }

        return true;
    }

    private void setupUI()
    {
        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(view -> Connect());
        serverLogText = findViewById(R.id.serverLog);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayShowTitleEnabled(false);

        configureButton = toolbar.findViewById(R.id.configureButton);
        configureButton.setOnClickListener(view -> configure());

        downloadMatchesButton = findViewById(R.id.matchesDownloadButton);
        downloadMatchesButton.setOnClickListener(view -> downloadEventMatches());


        // Classic useless feature
        downloadMatchesButton.setOnLongClickListener(view ->
        {

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null)
            {
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else
                {
                    //deprecated in API 26
                    v.vibrate(500);
                }
            }

            sendEventMatches();

            return true;
        });
    }


    private void downloadEventMatches()
    {
        final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        GetString("Enter Event Key:", eventKey ->
                TBAHandler.Matches(currentYear + eventKey, result ->
                {
                    try
                    {
                        StringBuilder matchesBuilder = new StringBuilder();

                        for (List<List<String>> match : result)
                        {
                            List<String> redTeams = match.get(0);
                            List<String> blueTeams = match.get(1);
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
                        FileHandler.Write(FileHandler.MATCHES_FILE, matchesBuilder.toString());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }));
    }


    // Loads all of the loaded matches, split into 6 devices, and then sends them as one giant string
    private void sendEventMatches()
    {
        Constants.OnConfirm("Send Matches?", this, new Runnable() {
            @Override
            public void run() {
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
                                            FileHandler.LoadContents(FileHandler.MATCHES_FILE)
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
                    l("Failed to send matches from stored file!");
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            setupBluetooth(this::setupUI);
        }
        else
        {
            l("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
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
                setupBluetooth(this::setupUI);
            }
            else
            {
                setupPermissions();
            }
        }
    }

    // Configure the current scouting schema and database connection
    private void configure()
    {
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivityForResult(intent, REQUEST_PREFERENCES);
    }

    int currentLog = 1;
    // Log to the end user about things like connected and disconnected devices
    private void VisualLog(String text)
    {
        serverLogText.append(currentLog + ": " + text + "\n");
        currentLog++;
    }

    // Handle an input message from one of the bluetooth threads
    @SuppressLint("SetTextI18n")
    protected synchronized void handle(Message msg) {
        switch (msg.what) {
            case MESSAGE_INPUT:

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
                    e.printStackTrace();
                    l("Failed to find connected thread: " + id + " was it disposed?");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    l("Exception occured in trying to write team received tag: " + e.getMessage());
                }
                //m_sendButton.setText(message);

                try {
                    saveJsonObject(new JSONObject(message));
                } catch (Exception e) {
                    l("Failed to load and send input json, most likely not logged in:" + message);
                    e.printStackTrace();
                }

                break;
            case MESSAGE_OTHER:
                String t = new String((byte[]) msg.obj);

                l("Received Message Other: " + t);

                break;
            case MESSAGE_CONNECTED:
                l("Received Connect");
                l("Size of connected threads: " + GetDevices());
                VisualLog("Device Connected!");
                connectButton.setText("Connected Devices: " + String.valueOf(GetDevices()));

                break;
            case MESSAGE_DISCONNECTED:
                //MessageBox("DISCONNECTED FROM DEVICE");
                l("Received Disconnect");
                VisualLog("Device Disconnected!");
                l("Size of connected threads: " + GetDevices());
                connectButton.setText("Connected Devices: " + String.valueOf(GetDevices()));
                break;
            default:
                l("Received Message: " + msg.what);
        }
    }

    private void loadJsonDatabase()
    {
        String fileContents = FileHandler.LoadContents(FileHandler.SERVER_FILE);
        if (fileContents == null || fileContents.trim().isEmpty()) {
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

    private void saveJsonDatabase()
    {
        FileHandler.Write(FileHandler.SERVER_FILE, jsonDatabase.toString());
    }

    private void saveJsonObject(JSONObject json)
    {
        saveJsonObject(json, false);
    }
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
                    e.printStackTrace();
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


            AsyncUploadTask uploadTask = new AsyncUploadTask(finalUrl, json, onComplete);
            uploadTask.execute(json);

            if (useRootUrl)
                return;
            // Save locally
            try {
                String matchNumber = (String) json.get(Constants.MATCH_NUMBER_JSON_TAG);

                if (matchNumber.equals(lastMatchNumber))
                {
                    if (!lastMatchTeamNumbers.contains(json.get(Constants.TEAM_NUMBER_JSON_TAG)))
                    {
                        lastMatchReceived++;
                        lastMatchTeamNumbers.add((String) json.get(Constants.TEAM_NUMBER_JSON_TAG));
                    }
                    serverLogText.setText(
                            "Last Match: " + lastMatchNumber + " Received: " + lastMatchReceived + "\n"
                    );
                }
                else
                {
                    lastMatchNumber = matchNumber;
                    lastMatchReceived = 1;
                    lastMatchTeamNumbers = new ArrayList<>();
                    lastMatchTeamNumbers.add((String) json.get(Constants.TEAM_NUMBER_JSON_TAG));
                    serverLogText.setText(
                            "Last Match: " + matchNumber + " Received: " + lastMatchReceived + "\n"
                    );
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (!useRootUrl) {
                String tag =
                        json.get(Constants.TEAM_NUMBER_JSON_TAG).toString() + "_" +
                                json.get(Constants.MATCH_NUMBER_JSON_TAG).toString();
                jsonDatabase.put(tag, json);
            }
            saveJsonDatabase();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private final Context context = this;
    private class AsyncUploadTask extends AsyncTask<JSONObject, Void, JSONObject> {

        private String uploadUrl;
        private JSONObject jsonData;
        private Runnable onCompleteEvent;
        AsyncUploadTask(String url, JSONObject data, Runnable onComplete)
        {
            uploadUrl = url;
            jsonData = data;
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
                        e.printStackTrace();
                    }

                    Log.d("BLUETOOTH_SCOUTER", "Received response code:" + conn.getResponseCode());

                    String resp = conn.getResponseMessage();
                    Log.d("BLUETOOTH_SCOUTER", "Response: " + resp);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
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


    // When the activity is finished, clean up all of the bluetooth elements
    @Override
    public void onDestroy()
    {
        super.onDestroy();


        saveJsonDatabase();
    }
}
