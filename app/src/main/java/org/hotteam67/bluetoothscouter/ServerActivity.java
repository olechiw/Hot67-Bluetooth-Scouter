package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import java.io.*;
import java.security.cert.TrustAnchor;
import java.util.*;
import java.text.Normalizer;


public class ServerActivity extends BluetoothActivity {


    public static final String FILE_NAME = "database.csv";

    public static final int MATCH_NUMBER = 1;

    FileWriter databaseFile = null;
    String content = "";

    TextView connectedDevicesText;
    TextView teamsReceivedText;
    TextView latestMatchText;

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
            if (databaseFile != null)
                databaseFile.write(msg);

            content += msg;
        }
        catch (IOException e)
        {
            l("Failed to write to file on receive: " + e.getMessage());
        }

        List<String> vars = new ArrayList<>(Arrays.asList(msg.split(",")));

        int match = Integer.valueOf(vars.get(MATCH_NUMBER));
        if (match != matchNumber)
            teamsReceived = 0;
        matchNumber = match;

        teamsReceived++;
        teamsReceivedText.setText("Teams Received: " + teamsReceived);
        latestMatchText.setText("Latest Game #: " + matchNumber);



        /*
        ServerOutputAdapter.Build(
                this,
                new ArrayList<>(Arrays.asList(content.split("\n"))),
                outputView);
                */
    }

    private int connectedDevices = 0;

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                String message = (String)msg.obj;
                handleInput(message.substring(0,msg.arg1));
                break;
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                connectedDevices++;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                toast("CONNECTED!");
                break;
            case MESSAGE_DISCONNECTED:
                connectedDevices--;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                // toast("Device Disconnected!");
                break;
        }
    }

    private void setupIO()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(getFilesDir().getAbsolutePath() + FILE_NAME));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            while (line != null)
            {
                builder.append(line);
                builder.append("\n");
                line = reader.readLine();
            }

            reader.close();
            content = builder.toString();
        }
        catch (FileNotFoundException e)
        {
            l("Unable to detect database file, skipping load");
        }
        catch (IOException e)
        {
            l("Unable to write to file: " + e.getMessage());
        }

        try
        {
            File f = new File(getFilesDir().getAbsolutePath() + FILE_NAME);
            if (!f.exists())
                f.createNewFile();

            databaseFile = new FileWriter(f.getAbsoluteFile(), true);
        }
        catch (IOException e)
        {
            l("Failed to open filewriter: " + e.getMessage());
        }
    }
}
