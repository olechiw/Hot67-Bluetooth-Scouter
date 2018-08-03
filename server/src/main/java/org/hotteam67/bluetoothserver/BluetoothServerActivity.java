package org.hotteam67.bluetoothserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.hotteam67.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class BluetoothServerActivity extends AppCompatActivity
{
    // Messages, for when any event happens, to be sent to the main thread
    public static final int MESSAGE_INPUT = 0;
    public static final int MESSAGE_OTHER = 1;
    public static final int MESSAGE_DISCONNECTED = 2;
    public static final int MESSAGE_CONNECTED = 3;

    // Number of active and allowed devices
    private static final int allowedDevices = 7;

    // Bluetooth hardware adapter
    protected BluetoothAdapter m_bluetoothAdapter;
    protected boolean bluetoothFailed = false;
    public static final int REQUEST_BLUETOOTH = 1;

    private Handler m_handler;

    protected void SetHandler(Handler h)
    {
        m_handler = h;
    }

    // Send a specific message, from the above list
    public synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    // Simple log function
    protected void l(String s)
    {
        Log.d("BLUETOOTH_SCOUTER_DEBUG", s);
    }

    // Accept incoming bluetooth connections thread, actual member and the definition
    AcceptThread acceptThread;
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
                }
            }
            l("Accept Thread Ended!");
        }

        public void cancel()
        {
            try
            {
                connectionSocket.close();
            }
            catch (java.io.IOException e)
            {
                // Log.e("[Bluetooth]", "Socket close failed", e);
            }
        }
    }

    private void connectSocket(BluetoothSocket connection)
    {

        if (connectedThreads.size() < allowedDevices)
        {
            l("Received a connection, adding a new thread: " + connectedThreads.size());
            ConnectedThread thread = new ConnectedThread(connection);
            thread.setId(connectedThreads.size() + 1);
            thread.start();
            connectedThreads.add(thread);
        }

    }

    // Disconnect a specific connected device, usually called from the thread itself
    private synchronized void disconnect(ConnectedThread thread)
    {
        thread.close();
        thread.interrupt();
        connectedThreads.remove(thread);
    }

    //
    // An arraylist of threads for each connected device,
    // with a unique id for when they finish so they may be removed
    //
    private ArrayList<ConnectedThread> connectedThreads = new ArrayList<>();
    private class ConnectedThread extends Thread
    {
        private BluetoothSocket connectedSocket;
        private byte[] buffer;
        private int id;

        private void setId(int i ) { id = i; }

        ConnectedThread(BluetoothSocket sockets)
        {
            connectedSocket = sockets;
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
            disconnect(this);
            MSG(MESSAGE_DISCONNECTED);
        }

        private boolean read(InputStream stream)
        {
            buffer = new byte[1024];
            int numBytes;
            try
            {
                numBytes = stream.read(buffer);

                l("Reading Bytes of Length:" + numBytes);

                m_handler.obtainMessage(MESSAGE_INPUT, numBytes, id, new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "")).sendToTarget();
                return true;
            }
            catch (java.io.IOException e)
            {
                Log.d("[Bluetooth]", "Input stream disconnected", e);
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
                disconnect(this);
            }
        }


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

    // Write to all threads, returns number of connected devices
    protected synchronized int WriteAllDevices(byte[] bytes)
    {
        // Send to each device
        for (ConnectedThread device : connectedThreads) {
            device.write(bytes);
        }
        return connectedThreads.size();
    }

    // Write a specific thread (by index). Returns number of connected devices
    protected synchronized int WriteDevice(byte[] bytes, int index)
    {
        if (connectedThreads.size() > index)
            connectedThreads.get(index).write(bytes);
        return connectedThreads.size();
    }

    protected synchronized int GetDevices()
    {
        return connectedThreads.size();
    }

    // Initialize the bluetooth hardware adapter
    protected synchronized void setupBluetooth(Runnable oncomplete)
    {
        l("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
            l("Bluetooth not detected");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH);
        }
        else
        {
            setupThreads();
            oncomplete.run();
        }

    }

    //
    // This is to handle the enable bluetooth activity,
    // and disable all attempts at bluetooth functionality
    // if for some reason the user denies permission
    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if (requestCode==REQUEST_BLUETOOTH)
        {
            bluetoothFailed = resultCode != RESULT_OK;
            setupThreads();
        }
    }

    // Initialize the accept bluetooth connections thread
    private void setupThreads()
    {
        if (!bluetoothFailed) {
            l("Setting up accept thread");
            acceptThread = new AcceptThread();

            l("Running accept thread");
            acceptThread.start();
        }
        else
            l("Attempted to setup threads, but bluetooth setup has failed");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        l("Destroying application threads");
        if (bluetoothFailed)
            return;
        if (acceptThread.connectionSocket != null && ! acceptThread.isInterrupted())
        {
            try
            {
                acceptThread.connectionSocket.close();
            } catch (java.io.IOException e)
            {
                l("Connection socket closing failed: " + e.getMessage());
            }
        }
        for (ConnectedThread thread : connectedThreads)
        {
            thread.close();
            thread.interrupt();
        }
    }
}
