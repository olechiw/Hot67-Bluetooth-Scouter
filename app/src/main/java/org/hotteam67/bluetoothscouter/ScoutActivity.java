package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;

import java.util.*;
import java.io.*;


public class ScoutActivity extends BluetoothActivity {

    public static final String FILE_NAME = "schema.csv";
    public static final String FILE_DIRECTORY =
            Environment.getExternalStorageDirectory() + "/BluetoothScouter/";

    boolean isConnected = false;

    Button sendButton;
    Button connectButton;

    EditText teamNumber;
    NumberPicker matchNumber;

    EditText notes;

    /*
    GridView scoutLayout;
    ScoutInputAdapter scoutInputAdapter;
    */
    ScoutGridLayout scoutGridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getApplicationContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Sending")
                        .setMessage("Send?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendButtonClick();
                            }

                        });
            }
        });

        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect();
                l("Attempting Connect!");
            }
        });

        teamNumber = (EditText) findViewById(R.id.teamNumber);
        notes = (EditText) findViewById(R.id.notes);

        matchNumber = (NumberPicker) findViewById(R.id.matchNumber);
        matchNumber.setMinValue(0);
        matchNumber.setMaxValue(150);

        /*
        scoutLayout = (GridView)findViewById(R.id.scoutLayout);

        scoutInputAdapter = new ScoutInputAdapter(this);
        scoutLayout.setAdapter(scoutInputAdapter);
        */
        scoutGridLayout = (ScoutGridLayout)findViewById(R.id.scoutLayout);

        if (!Build())
            l("Build failed, no values loaded");
    }


    private void sendButtonClick()
    {
        if (isConnected)
            send();
        else
            write();
    }

    private String getDatabaseContent()
    {
        String content = "";
        try
        {
            File f = new File(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME);
            if (f.exists())
            {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String line = reader.readLine();
                while (line != null)
                {
                    content += line + "\n";
                    line = reader.readLine();
                }
            }
            else
            {
                f.mkdirs();
                f.createNewFile();
            }
        }
        catch (IOException e)
        {
            l("IOException: " + e.getMessage());
            e.printStackTrace();
        }

        return content;
    }

    private void clearDatabase()
    {
        try
        {
            File f = new File(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME);
            if (f.exists())
            {
                FileWriter writer = new FileWriter(f.getAbsolutePath());
                writer.write("");
                writer.close();
            }
        }
        catch (IOException e)
        {
            l("Failed to clear file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void write()
    {
        try
        {
            File f = new File(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME);
            FileWriter writer = new FileWriter(f);

            String content = getDatabaseContent() + "\n" + getValues();
            writer.write(content);
            writer.close();
        }
        catch (IOException e)
        {
            l("Failed to open database file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getValues()
    {
                /*
        l("Sending values:\n" + "67,1");
        */
        String values = "";
        String div = ",";

        if (teamNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += teamNumber.getText().toString() + div;

        values += Integer.toString(matchNumber.getValue()) + div;

        List<String> currentValues = scoutGridLayout.GetCurrentValues();
        for (int i = 0; i < currentValues.size(); ++i)
        {
            String s = currentValues.get(i);
            l("Appending to output: '" + s + "'");
            values += s;
            values += div;
        }

        String s = notes.getText().toString().replace("\n", " ");
        if (!s.trim().isEmpty())
            values += s;
        else
            values = values.substring(0, values.length() - 1);

        return values;
    }

    private void send()
    {
        Write(getDatabaseContent());
        clearDatabase();
        Write(getValues());
    }



    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                if (!Build((String)msg.obj, true))
                {
                    toast("Failed to Build Values on Receive!");
                    l("Input Failed: " + (String)msg.obj);
                }
                break;
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                toast("Connected!");
                isConnected = true;
                connectButton.setText("Connected!");
                break;
            case MESSAGE_DISCONNECTED:
                l("Device connection lost");
                toast("Disconnected!");
                isConnected = false;
                connectButton.setText("Connect");
                break;
        }
    }


    private boolean Build()
    {
        File targetFile = new File(FILE_DIRECTORY + FILE_NAME);

        // No data present on device
        if (!targetFile.exists())
            return false;

        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_DIRECTORY + FILE_NAME));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            if (line != null)
                return Build(line, false);
        }
        catch (IOException e)
        {
            l("Failed to find file even after checking. Something went wrong");
            e.printStackTrace();
        }
        return false;
    }

    private boolean Build(String s, boolean write)
    {
        l("Building UI From String: " + s);
        if (write)
        {
            try
            {
                File dir = new File(FILE_DIRECTORY);
                dir.mkdirs();
                File file = new File(FILE_DIRECTORY + FILE_NAME);
                if (!file.exists())
                    file.createNewFile();

                FileWriter f = new FileWriter(file.getAbsolutePath(), false);
                f.write(s);
                f.close();
            }
            catch (Exception e)
            {
                l("Failed to create and/or write to file: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        return scoutGridLayout.Build(s);
    }
}
