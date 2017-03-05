package org.hotteam67.bluetoothscouter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.*;
import android.widget.*;
import android.view.*;
import java.util.*;
import java.io.*;
import android.util.*;
import android.app.*;
import android.content.*;
import android.os.Handler;
import android.os.Message;
public class MainActivity extends BluetoothActivity {
    Button sendButton;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);


        // AddUUID("1cb5d5ce-00f5-11e7-93ae-92361f002671");

        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Write("SENT!");
            }
        });
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect();
            }
        });
    }


    private void handleInput(String s)
    {
        this.sendButton.setText(s);
    }

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
            case MESSAGE_DISCONNECTED:
                toast("Device Disconnected!");
                break;
        }
    }
}
