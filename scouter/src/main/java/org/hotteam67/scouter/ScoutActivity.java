package org.hotteam67.scouter;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.*;
import android.view.*;
import android.os.Message;
import android.text.InputFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.text.Spanned;
import android.support.v7.widget.Toolbar;

import org.hotteam67.common.BluetoothActivity;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;

import java.util.*;
import java.io.*;

import javax.xml.validation.Schema;


public class ScoutActivity extends BluetoothActivity
{
    boolean isConnected = false;

    FloatingActionButton saveButton;
    Button connectButton;

    EditText teamNumber;
    EditText matchNumber;

    EditText notes;

    Toolbar toolbar;

    /*
    GridView scoutLayout;
    org.hotteam67.bluetoothscouter.ScoutInputAdapter scoutInputAdapter;
    */
    //ScoutGridLayout scoutGridLayout;
    // SectionedView scoutGridLayout;
    TableLayout inputTable;


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            android.util.Log.d(this.getClass().getName(), "back button pressed");
            doConfirmEnd();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void doConfirmEnd()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to quit?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int id)
            {
                dlg.dismiss();
                finish();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            doConfirmEnd();
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(R.layout.toolbar_scout);
        ab.setDisplayShowCustomEnabled(true);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        saveButton = (FloatingActionButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
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
                                saveButtonClick();
                            }
ex
                        }).show();
*/
                saveButtonClick();
            }
        });

        connectButton = (Button) ab.getCustomView().findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("Connecting!");
                Connect();
                l("Attempting Connect!");
            }
        });
        if (connectedThreads.size() > 0)
            connectButton.setText("Connected!");

        teamNumber = (EditText) ab.getCustomView().findViewById(R.id.teamNumberText);

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

        matchNumber = (EditText) ab.getCustomView().findViewById(R.id.matchNumberText);

        /*
        scoutLayout = (GridView)findViewById(R.id.scoutLayout);

        scoutInputAdapter = new org.hotteam67.bluetoothscouter.ScoutInputAdapter(this);
        scoutLayout.setAdapter(scoutInputAdapter);
        */
        inputTable = (TableLayout) findViewById(R.id.scoutLayout);

        if (!Build())
            l("Build failed, no values loaded");
    }


    private void saveButtonClick()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to save?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int id)
            {
                dlg.dismiss();
                write();
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

    private String getDatabaseContent()
    {
        return FileHandler.LoadContents(FileHandler.SCOUTER);
    }

    private void clearDatabase()
    {
        try
        {
            FileHandler.Write(FileHandler.SCOUTER, "");
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

        if (matchNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += matchNumber.getText() + div;

        List<String> currentValues = SchemaHandler.GetCurrentValues(inputTable);
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
                matchNumber.setText(values.get(0));
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
                    Build((String)msg.obj, true);
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
                toast("Lost Server Connection!");
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
                Build(line, false);
            return true;
        }
        catch (IOException e)
        {
            l("Failed to find file even after checking. Something went wrong");
            e.printStackTrace();
        }
        return false;
    }

    private void Build(String s, boolean write)
    {
        l("Building UI From String: " + s);
        if (write)
        {
            FileHandler.Write(FileHandler.SCHEMA, s);
        }

        SchemaHandler.Setup(inputTable, s, this);

    }
}
