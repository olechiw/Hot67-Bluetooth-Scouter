package org.hotteam67.bluetoothscouter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.util.Log;

import com.cpjd.main.*;
import com.cpjd.models.*;
import com.cpjd.requests.*;
import com.cpjd.utils.*;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;

import java.util.*;
import java.text.*;

public class MatchesActivity extends AppCompatActivity
{

    EditText codeText;
    Button fetchButton;
    EditText outputText;

    FloatingActionButton saveButton;

    TBA tba;

    Toolbar toolbar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);


        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(R.layout.toolbar_matches);
        ab.setDisplayShowCustomEnabled(true);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        TBA.setID("HOT67", "BluetoothScouter", "V1");
        tba = new TBA();
        Settings.GET_EVENT_MATCHES = true;

        codeText = (EditText) findViewById(R.id.codeText);
        fetchButton = (Button) findViewById(R.id.fetchButton);
        outputText = (EditText) findViewById(R.id.outputText);
        saveButton = (FloatingActionButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FileHandler.Write(FileHandler.MATCHES, outputText.getText().toString());
                toast("Saved!");
            }
        });

        outputText.setText(FileHandler.LoadContents(FileHandler.MATCHES));

        fetchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                l("Fetching");
                try
                {
                    String s = "";
                    Event e = tba.getEvent(codeText.getText().toString(),
                            Integer.valueOf(new SimpleDateFormat("yyyy", Locale.US).format(new Date())));
                    l("Obtained event: " + e.name);
                    l("Year: " + new SimpleDateFormat("yyyy", Locale.US).format(new Date()));
                    l("Matches: " + e.matches.length);
                    for (Match m : e.matches)
                    {
                        if (m.comp_level.equals("qm"))
                        {
                            for (String t : m.blueTeams)
                            {
                                s += t.replace("frc", "") + ",";
                            }
                            for (int i = 0; i < m.redTeams.length; ++i)
                            {
                                s += m.redTeams[i].replace("frc", "");
                                if (i + 1 != m.redTeams.length)
                                    s += ",";
                            }
                            s += "\n";
                        }
                    }

                    outputText.setText(s);
                }
                catch (Exception e)
                {
                    toast("Failed to load matches");
                    Log.e("[Matches Fetcher]", "Failed to get event: " + e.getMessage(), e);
                }
            }
        });
    }

    public void l(String s)
    {
        Log.d("[Matches Fetcher]", s);
    }

    protected void toast(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            l("Failed to create dialog: " + e.getMessage());
        }
    }
}
