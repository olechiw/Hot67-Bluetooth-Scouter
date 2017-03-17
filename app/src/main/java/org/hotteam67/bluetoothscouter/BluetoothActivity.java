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

public class BluetoothActivity extends AppCompatActivity {


    public final int MESSAGE_INPUT = 0;
    public final int MESSAGE_TOAST = 1;
    public final int MESSAGE_DISCONNECTED = 2;
    public final int MESSAGE_CONNECTED = 3;
    public void MSG(int msg) { m_handler.obtainMessage(msg, 0, -1, 0).sendToTarget(); }

    protected Handler m_handler;

    protected void l(String s)
    {
        Log.d(TAG, s);
    }

    public static final String TAG = "BLUETOOTH_SCOUTER_DEBUG";
    private final int numberOfDevices = 1;
    //private final List<UUID> uuid = new ArrayList<UUID>();

    private final UUID uuid = UUID.fromString("1cb5d5ce-00f5-11e7-93ae-92361f002671");
    // protected void AddUUID(String s) { uuid.add(UUID.fromString(s)); }

    protected BluetoothAdapter m_bluetoothAdapter;

/*
    private Button m_connectButton;
    private Button m_sendButton;
    */


    private List<BluetoothSocket> connectedDevices = new ArrayList<BluetoothSocket>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_scout);
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

    private void setupUI() {
/*
        l("Setting up Connect Button");
        m_connectButton = (Button) findViewById(R.id.connectButton);
        m_connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Connect();
            }
        });

        l("Setting up Send Button");
        m_sendButton = (Button) findViewById(R.id.sendButton);
        m_sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Write("SENT!");
            }
        });
*/

        l("Setting up accept thread");
        acceptThread = new AcceptThread();

        l("Running accept thread");
        acceptThread.start();

        connectedThread = new ConnectedThread(connectedDevices);
        connectedThread.start();
    }

    protected void toast(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
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
    Accept Thread
     */
    AcceptThread acceptThread;
    private class AcceptThread extends Thread {
        public final BluetoothServerSocket connectionSocket;
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = m_bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("ConnectDevice", uuid);
            }
            catch (java.io.IOException e)
            {
                Log.e("[Bluetooth]", "Socket connection failed", e);
            }


            connectionSocket = tmp;
        }

        public void run()
        {
            BluetoothSocket s = null;

            while (!Destroyed())
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
                }

                if (Thread.currentThread().isInterrupted())
                {
                    break;
                }

            }
            l("Accept Thread Ended!!");
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

            connectedSockets = new ArrayList<BluetoothSocket>();
            BluetoothSocket connectionSocket = null;
            for (BluetoothDevice device : connectedDevices) {
                connectionSocket = null;
                try {
                    l("Getting Connection");
                    connectionSocket = device.createRfcommSocketToServiceRecord(uuid);
                } catch (java.io.IOException e) {
                    Log.e("[Bluetooth]", "Failed to connect to socket", e);
                }
                connectedSockets.add(connectionSocket);
            }
        }

        public void run()
        {
            for (BluetoothSocket connectedSocket : connectedSockets)
                doConnect(connectedSocket);
        }

        private void doConnect(BluetoothSocket connectionSocket)
        {
            try
            {
                l("Connecting to socket");
                connectionSocket.connect();
            }
            catch (java.io.IOException e)
            {
                try
                {
                    connectionSocket.close();
                }
                catch (java.io.IOException e2)
                {
                    Log.e("[Bluetooth]", "Failed to close socket after failure to connect", e2);
                }

                return;
            }

            connectSocket(connectionSocket);
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
        private List<BluetoothSocket> connectedSockets;
        private byte[] buffer;

        public ConnectedThread(List<BluetoothSocket> sockets)
        {
            connectedSockets = sockets;
        }

        public void run()
        {
            while (!Destroyed())
            {
                List<InputStream> inputStreams = new ArrayList<>();

                for (BluetoothSocket socket : connectedSockets)
                {
                    InputStream tmpIn = null;
                    try {
                        tmpIn = socket.getInputStream();
                    } catch (IOException e) {
                        Log.e("[Bluetooth]", "Error occurred when creating input stream", e);
                    }

                    inputStreams.add(tmpIn);
                }

                for (InputStream stream : inputStreams)
                {
                    if (!read(stream))
                    {
                        int i = inputStreams.indexOf(stream);
                        connectedSockets.remove(i);
                    }
                }

                if (Thread.currentThread().isInterrupted())
                {
                    break;
                }
            }
            l("Connected Thread Ended!!!");
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
            List<OutputStream> outputStreams = new ArrayList<OutputStream>();

            for (BluetoothSocket socket : connectedSockets)
            {
                OutputStream tmpOut = null;
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e("[Bluetooth]", "Error occurred when creating output stream", e);
                }
                outputStreams.add(tmpOut);
            }


            for (OutputStream stream : outputStreams)
            {
                try
                {
                    l("Writing bytes to outstream");
                    stream.write(bytes);
                }
                catch (java.io.IOException e)
                {
                    Log.e("[Bluetooth]", "Failed to send data", e);
                }
            }
        }

        public void cancel()
        {
            for (BluetoothSocket socket : connectedSockets)
            {
                try
                {
                    socket.close();
                }
                catch (java.io.IOException e)
                {
                    Log.e("[Bluetooth]", "Failed to close socket", e);
                }
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
            System.exit(0);
        }

        if (!m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        else
            setupUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode==RESULT_OK)
            if (requestCode==1)
                setupUI();
    }

    protected synchronized void Connect()
    {
        l("Connecting");

        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() < numberOfDevices)
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
            connectedDevices.add(socket);
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
            connectedThread.write(text.getBytes("UTF-8"));
        }
        catch (Exception e)
        {
            l("Failed to write: Exception: " + e.getMessage());
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

                byte[] info = (byte[]) msg.obj;
                String message = new String(info);
                //m_sendButton.setText(message);

                inputEvent.call(message);
                break;
            case MESSAGE_TOAST:
                String t = new String((byte[]) msg.obj);

                l("TOASTING: " + t);

                //toast(t);

                sendMessageEvent.call(t);
                break;
            case MESSAGE_DISCONNECTED:
                //toast("DISCONNECTED FROM DEVICE");
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
        if (acceptThread.connectionSocket != null)
            try
            {
                acceptThread.connectionSocket.close();
            }
            catch (java.io.IOException e)
            {
                l("Connection socket closing failed: " + e.getMessage());
            }
    }
}
