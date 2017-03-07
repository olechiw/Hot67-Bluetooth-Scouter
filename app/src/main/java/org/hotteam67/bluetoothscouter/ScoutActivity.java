package org.hotteam67.bluetoothscouter;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.os.Message;


public class ScoutActivity extends BluetoothActivity {
    Button sendButton;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);


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
                break;
            case MESSAGE_DISCONNECTED:
                toast("Device Disconnected!");
                break;
        }
    }
}
