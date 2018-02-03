package org.hotteam67.bluetoothserver;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.models.Match;
import com.example.bluetoothserver.R;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ServerActivity extends AppCompatActivity {

    // Messages, for when any event happens, to be sent to the main thread
    public static final int MESSAGE_INPUT = 0;
    public static final int MESSAGE_OTHER = 1;
    public static final int MESSAGE_DISCONNECTED = 2;
    public static final int MESSAGE_CONNECTED = 3;

    public static final int REQUEST_BLUETOOTH = 1;
    public static final int REQUEST_PREFERENCES = 2;
    public static final int REQUEST_ENABLE_PERMISSION = 3;

    // Whether bluetooth hardware setup failed, such as nonexistent bluetooth device
    private boolean bluetoothFailed = false;

    // Message Handler, simple!
    Handler m_handler;

    // Simple log function
    protected void l(String s)
    {
        Log.d(TAG, s);
    }

    // The log tag
    public static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";

    // Send a specific message, from the above list
    public synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    // Number of active and allowed devices
    private int allowedDevices = 7;

    String lastMatchNumber = "0";
    int lastMatchReceived = 0;

    // Current database in json format
    private JSONObject jsonDatabase;

    // Bluetooth hardware adapter
    protected BluetoothAdapter m_bluetoothAdapter;

    // Display a popup box (not a toast, LOL)
    protected void toast(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            l("Failed to create dialog: " + e.getMessage());
        }
    }

    TextView connectedDevicesText;
    EditText serverLogText;

    ImageButton configureButton;
    ImageButton downloadMatchesButton;

    // When the app is initialized, setup the UI and the bluetooth adapter
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        m_handler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                handle(msg);
            }
        };

        setupPermissions();

        loadJsonDatabase();
    }


    // Initialize the bluetooth hardware adapter
    private synchronized void setupBluetooth()
    {
        l("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
            l("Bluetooth not detected");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH);
        }
        else
        {
            setupThreads();

            setupUI();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    @Override
    public synchronized boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menuItemSetupSchema:
            {
                Intent launchSchemaActivityIntent = new Intent(this, SchemaActivity.class);
                startActivity(launchSchemaActivityIntent);
                break;
            }
            case R.id.menuItemSendSchema:
            {
                // Obtain schema
                String schema = SchemaHandler.LoadSchemaFromFile();

                try {
                    // Send to each device
                    for (ConnectedThread device : connectedThreads) {
                        device.write((Constants.SCOUTER_SCHEMA_TAG + schema).getBytes());
                    }
                    VisualLog("Wrote schema to " + connectedThreads.size() + " devices");
                }
                catch (Exception e)
                {
                    VisualLog("Failed to send schema to devices: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            }
            case R.id.menuItemSendMatches:
            {
                sendEventMatches();
                break;
            }
        }

        return true;
    }

    private void setupUI()
    {
        connectedDevicesText = (TextView) findViewById(R.id.connectedDevicesText);
        serverLogText = (EditText) findViewById(R.id.serverLog);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayShowTitleEnabled(false);

        configureButton = toolbar.findViewById(R.id.configureButton);
        configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configure();
            }
        });

        downloadMatchesButton = (ImageButton) findViewById(R.id.matchesDownloadButton);
        downloadMatchesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                downloadEventMatches();
            }
        });


        // Classic useless feature
        downloadMatchesButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(500);

                sendEventMatches();

                return true;
            }
        });

        refreshFirebaseAuth();
    }


    private void downloadEventMatches()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText matchKeyInput = new EditText(this);

        final Context c = this;

        builder.setView(matchKeyInput);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                TBA.setID("HOT67", "BluetoothScouter", "V1");
                TBA tba = new TBA();
                Settings.GET_EVENT_MATCHES = true;

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
                StrictMode.setThreadPolicy(policy);

                l("Fetching");
                try
                {
                    StringBuilder s = new StringBuilder();
                    Event e = tba.getEvent(matchKeyInput.getText().toString(),
                            Integer.valueOf(new SimpleDateFormat("yyyy", Locale.US).format(new Date())));
                    toast(e.matches.length + " Matches Loaded");
                    l("Obtained event: " + e.name);
                    l("Year: " + new SimpleDateFormat("yyyy", Locale.US).format(new Date()));
                    l("Matches: " + e.matches.length);
                    for (Match m : e.matches)
                    {
                        if (m.comp_level.equals("qm"))
                        {
                            for (String t : m.redTeams)
                            {
                                s.append(t.replace("frc", "")).append(",");
                            }
                            for (int t = 0; t < m.blueTeams.length; ++t)
                            {
                                s.append(m.blueTeams[t].replace("frc", ""));
                                if (t + 1 != m.blueTeams.length)
                                    s.append(",");
                            }
                            s.append("\n");
                        }
                    }
                    FileHandler.Write(FileHandler.SERVER_MATCHES, s.toString());
                }
                catch (Exception e)
                {
                    toast("Failed to load matches");
                    Log.e("[Matches Fetcher]", "Failed to get event: " + e.getMessage(), e);
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {

            }
        }).setTitle("Enter Match Key:").create().show();
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
                                            FileHandler.LoadContents(FileHandler.SERVER_MATCHES)
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
                            VisualLog("Dropping match: " + matches.get(m));
                            ++failed;
                        } else {
                            for (int i = 0; i < teams.length; ++i) {
                                // Append to keep the match tag, and all previous iterations
                                deviceTeams.set(m, deviceTeams.get(i) + teams[i]);
                                // If not the last match, add a comma
                                if (m + 1 < matches.size())
                                    deviceTeams.set(m, deviceTeams.get(i) + ",");
                            }
                        }
                    }

                    for (int i = 0; i < connectedThreads.size(); ++i) {
                        connectedThreads.get(i).write(deviceTeams.get(i).getBytes());
                    }


                    String s;

                    if (connectedThreads.size() == 1)
                        s = "Sent to 1 device!";
                    else
                        s = "Sent to " + connectedThreads.size() + " devices!";

                    if (failed > 0)
                        s += " Dropped " + failed + " matches";
                    toast(s);
                }
                catch (Exception e)
                {
                    l("Failed to send matches from stored file!");
                    e.printStackTrace();
                }
            }
        });
    }


    //
    // This is to handle the enable bluetooth activity,
    // and disable all attempts at bluetooth functionality
    // if for some reason the user denies permission
    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if (requestCode==REQUEST_BLUETOOTH)
        {
            bluetoothFailed = resultCode != RESULT_OK;
            setupThreads();
        }
    }

    private void setupPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            setupBluetooth();
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
                setupBluetooth();
            }
            else
            {
                setupPermissions();
            }
        }
    }

    private void refreshFirebaseAuth()
    {
    }

    // Initialize the accept bluetooth connections thread
    private void setupThreads()
    {
        if (!bluetoothFailed) {
            l("Setting up accept thread");
            acceptThread = new AcceptThread();

            l("Running accept thread");
            acceptThread.start();
        }
        else
            l("Attempted to setup threads, but bluetooth setup has failed");
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
                    connectedThreads.get(id - 1).write(Constants.SERVER_TEAMS_RECEIVED_TAG.getBytes());
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
                l("Size of connected threads: " + connectedThreads.size());
                VisualLog("Device Connected!");
                connectedDevicesText.setText("Connected Devices: " + String.valueOf(connectedThreads.size()));

                break;
            case MESSAGE_DISCONNECTED:
                //toast("DISCONNECTED FROM DEVICE");
                l("Received Disconnect");
                VisualLog("Device Disconnected!");
                l("Size of connected threads: " + connectedThreads.size());
                connectedDevicesText.setText("Connected Devices: " + String.valueOf(connectedThreads.size()));
                break;
            default:
                l("Received Message: " + msg.what);
        }
    }

    private void loadJsonDatabase()
    {
        String fileContents = FileHandler.LoadContents(FileHandler.SERVER_DATABASE);
        if (fileContents == null || fileContents.trim().isEmpty()) {
            jsonDatabase = new JSONObject();
            return;
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
        FileHandler.Write(FileHandler.SERVER_DATABASE, jsonDatabase.toString());
    }

    private void saveJsonObject(JSONObject json) throws JSONException, IOException
    {
        AsyncUploadTask uploadTask = new AsyncUploadTask();
        uploadTask.execute(json);
    }

    private final Context context = this;
    private class AsyncUploadTask extends AsyncTask<JSONObject, Void, JSONObject> {
        protected JSONObject doInBackground(JSONObject... json)
        {
            try {
                // Each individual match gets a tag, with the team number then match number,
                // so unique for every team's match. For instance:
                //
                // 671 for team 67 match one,
                // or 204815 for team 2048 match 15
                //
                // This is simply to make sure no duplicate matches are recorded for any team

                String tag =
                        json[0].get(Constants.TEAM_NUMBER_JSON_TAG).toString() + "_" +
                                json[0].get(Constants.MATCH_NUMBER_JSON_TAG).toString();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String eventname = (String) prefs.getAll().get(Constants.PREF_EVENTNAME);
                String eventUrl = (String) prefs.getAll().get(Constants.PREF_DATABASEURL);
                String apiKey = (String) prefs.getAll().get(Constants.PREF_APIKEY);
                l(tag);
                if (!eventUrl.endsWith("/")) eventUrl += "/";

                JSONObject outputObject = json[0];
                JSONObject taggedObject = new JSONObject();
                taggedObject.put(tag, outputObject);
                // outputObject = taglessObject;

                String url;

                // Test for eventName
                /*
                HttpURLConnection testConn = (HttpURLConnection) new URL(eventUrl + eventname)
                        .openConnection();
                if (testConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                {
                    JSONObject tmp = new JSONObject();
                    tmp.put(eventname, outputObject);
                    outputObject = tmp;
                    url = eventUrl;
                }
                else
                {
                    url = eventUrl + eventname;
                }*/

                url = eventUrl + eventname + "/" + tag + ".json?auth=" + apiKey;

                String jsonString = outputObject.toString();
                Log.d("BluetoothScouter", "Outputting json: " + jsonString);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);

                try{
                    conn.getOutputStream().write(jsonString.getBytes());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                // Save locally
                jsonDatabase.put(tag, outputObject);
                saveJsonDatabase();


                Log.d("BLUETOOTH_SCOUTER", "Sending request to url: "
                        + url + "\n Received response code: " + conn.getResponseCode());

                String resp = conn.getResponseMessage();
                Log.d("BLUETOOTH_SCOUTER", "Response: " + resp);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return json[0];
        }

        protected void onPostExecute(JSONObject j)
        {
            try {
                /*
                VisualLog("Received Match Number: "
                        + j.get(Constants.MATCH_NUMBER_JSON_TAG)
                        + " For Team Number: "
                        + j.get(Constants.TEAM_NUMBER_JSON_TAG));
                        */
                String matchNumber = (String) j.get(Constants.MATCH_NUMBER_JSON_TAG);

                if (matchNumber.equals(lastMatchNumber))
                {
                    lastMatchReceived++;
                    serverLogText.setText(
                            "Last Match: " + lastMatchNumber + " Received: " + lastMatchReceived + "\n"
                    );
                }
                else
                {
                    lastMatchNumber = matchNumber;
                    lastMatchReceived = 1;
                    serverLogText.setText(
                            "Last Match: " + matchNumber + " Received: " + lastMatchReceived + "\n"
                    );
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


    // Accept incoming bluetooth connections thread, actual member and the definition
    AcceptThread acceptThread;
    private class AcceptThread extends Thread {
        public final BluetoothServerSocket connectionSocket;
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = m_bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("ConnectDevice", Constants.uuid);
            }
            catch (java.io.IOException e)
            {
                Log.e("[Bluetooth]", "Socket connection failed", e);
            }


            connectionSocket = tmp;
        }

        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                BluetoothSocket conn = null;
                try
                {
                    conn = connectionSocket.accept();
                }
                catch (java.io.IOException e)
                {
                    // Log.e("[Bluetooth]", "Socket acception failed", e);
                }

                if (conn != null)
                {
                    connectSocket(conn);
                    MSG(MESSAGE_CONNECTED);
                }
            }
            l("Accept Thread Ended!");
        }

        public void cancel()
        {
            try
            {
                connectionSocket.close();
            }
            catch (java.io.IOException e)
            {
                // Log.e("[Bluetooth]", "Socket close failed", e);
            }
        }
    }

    private void connectSocket(BluetoothSocket connection)
    {

        if (connectedThreads.size() < allowedDevices)
        {
            l("Received a connection, adding a new thread: " + connectedThreads.size());
            ConnectedThread thread = new ConnectedThread(connection);
            thread.setId(connectedThreads.size() + 1);
            thread.start();
            connectedThreads.add(thread);
        }

    }

    //
    // An arraylist of threads for each connected device,
    // with a unique id for when they finish so they may be removed
    //
    ArrayList<ConnectedThread> connectedThreads = new ArrayList<>();
    private class ConnectedThread extends Thread
    {
        private BluetoothSocket connectedSocket;
        private byte[] buffer;
        private int id;

        private void setId(int i ) { id = i; }

        public ConnectedThread(BluetoothSocket sockets)
        {
            connectedSocket = sockets;
        }

        public void close()
        {
            try
            {
                connectedSocket.close();
            }
            catch (Exception e)
            {
                l("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                InputStream stream;
                InputStream tmpIn = null;
                try {
                    l("Loading input stream");
                    tmpIn = connectedSocket.getInputStream();
                } catch (IOException e) {
                    Log.e("[Bluetooth]", "Error occurred when creating input stream", e);
                }
                stream = tmpIn;

                l("Reading stream");
                if (!read(stream))
                {
                    break;
                }

                if (Thread.currentThread().isInterrupted())
                {
                    break;
                }
            }
            l("Connected Thread Ended!!!");
            disconnect(id);
            MSG(MESSAGE_DISCONNECTED);
        }

        private boolean read(InputStream stream)
        {
            buffer = new byte[1024];
            int numBytes;
            try
            {
                numBytes = stream.read(buffer);

                l("Reading Bytes of Length:" + numBytes);

                m_handler.obtainMessage(MESSAGE_INPUT, numBytes, id, new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "")).sendToTarget();
                return true;
            }
            catch (java.io.IOException e)
            {
                Log.d("[Bluetooth]", "Input stream disconnected", e);
                return false;
            }
        }


        public void write(byte[] bytes)
        {
            l("Writing: " + new String(bytes));
            l("Bytes Length: " + bytes.length);
            OutputStream stream;

            OutputStream tmpOut = null;
            try {
                tmpOut = connectedSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("[Bluetooth]", "Error occurred when creating output stream", e);
            }
            stream = tmpOut;

            try
            {
                l("Writing bytes to outstream");
                stream.write(bytes);
            }
            catch (Exception e)
            {
                Log.e("[Bluetooth]", "Failed to send data", e);
                disconnect(id);
            }
        }

        /*
        public void write(byte[] bytes, int device)
        {
            l("Writing " + new String(bytes));
            l("Bytes length: " + bytes.length);
            OutputStream out = null;
            try
            {
                out = connectedSockets.get(device).getOutputStream();
                out.write(bytes);
            }
            catch (IndexOutOfBoundsException e)
            {
                l("Failed to write, device not found at index: " + device);
            }
            catch (IOException e)
            {
                l("Failed to write. IOException." + e.getMessage());
                e.printStackTrace();
            }
        }
        */

        public void cancel()
        {
            try
            {
                connectedSocket.close();
            }
            catch (java.io.IOException e)
            {
                Log.e("[Bluetooth]", "Failed to close socket", e);
            }
        }
    }

    // Disconnect a specific connected device, usually called from the thread itself
    private synchronized void disconnect(int id)
    {
        if ((id - 1) < allowedDevices)
        {
            l("Removing thread with id: " + id);
            connectedThreads.get(id - 1).close();
            connectedThreads.get(id - 1).interrupt();
            connectedThreads.remove(id - 1);
        }
    }

    // When the activity is finished, clean up all of the bluetooth elements
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        l("Destroying application threads");
        if (bluetoothFailed)
            return;
        if (acceptThread.connectionSocket != null && ! acceptThread.isInterrupted())
        {
            try
            {
                acceptThread.connectionSocket.close();
            } catch (java.io.IOException e)
            {
                l("Connection socket closing failed: " + e.getMessage());
            }
        }
        for (ConnectedThread thread : connectedThreads)
        {
            thread.close();
            thread.interrupt();
        }

        /*
        // Save server matches
        StringBuilder matchesFileContent = new StringBuilder();
        for (String match : serverMatches)
            matchesFileContent.append(match).append("\n");
        FileHandler.Write(FileHandler.SERVER_DATABASE, matchesFileContent.toString());
        */

        saveJsonDatabase();
    }
}
