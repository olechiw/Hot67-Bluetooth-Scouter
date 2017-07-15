package org.hotteam67.scouter;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends org.hotteam67.common.BluetoothActivity
{
    private Button connectButton;
    boolean connected = false;

    private List<String> matches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        SetupBluetooth();
    }

    private void SetupBluetooth()
    {
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onConnectButtonClick();
            }
        });
    }

    private void onConnectButtonClick()
    {
        if (this.connected)
            return;
        backupDatabase();
        Connect();
    }

    private void backupDatabase()
    {
        FileHandler.Write(FileHandler.SCOUTER, FileHandler.LoadContents(FileHandler.MATCHES));
    }

    private void clearDatabase()
    {
        FileHandler.Write(FileHandler.MATCHES, "");
    }


    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_CONNECTED:
                toast("Connected!");
                connectButton.setText("Connected!");
                connected = true;
                break;
            case MESSAGE_DISCONNECTED:
                toast("Disconnected!");
                connectButton.setText("Connect");
                connected = false;
                break;
            case MESSAGE_INPUT:
                toast("Received input");
                String input = (String)msg.obj;
                String tag = Constants.getTag(input);
                if (tag == Constants.MATCH_TAG)
                {
                    loadReceivedMatch(Constants.tagless(input));
                }
                else if (tag == Constants.SCHEMA_TAG)
                {
                    FileHandler.Write(FileHandler.SCHEMA, Constants.tagless(input));
                }
        }
    }

    private void loadReceivedMatch(String match)
    {
        matches.add(match);
        String output = "";
        int i = 0;
        for (String m : matches)
        {
            output += m;
            if (i < matches.size())
                output += "\n";
        }
        FileHandler.Write(FileHandler.MATCHES, output);
    }
}
