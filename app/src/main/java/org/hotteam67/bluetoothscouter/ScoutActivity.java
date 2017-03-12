package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;

import java.util.*;
import java.io.*;


public class ScoutActivity extends BluetoothActivity {
    Button sendButton;
    Button connectButton;

    EditText teamNumber;
    NumberPicker matchNumber;

    GridView scoutLayout;
    ScoutInputAdapter scoutInputAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);



        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButtonClick();
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

        matchNumber = (NumberPicker) findViewById(R.id.matchNumber);
        matchNumber.setMinValue(0);
        matchNumber.setMaxValue(150);

        scoutLayout = (GridView)findViewById(R.id.scoutLayout);

        scoutInputAdapter = new ScoutInputAdapter(this);
        scoutLayout.setAdapter(scoutInputAdapter);

        if (!Build())
            l("Build failed, no values loaded");
    }


    private void sendButtonClick()
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

        List<String> currentValues = ((ScoutInputAdapter)scoutLayout.getAdapter()).GetCurrentValues();
        for (int i = 0; i < currentValues.size(); ++i)
        {
            String s = currentValues.get(i);
            l("Appending to output: '" + s + "'");
            values += s;
            if (i + 1 != currentValues.size())
                values += div;
        }
        values += "\n";

        Write(values);
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
                toast("CONNECTED!");
                break;
            case MESSAGE_DISCONNECTED:
                toast("Device Disconnected!");
                break;
        }
    }


    private boolean Build()
    {
        File targetFile = new File(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME);

        // No data present on device
        if (!targetFile.exists())
            return false;

        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME));
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

    // Get just the last char
    private String getLast(String s)
    {
        return s.substring(s.length()-1);
    }
    // Get all values up to the last char
    private String getBefore(String s)
    {
        return s.substring(0, s.length()-1);
    }

    private boolean Build(String s, boolean write)
    {
        if (write)
        {
            try
            {
                File dir = new File(ServerActivity.FILE_DIRECTORY);
                dir.mkdirs();
                File file = new File(ServerActivity.FILE_DIRECTORY + ServerActivity.FILE_NAME);
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

        try
        {

            LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
            for (String str : s.split(","))
            {
                try
                {
                    int number = Integer.valueOf(getLast(str));
                    values.put(getBefore(str), number);
                } catch (Exception e)
                {
                    l("Failed to load type from input");
                    e.printStackTrace();
                }
            }

            scoutInputAdapter.Build(values);
            scoutInputAdapter.notifyDataSetChanged();

        }
        catch (Exception e)
        {
            e.printStackTrace();
            l("Failed to load");
            return false;
        }

        return true;
    }
}
