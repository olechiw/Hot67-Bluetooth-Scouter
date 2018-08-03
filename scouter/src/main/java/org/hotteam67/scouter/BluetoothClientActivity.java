package org.hotteam67.scouter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.hotteam67.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothClientActivity extends AppCompatActivity {


    // Messages, for when any event happens, to be sent to the main thread
    public final int MESSAGE_INPUT = 0;
    public final int MESSAGE_TOAST = 1;
    public final int MESSAGE_DISCONNECTED = 2;
    public final int MESSAGE_CONNECTED = 3;

    // Send a specific message, from the above list
    public synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    // The multi-thread handler for passing messages about bluetooth connection
    protected Handler m_handler;

    // Simple log function
    protected void l(String s)
    {
        Log.d(TAG, s);
    }

    // The log tag
    public static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";

    // Whether the bluetooth hardware setup has completely failed (typically means something like it failed to be enabled)
    private boolean bluetoothFailed = false;

    // Adapter to the hardware bluetooth device
    protected BluetoothAdapter m_bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                handle(msg);
            }
        };

        l("Setting up bluetooth");
        setupBluetooth();
    }

    // MessageBox
    protected void MessageBox(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            l("Failed to create dialog: " + e.getMessage());
        }
    }

    /*
    Connect Thread
     */
    ConnectThread connectThread;
    private class ConnectThread extends Thread {
        private final Set<BluetoothDevice> connectedDevices;
        private List<BluetoothSocket> connectedSockets;
        ConnectThread(Set<BluetoothDevice> devices)
        {
            connectedDevices = devices;

            connectedSockets = new ArrayList<>();
            BluetoothSocket connectionSocket;
            for (BluetoothDevice device : connectedDevices) {
                connectionSocket = null;
                try {
                    l("Getting Connection");
                    connectionSocket = device.createRfcommSocketToServiceRecord(Constants.uuid);
                } catch (java.io.IOException e) {
                    Log.e("[Bluetooth]", "Failed to connect to socket", e);
                }
                connectedSockets.add(connectionSocket);
            }
        }

        public void run()
        {
            for (BluetoothSocket connectionSocket : connectedSockets)
            {
                try
                {
                    l("Connecting to socket");
                    connectionSocket.connect();
                    connectSocket(connectionSocket);
                    break;
                } catch (java.io.IOException e)
                {
                    try
                    {
                        connectionSocket.close();
                    } catch (java.io.IOException e2)
                    {
                        Log.e("[Bluetooth]", "Failed to close socket after failure to connect", e2);
                    }
                }
            }
        }

        public void cancel()
        {
            for (BluetoothSocket connectionSocket : connectedSockets) {
                try {
                    connectionSocket.close();
                } catch (java.io.IOException e) {
                    Log.e("[Bluetooth]", "Failed to close socket", e);
                }
            }
        }
    }

    /*
    Connected Thread
     */
    ConnectedThread connectedThread;
    private class ConnectedThread extends Thread
    {
        private BluetoothSocket connectedSocket;
        private byte[] buffer;
        private int id;

        private void setId(int i ) { id = i; }

        public ConnectedThread(BluetoothSocket sockets)
        {
            connectedSocket = sockets;
        }

        public void close()
        {
            try
            {
                connectedSocket.close();
            }
            catch (Exception e)
            {
                l("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                InputStream stream;
                InputStream tmpIn = null;
                try {
                    l("Loading input stream");
                    tmpIn = connectedSocket.getInputStream();
                } catch (IOException e) {
                    Log.e("[Bluetooth]", "Error occurred when creating input stream", e);
                }
                stream = tmpIn;

                l("Reading stream");
                if (!read(stream))
                {
                    break;
                }
            }
            l("Connected Thread Ended!!!");
            disconnect();
        }

        private boolean read(InputStream stream)
        {
            buffer = new byte[1024];
            int numBytes;
            try
            {
                numBytes = stream.read(buffer);

                l("Reading Bytes of Length:" + numBytes);

                m_handler.obtainMessage(MESSAGE_INPUT, numBytes, -1, new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "")).sendToTarget();
                return true;
            }
            catch (java.io.IOException e)
            {
                Log.d("[Bluetooth]", "Input stream disconnected", e);
                MSG(MESSAGE_DISCONNECTED);
                return false;
            }
        }


        public void write(byte[] bytes)
        {
            l("Writing: " + new String(bytes));
            l("Bytes Length: " + bytes.length);
            OutputStream stream;

            OutputStream tmpOut = null;
            try {
                tmpOut = connectedSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("[Bluetooth]", "Error occurred when creating output stream", e);
            }
            stream = tmpOut;

            try
            {
                l("Writing bytes to outstream");
                stream.write(bytes);
            }
            catch (Exception e)
            {
                Log.e("[Bluetooth]", "Failed to send data", e);
                disconnect();
            }
        }

        /*
        public void write(byte[] bytes, int device)
        {
            l("Writing " + new String(bytes));
            l("Bytes length: " + bytes.length);
            OutputStream out = null;
            try
            {
                out = connectedSockets.get(device).getOutputStream();
                out.write(bytes);
            }
            catch (IndexOutOfBoundsException e)
            {
                l("Failed to write, device not found at index: " + device);
            }
            catch (IOException e)
            {
                l("Failed to write. IOException." + e.getMessage());
                e.printStackTrace();
            }
        }
        */

        public void cancel()
        {
            try
            {
                connectedSocket.close();
            }
            catch (java.io.IOException e)
            {
                Log.e("[Bluetooth]", "Failed to close socket", e);
            }
        }
    }


    private synchronized void setupBluetooth()
    {
        l("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
            l("Bluetooth not detected");
            msgToast("Error, Bluetooth not Detected!");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

            if (requestCode==1)
            {
                bluetoothFailed = resultCode != RESULT_OK;
            }
    }

    protected synchronized void Connect()
    {
        l("Connecting");

        if (bluetoothFailed) {
            l("Failed to connect, bluetooth setup was unsuccessful");
            return;
        }

        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() < 1)
        {
            msgToast("Not enough devices paired");
            return;
        }

        connectThread = new ConnectThread(pairedDevices);
        connectThread.start();
    }

    private synchronized void connectSocket(BluetoothSocket socket)
    {
        if (!Destroyed())
        {
            l("Storing socket in connected devices");
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
            msgToast("CONNECTED!");
            MSG(MESSAGE_CONNECTED);
        }
    }

    private synchronized void msgToast(String msg)
    {
        m_handler.obtainMessage(MESSAGE_TOAST, msg.getBytes().length, -1, msg.getBytes()).sendToTarget();
    }

    protected synchronized void Write(String text)
    {
        l("EVENT: send() " + text);
        try {
            connectedThread.write(text.getBytes());
        }
        catch (Exception e)
        {
            l("Failed to write: Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public interface Callable
    {
        void call(String input);
    }

    protected Callable inputEvent;
    protected Callable disconnectEvent;
    protected Callable sendMessageEvent;
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:

                String message = (String) msg.obj;
                //m_sendButton.setText(message);

                inputEvent.call(message);
                break;
            case MESSAGE_TOAST:
                String t = new String((byte[]) msg.obj);

                l("TOASTING: " + t);

                //MessageBox(t);

                sendMessageEvent.call(t);
                break;
            case MESSAGE_DISCONNECTED:
                //MessageBox("DISCONNECTED FROM DEVICE");
                disconnectEvent.call("");
                break;
        }
    }

    protected boolean ISDESTROYED = false;
    protected synchronized boolean Destroyed() { return ISDESTROYED; }
    protected synchronized void Destroyed(boolean value) { ISDESTROYED = value; }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        l("Destroying application threads");
        Destroyed(true);
        if (bluetoothFailed)
            return;
        if (connectedThread != null) {
            connectedThread.close();
            connectedThread.interrupt();
        }

    }

    private void disconnect()
    {
        connectedThread.close();
        connectedThread.interrupt();
    }
}
