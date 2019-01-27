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
import java.util.UUID;


/**
 * A bluetooth client that listens for a connection from the master device and sends its inputs to
 * the main thread
 */
public abstract class BluetoothClientActivity extends AppCompatActivity {

    protected String UUID = "";

    /**
     * Messages, for when any event happens, to be sent to the main thread
     */
    static class MessageTypes
    {
        static final int MESSAGE_INPUT = 0;
        static final int MESSAGE_CONNECTED = 3;
        static final int MESSAGE_DISCONNECTED = 2;
    }

    /**
     * Send a specific message, from the MessageTypes list, to the main thread
     */
    private synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    /**
     * The multi-thread handler for passing messages about bluetooth connection
     */
    private Handler m_handler;

    /**
     * Whether the bluetooth hardware setup has completely failed (typically means something like it failed to be enabled)
     */
    private boolean bluetoothFailed = false;

    // Adapter to the hardware bluetooth device
    private BluetoothAdapter m_bluetoothAdapter;

    /**
     * The constructor
     * @param savedInstanceState potential previous instance, ignored
     */
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

        Constants.Log("Setting up bluetooth");
    }

    /**
     * Show a messagebox with the given text
     * @param text text to display in the messagebox
     */
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
            Constants.Log(e);
        }
    }

    /**
     * The acceptThread instance that will handle incoming connections
     */
    private AcceptThread acceptThread;

    /**
     * Handles the accepting of connections from the master device, ending once a connection is
     * received
     */
    private class AcceptThread extends Thread
    {
        final BluetoothServerSocket connectionSocket;

        /**
         * Constructor, initialize the socket listening for the Constants.uuid
         */
        AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = m_bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("ConnectDevice",
                        java.util.UUID.fromString(UUID));
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }

            connectionSocket = tmp;
        }

        /**
         * Run, listening for a connection and handle either a termination of the socket or a
         * successful connection
         */
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                if (connectionSocket == null)
                {
                    disconnect();
                    break;
                }

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
                    MSG(MessageTypes.MESSAGE_CONNECTED);
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
                Constants.Log(e);
            }
        Constants.Log("Accept Thread Ended!");
        }
    }

    /**
     * Connected thread once a master device has been accepted
     */
    private ConnectedThread connectedThread;

    /**
     * Thread for once the device is connected, handling input by sending it to the main thread, or
     * accounting for disconnections
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket connectedSocket;
        private byte[] buffer;

        /**
         * Constructor
         * @param socket takes the socket of an already connected device
         */
        ConnectedThread(BluetoothSocket socket)
        {
            connectedSocket = socket;
        }

        /**
         * A cleanup function to close the socket and handle exceptions
         */
        void close()
        {
            try
            {
                connectedSocket.close();
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }
        }

        /**
         * The run thread, checks for input and sends it to the thread, or handles the exception and
         * informs the main thread that the socket is closed
         */
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                InputStream stream;
                InputStream tmpIn = null;
                try {
                Constants.Log("Loading input stream");
                    tmpIn = connectedSocket.getInputStream();
                } catch (IOException e) {
                    Log.e("[Bluetooth]", "Error occurred when creating input stream", e);
                }
                stream = tmpIn;
                if (stream == null) break;

            Constants.Log("Reading stream");
                if (!read(stream))
                {
                    break;
                }

                if (Thread.currentThread().isInterrupted())
                {
                    break;
                }
            }
        Constants.Log("Connected Thread Ended!!!");
            disconnect();
        }

        /**
         * read a thread
         * @param stream the input stream from the connected socket
         * @return boolean whether the connection is broken
         */
        private boolean read(InputStream stream)
        {
            buffer = new byte[1024];
            int numBytes;
            try
            {
                numBytes = stream.read(buffer);

            Constants.Log("Reading Bytes of Length:" + numBytes);

                m_handler.obtainMessage(MessageTypes.MESSAGE_INPUT, numBytes, -1, new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "")).sendToTarget();
                return true;
            }
            catch (java.io.IOException e)
            {
                Log.d("[Bluetooth]", "Input stream disconnected", e);
                MSG(MessageTypes.MESSAGE_DISCONNECTED);
                return false;
            }
        }

        /**
         * Write output to the connected socket
         * @param bytes the bytes to send into the stream
         */
        void write(byte[] bytes)
        {
        Constants.Log("Writing: " + new String(bytes));
        Constants.Log("Bytes Length: " + bytes.length);
            OutputStream stream;

            OutputStream tmpOut = null;
            try {
                tmpOut = connectedSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("[Bluetooth]", "Error occurred when creating output stream", e);
            }
            stream = tmpOut;

            if (stream == null)
            {
                disconnect();
                return;
            }

            try
            {
            Constants.Log("Writing bytes to outstream");
                stream.write(bytes);
            }
            catch (Exception e)
            {
                Log.e("[Bluetooth]", "Failed to send data", e);
                disconnect();
            }
        }
    }

    /**
     * Setup the bluetooth adapter of the activity
     */
    protected synchronized void setupBluetooth()
    {
    Constants.Log("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
        Constants.Log("Bluetooth not detected");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_PERMISSION);
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

            if (requestCode==Constants.REQUEST_ENABLE_PERMISSION)
            {
                bluetoothFailed = resultCode != RESULT_OK;
                setupBluetooth();
            }
    }

    /**
     * Get a connected socket from the acceptThread and start a connected thread.
     * @param socket the newly connected device
     */
    private synchronized void connectSocket(BluetoothSocket socket)
    {
        if (!Destroyed())
        {
            Constants.Log("Storing socket in connected devices");
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
            MSG(MessageTypes.MESSAGE_CONNECTED);
        }
    }

    /**
     * Write a string to the connected bluetooth device
     * @param text text to send
     */
    synchronized void BluetoothWrite(String text)
    {
    Constants.Log("EVENT: send() " + text);
        try {
            connectedThread.write(text.getBytes());
        }
        catch (Exception e)
        {
        Constants.Log("Failed to write: Exception: " + e.getMessage());
            Constants.Log(e);
        }
    }

    /**
     * Function meant to be overrided, will handle all of the bluetooth input, runs on the main
     * thread
     * @param msg the message sent to the thread
     */
    void handle(Message msg)
    {
        // Do nothing, OVERRIDE ME
    }

    /**
     * Whether the activity is cleaned up
     */
    private boolean ISDESTROYED = false;
    private synchronized boolean Destroyed() { return ISDESTROYED; }
    private synchronized void SetDestroyed() { ISDESTROYED = true; }


    /**
     * Clean up all of the threads and sockets when the activity is destroyed
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    Constants.Log("Destroying application threads");
        SetDestroyed();
        if (bluetoothFailed)
            return;
        if (connectedThread != null) {
            connectedThread.close();
            connectedThread.interrupt();
        }

    }

    /**
     * Close all sockets
     */
    private void disconnect()
    {
        if (connectedThread != null) {
            connectedThread.close();
            connectedThread.interrupt();
        }
    }
}
