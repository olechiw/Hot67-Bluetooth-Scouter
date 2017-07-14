package org.hotteam67.scouter;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatImageButton;
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

    ImageButton saveButton;
    Button connectButton;

    FloatingActionButton nextMatchButton;
    FloatingActionButton prevMatchButton;

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

    List<String> matches = new ArrayList<>();
    List<String> teams = new ArrayList<>();


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

        // setRequestedOrientation(getResources().getConfiguration().orientation);

            saveButton = (ImageButton) toolbar.findViewById(R.id.saveButton);
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
                save();
            }
        });

        teamNumber = (EditText) toolbar.findViewById(R.id.teamNumberText);

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

        matchNumber = (EditText) findViewById(R.id.matchNumberText);

        /*
        scoutLayout = (GridView)findViewById(R.id.scoutLayout);

        scoutInputAdapter = new org.hotteam67.bluetoothscouter.ScoutInputAdapter(this);
        scoutLayout.setAdapter(scoutInputAdapter);
        */
        inputTable = (TableLayout) findViewById(R.id.scoutLayout);

        nextMatchButton = (FloatingActionButton) findViewById(R.id.nextMatchButton);
        prevMatchButton = (FloatingActionButton) findViewById(R.id.prevMatchButton);

        nextMatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                save();
                loadMatch(currentMatch() + 1);
                ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
            }
        });
        prevMatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                save();
                if (currentMatch() > 1)
                    loadMatch(currentMatch() - 1);
                ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
            }
        });

        if (!Build())
            l("Build failed, no values loaded");

        loadDatabase();

        matchNumber.setText("1");
        if (!matches.isEmpty())
        {
            teamNumber.setText(teams.get(0));
            loadMatch(1);
        }
    }

    int currentMatch() { return Integer.valueOf(matchNumber.getText().toString()); }

    private void loadMatch(int match)
    {
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
        if (matches.size() >= match)
        {
            l("Loading match: " + matches.get(match - 1));
            String[] vals = matches.get(match - 1).split(",");
            // List<String> subList = Arrays.asList(vals).subList(2, vals.length - 1);
            SchemaHandler.SetCurrentValues(inputTable, Arrays.asList(vals).subList(2, vals.length-1));

            teamNumber.setText(teams.get(match - 1));
        }
        else if (matches.size() + 1 == match)
        {
            teamNumber.setText("0");
            SchemaHandler.ClearCurrentValues(inputTable);
        }
        else
        {
            loadMatch(matches.size());
            return;
        }

        matchNumber.setText(String.valueOf(match));
    }

    private String currentTeam()
    {
        String s = teamNumber.getText().toString();
        if (!s.trim().isEmpty())
            return s;
        else
            return "0";
    }

    private void clearMatches()
    {
        try
        {
            FileHandler.Write(FileHandler.MATCHES, "");
        }
        catch (Exception e)
        {
            l("Failed to clear file for re-write: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void save()
    {
        if (matches.size() >= currentMatch())
        {
            matches.set(currentMatch() - 1, getValues());
            teams.set(currentMatch() - 1, currentTeam());
        }
        else if (matches.size() + 1 == currentMatch())
        {
            matches.add(getValues());
            teams.add(currentTeam());
        }

        clearMatches();
        String output = "";
        int i = 1;
        for (String s : matches)
        {
            output += s;
            if (i < matches.size())
                output += "\n";
            i++;
        }
        FileHandler.Write(FileHandler.MATCHES, output);
    }

    private String getValues()
    {
                /*
        l("Sending values:\n" + "67,1");
        */
        String values = "";
        String div = ",";

        /*
        if (teamNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += teamNumber.getText().toString() + div;
            */
        values += currentTeam() + div;
/*
        if (matchNumber.getText().toString().trim().isEmpty())
            values += "0" + div;
        else
            values += matchNumber.getText() + div;
            */
        values += currentMatch() + div;

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

    private void loadDatabase()
    {
        try
        {
            BufferedReader r = FileHandler.GetReader(FileHandler.MATCHES);
            String line = r.readLine();
            while (line != null)
            {
                matches.add(line);
                teams.add(line.split(",")[0]);
                line = r.readLine();
            }
        }
        catch (Exception e)
        {
            l("Failed to load contents of matches database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                /*
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
                */
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                // toast("Connected!");
                /*
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
                */
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
