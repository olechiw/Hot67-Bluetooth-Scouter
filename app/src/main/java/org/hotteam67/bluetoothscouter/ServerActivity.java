package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import java.io.*;
import java.security.cert.TrustAnchor;
import java.util.*;
import java.text.Normalizer;
import android.support.v4.app.ActivityCompat;
import android.os.Environment;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;


public class ServerActivity extends BluetoothActivity {


    public static final String FILE_NAME = "database.csv";
    public static final String FILE_DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";

    public static final int REQUEST_PERM_EXTERNAL = 1;

    public static final int MATCH_NUMBER = 1;

    FileWriter databaseFile = null;
    String content = "";
    String header = "";

    TextView connectedDevicesText;
    TextView teamsReceivedText;
    TextView latestMatchText;

    Button sendButton;

    /*
    TableLayout outputView;
    ServerOutputAdapter outputAdapter;
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        setupIO();

        connectedDevicesText = (TextView)findViewById(R.id.connectedDevices);
        teamsReceivedText = (TextView)findViewById(R.id.teamsReceived);
        latestMatchText = (TextView)findViewById(R.id.latestMatch);

        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!header.trim().isEmpty())
                    Write(header);
            }
        });
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
            l("Writing to database file: " + msg);
            if (databaseFile != null)
                databaseFile.append(msg + "\n");
            databaseFile.flush();
            content += msg;
        }
        catch (IOException e)
        {
            l("Failed to write to file on receive: " + e.getMessage());
            e.printStackTrace();
        }

        List<String> vars = new ArrayList<>(Arrays.asList(msg.split(",")));

        int match = Integer.valueOf(vars.get(MATCH_NUMBER));
        if (match != matchNumber)
            teamsReceived = 0;
        matchNumber = match;

        teamsReceived++;
        teamsReceivedText.setText("Teams Received: " + teamsReceived);
        latestMatchText.setText("Latest Game #: " + matchNumber);
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
                // toast("Device Disconnected!");
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupIO();
                }
            }
        }
    }

    private void setupIO()
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            l("Needs External Storage Permissions. Requesting");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERM_EXTERNAL);
            return;
        }


        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            l("No Access to SD Card!!");
        }

        loadExistingContent();

        loadWriter();
    }

    private void loadExistingContent()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_DIRECTORY + FILE_NAME));
            StringBuilder builder = new StringBuilder();

            String line = reader.readLine();
            if (line != null)
            {
                l("Found Header: " + line);
                builder.append(line);
                builder.append("\n");
                header = line;
                line = reader.readLine();
            }
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

            databaseFile = new FileWriter(f.getAbsolutePath(), true);
        }
        catch (IOException e)
        {
            l("Failed to open filewriter: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
