package org.hotteam67.master;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.hotteam67.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The subclass for the server activity, handles bluetooth input/output and communication with main
 * thread
 */
public abstract class BluetoothServerActivity extends AppCompatActivity
{
    protected List<String> uuidEndings = new ArrayList<>();

    /**
     * Messages for communicating with the main thread
     */
    static class Messages
    {
        static final int MESSAGE_INPUT = 0;
        static final int MESSAGE_CONNECTION_FAILED = 4;
        static final int MESSAGE_CONNECTING = 1;
        static final int MESSAGE_DISCONNECTED = 2;
        static final int MESSAGE_CONNECTED = 3;
    }

    /**
     * Number of allowed devices to be connected to the server, bluetooth will max at 7
     */
    private static final int allowedDevices = 7;

    /**
     * Software interface for the bluetooth hardware adapter
     */
    private BluetoothAdapter m_bluetoothAdapter;
    /**
     * Whether bluetooth permissions and setup failed
     */
    private boolean bluetoothFailed = false;

    /**
     * Activity Request code for bluetooth permissions if they are not enabled
     */
    private static final int REQUEST_BLUETOOTH = 1;

    /**
     * Handler that communicates with the main thread
     */
    private Handler m_handler;

    /**
     * Public function to configure the handler
     * @param h the handler to use for sending messages to the main thread
     */
    void SetHandler(Handler h)
    {
        m_handler = h;
    }

    /**
     * Send a specific message from Messages
     * @param msg the message to send
     */
    private synchronized void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    /**
     * Instance of the thread actively connecting to the devices
     */
    private ConnectThread connectThread;

    /**
     * Connect thread, which will cycle through available devices connecting to each one
     */
    private class ConnectThread extends Thread {
        private final List<BluetoothDevice> devices;

        /**
         * Constructor, turns devices into sockets with uuid
         * @param devices the devices to try to connect to
         */
        ConnectThread(List<BluetoothDevice> devices)
        {
            this.devices = devices;
        }

        /**
         * Connect to each of the sockets, only ocnnect once. If it failed send a CONNECTION_FAILED
         * message
         */
        public void run()
        {
            for (BluetoothDevice device : devices)
            {
                BluetoothSocket connectionSocket;
                boolean success = false;
                try
                {
                    // Send the message to UI thread we are connecting to device i
                    m_handler.obtainMessage(Messages.MESSAGE_CONNECTING, 0, 0,
                            device.getName()).sendToTarget();
                    Constants.Log("Connecting to device: " + device.getName());

                    for (int i = 0; i < uuidEndings.size(); ++i)
                    {
                        try
                        {
                            connectionSocket = device.createInsecureRfcommSocketToServiceRecord(
                                    UUID.fromString(Constants.incompleteUUID + uuidEndings.get(i)));
                            connectionSocket.connect();
                            Constants.Log("Succeeded UUID: " + uuidEndings.get(i));
                            connectSocket(connectionSocket);
                            success = true;
                            break;
                        }
                        catch (IOException e)
                        {
                            Constants.Log("Tried and failed UUID: " + uuidEndings.get(i));
                        }
                        catch (Exception e)
                        {
                            Constants.Log(e);
                            Constants.Log("Tried and failed UUID: " + uuidEndings.get(i));
                        }
                    }
                    if (!success) MSG(Messages.MESSAGE_CONNECTION_FAILED);
                }
                catch (Exception e)
                {
                    try
                    {
                        MSG(Messages.MESSAGE_CONNECTION_FAILED);
                    }
                    catch (Exception e2)
                    {
                        Constants.Log(e2);
                    }
                }
            }
            Constants.Log("Connect thread ended!");
        }
    }

    /**
     * Disconnect all existing threads and get user input for which paired devices should be
     * connected to, then give them to a ConnectThread
     */
    synchronized void Connect()
    {
        // Reset all connections
        for (int i = 0; i < connectedThreads.size(); ++i)
        {
            if (connectedThreads.size() > 0)
                disconnect(connectedThreads.get(0));
        }
    Constants.Log("Connecting");

        if (bluetoothFailed) {
        Constants.Log("Failed to connect, bluetooth setup was unsuccessful");
            return;
        }

        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();

        List<String> pairedNames =  new ArrayList<>();
        for (BluetoothDevice device : pairedDevices)
        {
            String name = device.getName();
            pairedNames.add(name);
        }

        PromptChoice(this, "Red Devices", pairedNames, redDevices ->
        {
            List<String> remainingNames = new ArrayList<>();
            for (String s : pairedNames)
                if (!redDevices.contains(s)) remainingNames.add(s);

            PromptChoice(this, "Blue Devices", remainingNames, blueDevices ->
            {
                // Add to final connect devices array, RED FIRST THEN BLUE.
                // If less than 6, it will do first 3 red then next 3 blue no matter what
                List<BluetoothDevice> devices = new ArrayList<>();
                for (BluetoothDevice pairedDevice : pairedDevices)
                {
                    if (redDevices.contains(pairedDevice.getName())
                            || blueDevices.contains(pairedDevice.getName()))
                        devices.add(pairedDevice);
                }
                connectThread = new ConnectThread(devices);
                connectThread.start();
            });
        });
    }

    /**
     * Prompt the user for selections, up to 3, from given options
     * @param context the parent context
     * @param title the title for the multiple choice prompt
     * @param options the options for the user
     * @param onComplete OnCompleteEvent takes an array of strings containing the selected options
     */
    private static void PromptChoice(Context context, String title, List<String> options, Constants.OnCompleteEvent<List<String>> onComplete)
    {
        List<String> result = new ArrayList<>();
        boolean[] checkedItems = new boolean[options.size()];
        new AlertDialog.Builder(context, R.style.AlertDialogTheme).setTitle(title).setMultiChoiceItems(
                options.toArray(new CharSequence[0]), checkedItems, (dialogInterface, i, b) ->
                {
                    if (b)
                    {
                        result.add(0, options.get(i)); // Put current at start of queue

                        ListView list = ((AlertDialog) dialogInterface).getListView();
                        // Limit to 6, so fix it if more than 6
                        if (result.size() > 3)
                        {
                            // Uncheck last and remove it
                            int lastIndex = options.indexOf(result.get(3));
                            list.setItemChecked(lastIndex, false);
                            checkedItems[lastIndex] = false;


                            result.remove(3);
                        }
                    }
                    else
                    {
                        result.remove(options.get(i));
                    }
                }
        ).setPositiveButton("Done", ((dialogInterface, i) ->
                onComplete.OnComplete(result))).create().show();
    }

    /**
     * Create a connected thread for a given socket, to read/write to from/to it
     * @param connection the socket for the connected device
     */
    private void connectSocket(BluetoothSocket connection)
    {

        if (connectedThreads.size() < allowedDevices)
        {
        Constants.Log("Received a connection, adding a new thread: " + connectedThreads.size());
            ConnectedThread thread = new ConnectedThread(connection);
            thread.setId(connectedThreads.size() + 1);
            thread.start();
            connectedThreads.add(thread);
            MSG(Messages.MESSAGE_CONNECTED);
        }

    }

    /**
     * Disconnect a specific connected device, usually called from the thread itself
     */
    private synchronized void disconnect(ConnectedThread thread)
    {
        thread.close();
        thread.interrupt();
        connectedThreads.remove(thread);
    }

    /**
     * An ArrayList of threads for each connected device,
     * including a unique id for when they need to be removed
     */
    private final ArrayList<ConnectedThread> connectedThreads = new ArrayList<>();

    /**
     * The thread for one connected device, handles disconnections, reading, and writing to the device
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket connectedSocket;
        private byte[] buffer;
        private int id;

        private void setId(int i ) { id = i; }

        /**
         * Constructor with the sockets
         * @param socket the socket to connect
         */
        ConnectedThread(BluetoothSocket socket)
        {
            connectedSocket = socket;
        }

        /**
         * Close the connection
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
         * Read from the socket over and over or tell the main thread it is disconnected
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

               if (!read(stream)) break;

                Constants.Log("Reading stream");

                if (Thread.currentThread().isInterrupted())
                {
                    break;
                }
            }
        Constants.Log("Connected Thread Ended!!!");
            disconnect(this);
            MSG(Messages.MESSAGE_DISCONNECTED);
        }

        /**
         * Read from given input stream
         * @param stream stream to read
         * @return whether the input was successfully obtained
         */
        private boolean read(InputStream stream)
        {
            buffer = new byte[1024];
            int numBytes;
            try
            {
                numBytes = stream.read(buffer);
                String s = new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "");

                Constants.Log("String Received: " + s);

                m_handler.obtainMessage(Messages.MESSAGE_INPUT, numBytes, id, new String(buffer, "UTF-8").substring(0, numBytes).replace("\0", "")).sendToTarget();
                return true;
            }
            catch (java.io.IOException e)
            {
                Constants.Log(e);
                return false;
            }
        }

        /**
         * Write to the connected socket
         * @param bytes the byte array to write to the thread
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
                disconnect(this);
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
                disconnect(this);
            }
        }
    }

    /**
     * Write to all connected devices
     * @param bytes byte array to send
     * @return returns the number of devices written to
     */
    synchronized int WriteAllDevices(byte[] bytes)
    {
        // Send to each device
        for (ConnectedThread device : connectedThreads) {
            device.write(bytes);
        }
        return connectedThreads.size();
    }

    /**
     * Write to a specific thread
     * @param bytes the byte array to send
     * @param index the index in connectedThreads to write to
     * @return the number of threads
     */
    synchronized int WriteDevice(byte[] bytes, int index)
    {
        if (connectedThreads.size() > index)
            connectedThreads.get(index).write(bytes);
        return connectedThreads.size();
    }

    /**
     * Get the number of connectedThreads right now
     * @return number of threads
     */
    synchronized int GetDevices()
    {
        return connectedThreads.size();
    }

    /**
     * Setup the bluetooth adapter or handle the issue + flag bluetoothFailed if it does not succeed
     * @param oncomplete the onComplete runnable to run once bluetooth is setup/permissions are gained
     */
    synchronized void setupBluetooth(Runnable oncomplete)
    {
    Constants.Log("Getting adapter");
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (m_bluetoothAdapter == null) {
        Constants.Log("Bluetooth not detected");
            bluetoothFailed = true;
        }

        if (!bluetoothFailed && !m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH);
        }
        else
        {
            oncomplete.run();
        }

    }

    /**
     * Activity result for when bluetooth is enabled by the user
     * @param requestCode code from when the activity was run
     * @param resultCode the result of the activity
     * @param data any extra data attached to the activity's end
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if (requestCode==REQUEST_BLUETOOTH)
        {
            bluetoothFailed = (resultCode != RESULT_OK);
        }
    }

    /**
     * Close all of the threads when the activity ends
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    Constants.Log("Destroying application threads");
        if (bluetoothFailed)
            return;
        for (ConnectedThread thread : connectedThreads)
        {
            thread.close();
            thread.interrupt();
        }
    }
}
