package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;

import java.util.*;


public class ScoutActivity extends BluetoothActivity {
    Button sendButton;
    Button connectButton;

    EditText teamNumber;
    NumberPicker matchNumber;

    GridView scoutLayout;


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
            }
        });

        teamNumber = (EditText) findViewById(R.id.teamNumber);

        matchNumber = (NumberPicker) findViewById(R.id.matchNumber);
        matchNumber.setMinValue(0);
        matchNumber.setMaxValue(150);

        scoutLayout = (GridView)findViewById(R.id.scoutLayout);



        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();

        values.put("First Boolean", ScoutInputAdapter.TYPE_BOOL);
        values.put("Second Boolean", ScoutInputAdapter.TYPE_BOOL);
        values.put("First String", ScoutInputAdapter.TYPE_STRING);
        values.put("Second String", ScoutInputAdapter.TYPE_STRING);
        values.put("First Int", ScoutInputAdapter.TYPE_INTEGER);
        values.put("Second Int", ScoutInputAdapter.TYPE_INTEGER);

        l("Adding ScoutInputAdapter View");
        ScoutInputAdapter sec = new ScoutInputAdapter(this);
        sec.Build(values);
        scoutLayout.setAdapter(sec);
        sec.notifyDataSetChanged();
    }
    private void handleInput(String s)
    {
        this.sendButton.setText(s);
    }


    private void sendButtonClick()
    {
        l("Sending value:\n" + "67,1");
        Write("67,1");
    }



    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                /*
                l("Getting full info");
                byte[] info = (byte[]) msg.obj;
                l("Translating with new statement");
                byte[] translatedInfo = new byte[msg.arg1];

                l("Filling new array at length: " + msg.arg1);
                for (int i = 0; i < msg.arg1; ++i)
                {
                    translatedInfo[i] = info[i];
                }
                l("Converting to string");
                String message = new String(translatedInfo);
                */
                handleInput((String)msg.obj);
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

}
