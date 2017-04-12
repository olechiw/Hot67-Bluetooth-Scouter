package org.hotteam67.bluetoothscouter;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import java.io.*;
import java.util.*;

import android.support.v4.app.ActivityCompat;
import android.os.Environment;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;


public class ServerActivity extends BluetoothActivity {


    public static final String FILE_NAME = "database.csv";
    public static final String FILE_DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";

    public static final String TEAM_NUMBER_SCHEMA =
            "Team 12,Team 22,Team 32,Team 42,Team 52,Team 62";

    public static final int REQUEST_PERM_EXTERNAL = 1;

    public static final int MATCH_NUMBER = 1;

    FileWriter databaseFile = null;
    String content = "";

    TextView connectedDevicesText;
    TextView teamsReceivedText;
    TextView latestMatchText;

    Button sendConfigurationButton;
    Button sendTeamsButton;

    NumberPicker match;

    InputTableLayout teamsLayout;

    /*
    TableLayout outputView;
    ServerOutputAdapter outputAdapter;
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        setupIO();

        connectedDevicesText = (TextView)findViewById(R.id.connectedDevices);
        teamsReceivedText = (TextView)findViewById(R.id.teamsReceived);
        latestMatchText = (TextView)findViewById(R.id.latestMatch);

        sendConfigurationButton = (Button)findViewById(R.id.sendConfigurationButton);
        sendConfigurationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                l("Sending configuration");
                String s = loadSchema();
                if (!s.trim().isEmpty())
                    Write(s);
                else
                    l("No configuration found");
            }
        });
        sendTeamsButton = (Button) findViewById(R.id.sendTeamsButton);
        sendTeamsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SendTeams();
            }
        });

        match = (NumberPicker) findViewById(R.id.matchNumber);
        match.setMinValue(1);
        match.setMaxValue(200);

        teamsLayout = (InputTableLayout) findViewById(R.id.teamNumberLayout);
        teamsLayout.Build(TEAM_NUMBER_SCHEMA);


        /*
        outputView = (TableLayout)findViewById(R.id.outputView);


        for (int i = 0; i < 15; ++i)
        {
            content += "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z\n";
        }
        l("Content:\n" + content);
        List<String> data = new ArrayList<>(Arrays.asList(content.split("\n")));
        ServerOutputAdapter.Build(this, data, outputView);
        */
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        teamsLayout.ReBuild();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try
        {
            if (databaseFile != null)
                databaseFile.close();
        }
        catch (IOException e)
        {
            l("Failed to close database file");
        }
    }

    int teamsReceived = 0;
    int matchNumber = 0;

    private void handleInput(String msg)
    {
        l("Handling input: Value: " + msg);

        try
        {
            if (databaseFile != null)
            {
                l("Writing to database file: " + msg);
                if (databaseFile != null)
                    databaseFile.append(msg + "\n");
                databaseFile.flush();
                content += msg;
            }
        }
        catch (IOException e)
        {
            l("Failed to write to file on receive: " + e.getMessage());
            e.printStackTrace();
        }

        List<String> vars = new ArrayList<>(Arrays.asList(msg.split(",")));

        try
        {
            int match = Integer.valueOf(vars.get(MATCH_NUMBER));
            if (match != matchNumber)
                teamsReceived = 0;
            matchNumber = match;
        }
        catch (Exception e)
        {
            l("Invalid match #: " + vars.get(MATCH_NUMBER));
            e.printStackTrace();
        }

        teamsReceived++;
        teamsReceivedText.setText("Teams Received: " + teamsReceived);
        latestMatchText.setText("Latest Game #: " + matchNumber);
    }

    private void SendTeams()
    {
        List<String> teams = teamsLayout.GetCurrentValues();
        String output = match.getValue() + ",";
        match.setValue(match.getValue() + 1);
        for (int i = 0; i < teams.size(); ++i)
        {
            if (i < connectedSockets.size())
                Write(output + teams.get(i), i);
        }
    }

    private int connectedDevices = 0;

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                String message = (String)msg.obj;
                handleInput(message);
                break;
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                connectedDevices++;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                // toast("CONNECTED!");
                break;
            case MESSAGE_DISCONNECTED:
                connectedDevices--;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                l(
                        "Disconnect. Connected Devices: "
                                + connectedDevices
                                + " Socket Count: "
                                + connectedSockets.size());
                // toast("Device Disconnected!");
                break;
        }
    }

    private void setupIO()
    {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            l("No Access to SD Card!!");
        }

        loadExistingContent();

        loadWriter();
    }

    private String loadSchema()
    {
        String line = "";
        try
        {
            BufferedReader reader = new BufferedReader(
                    new FileReader(ScoutActivity.FILE_DIRECTORY + ScoutActivity.FILE_NAME));
            line = reader.readLine();
            reader.close();
            l("Read line from configuration file: " + line);
        }
        catch (FileNotFoundException e)
        {
            l("Unable to detect schema file, skipping send-config");
        }
        catch (IOException e)
        {
            l("Failed to read schema file : " + e.getMessage());
            e.printStackTrace();
        }
        return line;
    }

    private void loadExistingContent()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_DIRECTORY + FILE_NAME));
            StringBuilder builder = new StringBuilder();

            String line = reader.readLine();
            while (line != null)
            {
                builder.append(line);
                builder.append("\n");
                line = reader.readLine();
            }
            reader.close();
            l("File Found: " + FILE_DIRECTORY + FILE_NAME);
            content = builder.toString();
            l("Read content: " + content);
        }
        catch (FileNotFoundException e)
        {
            l("Unable to detect database file, skipping load");


            File dir = new File(FILE_DIRECTORY);

            l("Creating files directory: " + dir.getAbsolutePath());
            dir.mkdirs();
        }
        catch (IOException e)
        {
            l("Unable to read from file: " + e.getMessage());
        }
    }

    private String getSchemaHeader(String schema)
    {
        /*
        ScoutGridLayout layout = new ScoutGridLayout(this);
        layout.Build(schema);
        List<ScoutGridLayout.Variable> vars = layout.GetVariables();
        String s = "Team Number,Match Number,";
        int i = 0;

        for (ScoutGridLayout.Variable v : vars)
        {
            if (v.Type != ScoutGridLayout.TYPE_HEADER)
            {
                s += v.Tag;
                if (i < vars.size() - 1)
                    s += ',';
            }
            ++i;
        }
        */
        InputTableLayout layout = new InputTableLayout(this);
        layout.Build(schema);
        String s = "Team Number,Match Number,";
        int i = 0;

        for (InputTableLayout.Variable v : layout.getVariables())
        {
            if (v.Type != InputTableLayout.TYPE_HEADER)
            {
                s += v.Tag;
                s += ',';
            }
            ++i;
        }
        s = s.substring(0, s.length() - 1);
        return s;
    }

    private void loadWriter()
    {
        try
        {
            l("Loading file for writing");
            File f = new File(FILE_DIRECTORY + FILE_NAME);
            if (!f.exists()) {
                l("File does not exist. Creating: " + f.getAbsolutePath());
                f.createNewFile();
            }

            databaseFile = new FileWriter(f.getAbsolutePath(), false);
        }
        catch (IOException e)
        {
            l("Failed to open filewriter: " + e.getMessage());
            e.printStackTrace();
        }

        // Update header with schema
        String s = loadSchema();
        if (!s.trim().isEmpty())
        {
            try
            {
                l("Loading databasefile for header write");
                l("Writing new header line");
                List<String> oldString = new ArrayList<>(Arrays.asList(content.split("\n")));
                if (oldString.size() > 0)
                    oldString.remove(0);
                oldString.add(0, getSchemaHeader(s));
                l("Old String size: " + oldString.size());
                l("Building string again");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < oldString.size(); ++i)
                    builder.append(oldString.get(i) + "\n");
                l("Writing: " + builder.toString());
                databaseFile.write(builder.toString());
                databaseFile.flush();
            }
            catch (IOException e)
            {
                l("Failed to open file for header writing. Something went wrong??");
                e.printStackTrace();
            }
        }
    }
}
