package org.hotteam67.scouter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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

public abstract class BluetoothClientActivity extends AppCompatActivity {


    // Messages, for when any event happens, to be sent to the main thread
    final int MESSAGE_INPUT = 0;
    final int MESSAGE_DISCONNECTED = 2;
    final int MESSAGE_CONNECTED = 3;

    // Send a specific message, from the above list
    private synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    // The multi-thread handler for passing messages about bluetooth connection
    private Handler m_handler;

    // Simple log function
    void l(String s)
    {
        Log.d(TAG, s);
    }

    // The log tag
    private static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";

    // Whether the bluetooth hardware setup has completely failed (typically means something like it failed to be enabled)
    private boolean bluetoothFailed = false;

    // Adapter to the hardware bluetooth device
    private BluetoothAdapter m_bluetoothAdapter;

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
    void MessageBox(String text)
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

    // Accept incoming bluetooth connections thread, actual member and the definition
    private AcceptThread acceptThread;
    private class AcceptThread extends Thread {
        final BluetoothServerSocket connectionSocket;
        AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = m_bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("ConnectDevice", Constants.uuid);
            }
            catch (java.io.IOException e)
            {
                Log.e("[Bluetooth]", "Socket connection failed", e);
            }


            connectionSocket = tmp;
        }

        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                BluetoothSocket conn = null;
                try
                {
                    conn = connectionSocket.accept();
                }
                catch (java.io.IOException e)
                {
                    // Log.e("[Bluetooth]", "Socket acception failed", e);
                }

                if (conn != null)
                {
                    connectSocket(conn);
                    MSG(MESSAGE_CONNECTED);
                    break;
                }
            }
            try
            {
                connectionSocket.close();
            } catch (IOException e)
            {
                // Already closed, probably
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            l("Accept Thread Ended!");
        }
    }

    /*
    Connected Thread
     */
    private ConnectedThread connectedThread;
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket connectedSocket;
        private byte[] buffer;

        ConnectedThread(BluetoothSocket socket)
        {
            connectedSocket = socket;
        }

        void close()
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

                if (Thread.currentThread().isInterrupted())
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


        void write(byte[] bytes)
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
    }


    private synchronized void setupBluetooth()
    {
        l("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
            l("Bluetooth not detected");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        else
        {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

            if (requestCode==1)
            {
                bluetoothFailed = resultCode != RESULT_OK;
                setupBluetooth();
            }
    }

    private synchronized void connectSocket(BluetoothSocket socket)
    {
        if (!Destroyed())
        {
            l("Storing socket in connected devices");
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
            MSG(MESSAGE_CONNECTED);
        }
    }

    synchronized void Write(String text)
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

    void handle(Message msg)
    {
        // Do nothing, OVERRIDE ME
    }

    private boolean ISDESTROYED = false;
    private synchronized boolean Destroyed() { return ISDESTROYED; }
    private synchronized void SetDestroyed() { ISDESTROYED = true; }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        l("Destroying application threads");
        SetDestroyed();
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

        acceptThread = new AcceptThread();
        acceptThread.start();
    }
}
