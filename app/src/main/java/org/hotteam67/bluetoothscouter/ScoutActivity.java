package org.hotteam67.bluetoothscouter;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import android.text.InputFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.text.Spanned;

import java.util.*;
import java.io.*;


public class ScoutActivity extends BluetoothActivity {

    public static final String FILE_NAME = "schema.csv";
    public static final String FILE_DIRECTORY =
            Environment.getExternalStorageDirectory() + "/BluetoothScouter/";

    public static final String DATABASE_FILE_NAME = "localdatabase.csv";

    boolean isConnected = false;

    Button sendButton;
    Button connectButton;

    EditText teamNumber;
    NumberPicker matchNumber;

    EditText notes;

    /*
    GridView scoutLayout;
    org.hotteam67.bluetoothscouter.ScoutInputAdapter scoutInputAdapter;
    */
    //ScoutGridLayout scoutGridLayout;
    // SectionedView scoutGridLayout;
    InputTableLayout inputTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        sendButton = (Button) findViewById(R.id.sendConfigurationButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
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

                        }).show();
*/
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
        if (connectedSockets.size() > 0)
            connectButton.setText("Connected!");

        teamNumber = (EditText) findViewById(R.id.teamNumber);

        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                if (source != null && ",".contains(("" + source))) {
                    return "";
                }
                return null;
            }
        };


        notes = (EditText) findViewById(R.id.notes);
        notes.setFilters(new InputFilter[] { filter });

        matchNumber = (NumberPicker) findViewById(R.id.matchNumber);
        matchNumber.setMinValue(1);
        matchNumber.setMaxValue(200);

        /*
        scoutLayout = (GridView)findViewById(R.id.scoutLayout);

        scoutInputAdapter = new org.hotteam67.bluetoothscouter.ScoutInputAdapter(this);
        scoutLayout.setAdapter(scoutInputAdapter);
        */
        inputTable = (InputTableLayout) findViewById(R.id.scoutLayout);

        if (!Build())
            l("Build failed, no values loaded");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        inputTable.ReBuild();
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
            File f = new File(ServerActivity.FILE_DIRECTORY + DATABASE_FILE_NAME);
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
            File f = new File(ServerActivity.FILE_DIRECTORY + DATABASE_FILE_NAME);
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
            File f = new File(ServerActivity.FILE_DIRECTORY + DATABASE_FILE_NAME);
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

        List<String> currentValues = inputTable.GetCurrentValues();
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
            if (values.length() > 0)
                values = values.substring(0, values.length() - 1);

        return values;
    }

    private void send()
    {
        if (!getDatabaseContent().isEmpty())
        {
            Write(getDatabaseContent());
            clearDatabase();
        }
        Write(getValues());
        // matchNumber.setValue(matchNumber.getValue() + 1);
        teamNumber.setText("");
        notes.setText("");
    }


    private void HandleMatchTeam(String s)
    {
        List<String> values = new ArrayList<>(Arrays.asList(s.split(",")));
        if (values.size() != 2)
        {
            l("Invalid team/match number size: " + values.size());
            return;
        }
        else
        {
            teamNumber.setText(values.get(1));
            toast("Received a new Team!: " + values.get(1));
            try
            {
                matchNumber.setValue(Integer.valueOf(values.get(0)));
            }
            catch (Exception e)
            {
                l("Match number input failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                String s = (String)msg.obj;
                if (s.split(",").length > 2)
                {
                    if (!Build((String) msg.obj, true))
                    {
                        toast("Failed to Build Values on Receive!");
                        l("Input Failed: " + (String) msg.obj);
                    }
                }
                else
                {
                    HandleMatchTeam(s);
                }

                break;
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                // toast("Connected!");
                l("Device connection gained");
                isConnected = true;
                connectButton.setText("Connected!");
                break;
            case MESSAGE_DISCONNECTED:
                l("Device connection lost");
                // toast("Disconnected!");
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
                {
                    l("Writing to: " + file.getAbsolutePath());
                    file.createNewFile();
                }

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

        return inputTable.Build(s);
    }
}
