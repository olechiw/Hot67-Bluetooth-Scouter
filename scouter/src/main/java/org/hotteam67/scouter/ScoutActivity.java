package org.hotteam67.scouter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import android.view.*;
import android.os.Message;
import android.text.InputFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.text.Spanned;
import android.support.v7.widget.Toolbar;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.io.*;


public class ScoutActivity extends BluetoothActivity
{
    private static final int REQUEST_ENABLE_PERMISSION = 3;

    ImageButton connectButton;
    ImageButton syncAllButton;

    FloatingActionButton nextMatchButton;
    FloatingActionButton prevMatchButton;

    EditText teamNumber;
    EditText matchNumber;

    EditText notes;

    Toolbar toolbar;

    GestureDetectorCompat gestureDetectorCompat;

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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                doConfirmEnd();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);

        setupPermissions();

    }

    private void setupUI()
    {
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        //ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);

        // setRequestedOrientation(getResources().getConfiguration().orientation);

        connectButton = (ImageButton) toolbar.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                l("Triggered Connect!");
                connectButton.setImageResource(R.drawable.ic_network_check);
                Connect();
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
                loadNext();
            }
        });
        prevMatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadPrevious();
            }
        });

        final Context c = this;
        syncAllButton = (ImageButton) findViewById(R.id.syncAllButton);
        syncAllButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                for (String match : matches)
                {
                    SendJsonValues(match);
                }
                new AlertDialog.Builder(c)
                        .setMessage("Sent all scouted data to server!")
                        .create().show();
            }
        });


        Build(); // Build the input table's rows and columns.

        loadDatabase();

        matchNumber.clearFocus();
        matchNumber.setText("1");
        if (!matches.isEmpty())
        {
            l("Loading first match!");
            teamNumber.setText(teams.get(0));
            loadMatch(1);
        }

        matchNumber.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                if (getCurrentFocus() == matchNumber)
                    save();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                loadMatch(currentMatch(), false);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        gestureDetectorCompat = new GestureDetectorCompat(this, new GestureListener());

        ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        // scrollView.requestDisallowInterceptTouchEvent(true);
        scrollView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return gestureDetectorCompat.onTouchEvent(event);
            }
        });
    }

    private void loadNext()
    {
        save();
        l("Loading Next Match");
        loadMatch(currentMatch() + 1);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    private void loadPrevious()
    {
        save();
        if (currentMatch() > 1)
        {
            l("Loading Previous Match");
            loadMatch(currentMatch() - 1);
        }
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
    }

    int currentMatch()
    {
        try
        {
            int i = Integer.valueOf(matchNumber.getText().toString());
            if (i <= 0)
                return 1;
            return i;
        }
        catch (Exception e)
        {
            return 1;
        }
    }

    private void loadMatch(int match)
    {
        loadMatch(match, true);
    }
    private void loadMatch(int match, boolean changeMatchText)
    {
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);
        if (matches.size() >= match)
        {
            // l("Loading match number: " + match);
            // l("Loading match: " + matches.get(match - 1));
            String[] vals = matches.get(match - 1).split(",");
            // List<String> subList = Arrays.asList(vals).subList(2, vals.length - 1);
            try {
                SchemaHandler.SetCurrentValues(inputTable, Arrays.asList(vals).subList(2, vals.length - 1));
            }
            catch (Exception e)
            {
                l("Failed to load match, corrupted, out of sync, or doesn't exist " + e.getMessage());
                // e.printStackTrace();
                l("Offending match: -->  " + matches.get(match - 1) + " <--");
            }

            if (teams.size() >= match)
                teamNumber.setText(teams.get(match - 1));
            else
                teamNumber.setText("0");
        }
        else if (matches.size() + 1 == match)
        {
            if (teams.size() >= match)
                teamNumber.setText(teams.get(match - 1));
            else
                teamNumber.setText("0");

            SchemaHandler.ClearCurrentValues(inputTable);
        }
        else
        {
            loadMatch(matches.size());
            return;
        }

        if (changeMatchText)
        {
            matchNumber.clearFocus();
            matchNumber.setText(String.valueOf(match));
        }
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
        // Existing match
        if (matches.size() >= currentMatch())
        {
            // l("Setting value: " + getValues());
            matches.set(currentMatch() - 1, getValues());
            teams.set(currentMatch() - 1, currentTeam());
        }
        // New match
        else if (matches.size() + 1 == currentMatch())
        {
            matches.add(getValues());
            teams.add(currentTeam());
        }

        // Store all matches locally
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
        // l("Writing output to matches file: " + output);
        FileHandler.Write(FileHandler.MATCHES, output);

        SendJsonValues(matches.get(currentMatch() - 1));
    }

    private void SendJsonValues(String match)
    {
        try
        {
            JSONObject outputObject = new JSONObject();

            List<String> values = new ArrayList<>(Arrays.asList(
                    match.split(",")));
            List<String> headers = new ArrayList<>(Arrays.asList(
                    SchemaHandler.GetHeader(
                            SchemaHandler.LoadSchemaFromFile()).split(",")));
            headers.removeAll(Arrays.asList("", null));
            headers.add(0, Constants.TEAM_NUMBER_JSON_TAG);
            headers.add(1, Constants.MATCH_NUMBER_JSON_TAG);
            if (headers.size() != values.size())
            {
                l("Failed to load schema into json, values out of sync!");
                l(String.valueOf(headers.size()));
                l(String.valueOf(values.size()));
            } else
            {
                try
                {
                    for (int i = 0; i < headers.size(); ++i)
                    {
                        outputObject.put(headers.get(i), values.get(i));
                    }

                    Write(outputObject.toString());

                    l("Output JSON: " + outputObject.toString());
                } catch (JSONException e)
                {
                    l("Exception raised in json addition:" + e.getMessage());
                    return;
                }
            }
        }
        catch (Exception e)
        {
            l("Failure to load and parse csv match ot json: " + match);
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
            values += s;
            values += div;
        }


        String s = notes.getText().toString().replace("\n", " ").replace(",", " ");
        if (!s.trim().isEmpty())
            values += s;
        else
            if (values.length() > 0)
                values = values.substring(0, values.length() - 1);

        // l("Current Values: " + values);

        return values;
    }

    private void setupPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            l("Permission granted");
            setupUI();
        }
        else
        {
            l("Permission requested!");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ENABLE_PERMISSION);
        }
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            // l("onDown: " + event.toString());
            return false; // Allow scrollview work
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            float sens = 500;
            if (
                    Math.abs(velocityX) > Math.abs(velocityY)
                    && Math.abs(velocityX) > sens)
            {
                if (velocityX > 0)
                {
                    loadPrevious();
                }
                else
                {
                    loadNext();
                }
                return true;
            }
            return false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_ENABLE_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                l("Permission granted");
                setupUI();
            }
            else
            {
                setupPermissions();
            }
        }
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
            r.close();
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
            case MESSAGE_INPUT: // Input received through bluetooth
                final String message =
                        Constants.getScouterInputWithoutTag(new String((byte[]) msg.obj));
                final String tag =
                        Constants.getScouterInputTag(new String((byte[]) msg.obj));

                if (tag == Constants.SCOUTER_SCHEMA_TAG)
                {
                    final Context c = this;
                    // Show a confirmation dialog
                    new AlertDialog.Builder(this)
                            .setMessage("Received new schema, clear local schema?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                            {
                                // If the user selects yes
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    FileHandler.Write(FileHandler.SCHEMA, message);
                                    SchemaHandler.Setup(
                                            inputTable, // Table to setup the new schema on
                                            SchemaHandler.LoadSchemaFromFile(), // Schema text
                                            c); // Context
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {

                        }
                    }).create().show();
                }
                else if (tag == Constants.SCOUTER_TEAMS_TAG)
                {
                    // Show a confirmation dialog
                    new AlertDialog.Builder(this)
                            .setMessage("Received new teams, clear local database?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                            {
                                // Confirmed. Clear out the database and load in new teams and matches
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    teams = new ArrayList<>(Arrays.asList(message.split(",")));
                                    matches = new ArrayList<>();
                                    clearMatches();
                                    SchemaHandler.ClearCurrentValues(inputTable);
                                    loadMatch(1);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {

                        }
                    }).create().show();
                }
            case MESSAGE_TOAST: // Deprecated
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED: // A device has connected
                connectButton.setImageResource(R.drawable.ic_network_wifi);
                break;
            case MESSAGE_DISCONNECTED: // A device has disconnected
                connectButton.setImageResource(R.drawable.ic_network_off);
                break;
        }
    }

    private void Build()
    {
        SchemaHandler.Setup(
                inputTable, // Table
                SchemaHandler.LoadSchemaFromFile(), // Text schema
                this); // Context
    }
}
