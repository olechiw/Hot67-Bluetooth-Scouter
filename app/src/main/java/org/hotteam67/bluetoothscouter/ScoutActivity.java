package org.hotteam67.bluetoothscouter;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.*;
import android.widget.*;
import android.view.*;
import android.os.Message;
import android.text.InputFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.text.Spanned;
import android.support.v7.widget.Toolbar;

import java.util.*;
import java.io.*;


public class ScoutActivity extends BluetoothActivity {
    boolean isConnected = false;

    FloatingActionButton sendButton;
    Button connectButton;

    EditText teamNumber;
    NumberPicker matchNumber;

    EditText notes;

    Toolbar toolbar;

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

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(R.layout.layout_toolbar);
        ab.setDisplayShowCustomEnabled(true);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        sendButton = (FloatingActionButton) findViewById(R.id.sendConfigurationButton);
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
        sendButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));

        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect();
                l("Attempting Connect!");
            }
        });
        if (connectedThreads.size() > 0)
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Toolbar tb = (Toolbar) findViewById(R.id.toolBar);
        //tb.inflateMenu(R.menu.toolbar_menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        inputTable.ReBuild();
    }


    private void sendButtonClick()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to send?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int id)
            {
                dlg.dismiss();
                DoSend();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int id)
            {
                dlg.dismiss();
            }
        });
        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private void DoSend()
    {
        if (isConnected)
            send();
        else
            write();
    }

    private String getDatabaseContent()
    {
        return FileHandler.LoadContents(FileHandler.SCOUTER);
    }

    private void clearDatabase()
    {
        try
        {
            FileWriter writer = FileHandler.GetWriter(FileHandler.SCOUTER);
            writer.write("");
            writer.close();
        }
        catch (Exception e)
        {
            l("Failed to clear file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void write()
    {
        try
        {
            String content = getDatabaseContent() + "\n" + getValues();
            FileHandler.Write(FileHandler.SCOUTER, content);
            teamNumber.setText("");
            notes.setText("");
        }
        catch (Exception e)
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

        String s = notes.getText().toString().replace("\n", " ").replace(",", " ");
        if (!s.trim().isEmpty())
            values += s;
        else
            if (values.length() > 0)
                values = values.substring(0, values.length() - 1);

        return values;
    }

    private void send()
    {
        if (teamNumber.getText().toString().trim().isEmpty())
        {
            toast("No team number!");
            return;
        }


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
                        l("Input Failed: " + msg.obj);
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
        try
        {
            BufferedReader reader = FileHandler.GetReader(FileHandler.SCHEMA);
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
            FileHandler.Write(FileHandler.SCOUTER, s);
        }

        return inputTable.Build(s);
    }
}
