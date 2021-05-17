package com.example.robotcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    public static final int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 1;
    private boolean isConnected = false;
    private final static String[] cmds = {
            "0",    //  go ahead
            "1",    //  go back
            "2",    //  turn left
            "3",    //  turn right
            "4",    //  spin
            "5",    //  hello
            "6",    //  music
            "7",    //  talk
            "8",    //  dance
            "9",    //  time
            "10"    // stop
    };

    //CONNECTION
    private CardView btnPower;
    private TextView txtBluetoothStatus;
    //PROGRESSBAR
    private ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        //_______________________UI_________________________
            //CONNECTION
        btnPower = findViewById(R.id.btnPower);
        txtBluetoothStatus = findViewById(R.id.txtBluetoothStatus);
            //PROGRESS
        progressBar = findViewById(R.id.progressBar);
            //MOVING
        final CardView btnSpin = findViewById(R.id.btnSpin);
        final ImageView btnTurnLeft = findViewById(R.id.btnTurnLeft);
        final ImageView btnTurnRight = findViewById(R.id.btnTurnRight);
        final ImageView btnMoveOn = findViewById(R.id.btnMoveOn);
        final ImageView btnMoveBack = findViewById(R.id.btnGoBack);
            //EXTEND
        final CardView btnHello = findViewById(R.id.btnHello);
        final CardView btnGoodbye = findViewById(R.id.btnGoodbye);
        final CardView btnTalk = findViewById(R.id.btnTalk);
        final CardView btnDance = findViewById(R.id.btnDance);
        final CardView btnTime = findViewById(R.id.btnTime);
        final CardView btnStop = findViewById(R.id.btnStop);


        //______________________FIRST START____________________________
        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device (Bluetooth) address
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            txtBluetoothStatus.setText("Connecting to " + deviceName + "...");
            txtBluetoothStatus.setTextColor(Color.rgb(247, 221, 114));
            progressBar.setVisibility(View.VISIBLE);
            btnPower.setCardBackgroundColor(Color.rgb(247, 221, 114));
            btnPower.setEnabled(false);

            // Creating bluetooth connection
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, mFilter);

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                txtBluetoothStatus.setText("Connected to " + deviceName);
                                txtBluetoothStatus.setTextColor(Color.rgb(169, 251, 215));
                                progressBar.setVisibility(View.GONE);
                                btnPower.setCardBackgroundColor(Color.rgb(169, 251, 215));
                                btnPower.setEnabled(true);
                                isConnected = true;


                                break;
                            case -1:
                                txtBluetoothStatus.setText("Connection failed. Please try again!");
                                txtBluetoothStatus.setTextColor(Color.rgb(197, 34, 51));
                                progressBar.setVisibility(View.GONE);
                                btnPower.setCardBackgroundColor(Color.rgb(197, 34, 51));
                                btnPower.setEnabled(true);
                                isConnected = false;
                                break;
                        }
                        break;

                    // Receiving message from board
                    case MESSAGE_READ:
                        String receiveMsg = msg.obj.toString(); // Read message
                        if (!receiveMsg.isEmpty())
                            Toast.makeText(MainActivity.this, "Robot: "+receiveMsg, Toast.LENGTH_SHORT).show();
//                        switch (receiveMsg.toLowerCase()){
//                            case "something":
//                                //do sth
//                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device Or Disconnect
        btnPower.setOnClickListener(view -> {
            if (isConnected){
                //Disconnect
                connectedThread.cancel();
                btnPower.setCardBackgroundColor(Color.rgb(97, 132, 216));
                txtBluetoothStatus.setTextColor(Color.WHITE);
                txtBluetoothStatus.setText("Press to connect");
                isConnected = false;
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Select Device
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // buttons click listener
        btnMoveOn.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[0]));       //0
        btnMoveBack.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[1]));     //1
        btnTurnLeft.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[2]));     //2
        btnTurnRight.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[3]));    //3
        btnSpin.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[4]));         //4
        btnHello.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[5]));        //5
        btnGoodbye.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[6]));      //6
        btnTalk.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[7]));         //7
        btnDance.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[8]));        //8
        btnTime.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[9]));         //9
        btnStop.setOnClickListener(v -> checkConnectionAndTransmitData(cmds[10]));         //10
    }

    // check bluetooth connection before transmit data
    private void checkConnectionAndTransmitData(String data){
        if(isConnected){
            Log.d(TAG, "checkConnectionAndTransmitData: connected");
            connectedThread.write(data);
        } else {
            Toast.makeText(this, "Please connect connect to the board and try again!", Toast.LENGTH_SHORT).show();
        }
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();  //HC-05
//            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //pi

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                 */
                Method method;

                method = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class } );
                tmp = (BluetoothSocket) method.invoke(bluetoothDevice, 1);

            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);

                try {
                    tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                } catch (Exception e2) {
                    Log.e(TAG, "Socket's create() method failed", e2);
                    try {
                        tmp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    } catch (Exception e3) {
                        Log.e(TAG, "Socket's create() method failed", e3);

                    }
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: "+e.getMessage() );
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from board until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Board Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: "+e.getMessage() );
            }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    // broadcast receiver: bluetooth events catching
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            txtBluetoothStatus.setText("Bluetooth is turned off");
                            txtBluetoothStatus.setTextColor(Color.rgb(197, 34, 51));
                            progressBar.setVisibility(View.GONE);
                            btnPower.setCardBackgroundColor(Color.rgb(197, 34, 51));
                            btnPower.setEnabled(false);
                            isConnected = false;
                            mmSocket.close();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            txtBluetoothStatus.setText("Bluetooth is turning off");
                            txtBluetoothStatus.setTextColor(Color.rgb(247, 221, 114));
                            progressBar.setVisibility(View.VISIBLE);
                            btnPower.setCardBackgroundColor(Color.rgb(247, 221, 114));
                            btnPower.setEnabled(false);
                            isConnected = false;
                            mmSocket.close();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            txtBluetoothStatus.setText("Press to connect");
                            txtBluetoothStatus.setTextColor(Color.rgb(250,250,250));
                            progressBar.setVisibility(View.GONE);
                            btnPower.setCardBackgroundColor(Color.rgb(97,132,216));
                            btnPower.setEnabled(true);
                            isConnected = false;
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            txtBluetoothStatus.setText("Bluetooth is turning on");
                            txtBluetoothStatus.setTextColor(Color.rgb(247, 221, 114));
                            progressBar.setVisibility(View.VISIBLE);
                            btnPower.setCardBackgroundColor(Color.rgb(247, 221, 114));
                            btnPower.setEnabled(false);
                            isConnected = false;
                            break;
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    if (isConnected) {
                        txtBluetoothStatus.setText("Disconnected");
                        txtBluetoothStatus.setTextColor(Color.rgb(197, 34, 51));
                        progressBar.setVisibility(View.GONE);
                        btnPower.setCardBackgroundColor(Color.rgb(197, 34, 51));
                        btnPower.setEnabled(true);
                        isConnected = false;
                        mmSocket.close();
                    }
                }
            } catch (Exception e){
                Log.e(TAG, "onReceive: ", e);
                Toast.makeText(MainActivity.this, "Oops! An error occurred", Toast.LENGTH_SHORT).show();
            }

        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }
}