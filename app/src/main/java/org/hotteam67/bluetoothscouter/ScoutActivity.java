package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;
import java.sql.Struct;
import java.util.*;
import android.support.v7.app.AppCompatActivity;
import android.widget.*;
import android.content.*;


public class ScoutActivity extends AppCompatActivity {
    Button sendButton;
    Button connectButton;

    Button dynamicButton;
    LinearLayout scoutLayout;

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
        /*
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect();
            }
        });
        */


        scoutLayout = (LinearLayout) findViewById(R.id.scoutLayout);
        dynamicButton = new Button(this);
        dynamicButton.setText("Hello!");
        addView(dynamicButton);
    }
    private void handleInput(String s)
    {
        this.sendButton.setText(s);
    }


    private void sendButtonClick()
    {
        // Write("SENT!");
    }


    private void addView(View v)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = Gravity.CENTER;
        v.setLayoutParams(params);
        scoutLayout.addView(v);
    }

    /*

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                byte[] info = (byte[]) msg.obj;
                String message = new String(info);
                handleInput(message);
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
    */
}
