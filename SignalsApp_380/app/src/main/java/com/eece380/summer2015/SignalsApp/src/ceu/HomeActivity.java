package com.eece380.summer2015.SignalsApp.src.ceu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.eece380.summer2015.SignalsApp.R;
import com.jjoe64.graphview.GraphView;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;
import android.util.SparseBooleanArray;


public class HomeActivity extends Activity {

    public static String btName, signalName;
    private DeviceConfiguration newConfiguration;
    public String[] activeChannels = {"EMG", "EDA", "ECG", "ACC"};
    public int numberOfActiveChannels = 4;
    public String[] SignalNames = {"EMG", "EDA", "ECG", "ACC"};
    Context context = this;
    Button mButton1, mButton2, mButton3;
    boolean clicked = false;
    Graph[] graph;
    public String recordingName = "MYRECORDING";

    public static final String KEY_DURATION = "duration";
    public static final String KEY_RECORDING_NAME = "recordingName";
    public static final String KEY_CONFIGURATION = "configSelected";
    private  final int maxDataCount = 10000;
    private Chronometer chronometer;
    private boolean isServiceBounded = false;
    private boolean recordingOverride = false;
    private boolean drawState = true;
    private boolean goToEnd = true;
    private boolean serviceError = false;
    private boolean connectionError = false;
    public static boolean btConnectError = false;
    private Messenger serviceMessenger = null;
    private final Messenger activityMessenger = new Messenger(new IncomingHandler());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ly_home);
        setConfigurationDefaults();
        mButton1 = (Button) findViewById(R.id.button1);
        mButton2 = (Button) findViewById(R.id.button2);
        mButton3 = (Button) findViewById(R.id.button3);
        graph = new Graph[4];
        initGraphLayout();
    }

    public void onClickButton1(View view) {
        //Bluetooth Button Clicked
        Toast.makeText(getApplicationContext(), "CLICKED BT", Toast.LENGTH_SHORT).show();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                if (mBluetoothAdapter.isDiscovering()) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Discovering", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                    final String[] string = new String[pairedDevices.size()];
                    for (int j = 0; j < pairedDevices.size(); j++) {
                        string[j] = " ";
                    }
                    int count = 0;
                    for (BluetoothDevice bt : pairedDevices) {
                        string[count] = bt.getName();
                        count++;
                    }

                    AlertDialog dialog;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("PAIRED DEVICES");
                    builder.setItems(string, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            btName = string[position];
                            mButton1.setText("BLUETOOTH: " + btName);
                        }
                    });
                    dialog = builder.create();
                    dialog.show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth Not Enabled", Toast.LENGTH_SHORT).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    public void onClickButton2(View view) {
        //signal button clicked
        final String[] string = new String[4];
        string[0] = "EMG";
        string[1] = "EDA";
        string[2] = "ECG";
        string[3] = "ACC";

        final boolean[] states = new boolean[4];
        for (int j=0; j<4; j++){
            states[j] = false;
        }

        AlertDialog dialog;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SIGNALS");

        builder.setMultiChoiceItems(string, states, new DialogInterface.OnMultiChoiceClickListener(){
            public void onClick(DialogInterface dialogInterface, int item, boolean state) {

            }
        });

        builder.setPositiveButton("SELECT", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SparseBooleanArray Checked = ((AlertDialog)dialog).getListView().getCheckedItemPositions();

                numberOfActiveChannels = Checked.size();
                int count = 0;

                for (int j=0; j<4; j++) {
                    activeChannels[j] = null;
                }

                if (Checked.get(0) == true) {
                    activeChannels[0] = "EMG";
                    SignalNames[count] = "EMG";
                    count++;
                }
                else activeChannels[0] = null;

                if (Checked.get(1) == true) {
                    activeChannels[1] = "EDA";
                    SignalNames[count] = "EDA";
                    count++;
                }
                else activeChannels[1] = null;

                if (Checked.get(2) == true) {
                    activeChannels[2] = "ECG";
                    SignalNames[count] = "ECG";
                    count++;
                }
                else activeChannels[2] = null;

                if (Checked.get(3) == true) {
                    activeChannels[3] = "ACC";
                    SignalNames[count] = "ACC";
                    count++;
                }
                else activeChannels[3] = null;

                for (int j=count; j<4; j++){
                    SignalNames[j] = null;
                }

                //Toast.makeText(context, numberOfActiveChannels +" "+activeChannels[0]+" "+activeChannels[1]+" "+activeChannels[2]+" "+activeChannels[3], Toast.LENGTH_LONG).show();
                //Toast.makeText(context, SignalNames[0]+" "+SignalNames[1]+" "+SignalNames[2]+" "+SignalNames[3], Toast.LENGTH_LONG).show();

                setConfigurationDefaults();
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    public void onClickButton3(View view) {
        //start/stop button clicked
        if (clicked) {
            clicked = false;
            //Toast.makeText(this, "not clicked", Toast.LENGTH_SHORT).show();
            mButton3.setText("START");
            stopRecording();
        } else {
            clicked = true;
            //Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
            mButton3.setText("STOP");
            startPressed();
        }
    }

    private void setConfigurationDefaults() {
        newConfiguration = new DeviceConfiguration(this);
        newConfiguration.setNumberOfBits(12);
        newConfiguration.setActiveChannels(activeChannels);
        newConfiguration.setDisplayChannels(activeChannels);
        newConfiguration.setName("MYconfig");
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        Date date = new Date();
        newConfiguration.setCreateDate(dateFormat.format(date));
        newConfiguration.setVisualizationFrequency(100);
        newConfiguration.setSamplingFrequency(100);
        newConfiguration.setMacAddress(btName);
    }

    public void initGraphLayout(){

        graph[0] = new Graph(this, SignalNames[0] + "1");
        LinearLayout layout0 = (LinearLayout) findViewById(R.id.graphEMG);
        layout0.addView(graph[0].getGraphView());
        if (numberOfActiveChannels < 1) {
            layout0.setVisibility(View.GONE);
            Toast.makeText(this, "No Signal Selected", Toast.LENGTH_LONG).show();
        }
        else layout0.setVisibility(View.VISIBLE);

        graph[1] = new Graph(this, SignalNames[1] + "2");
        LinearLayout layout1 = (LinearLayout) findViewById(R.id.graphECG);
        layout1.addView(graph[1].getGraphView());
        if (numberOfActiveChannels < 2) layout1.setVisibility(View.GONE);
        else layout1.setVisibility(View.VISIBLE);

        graph[2] = new Graph(this, SignalNames[2] + "3");
        LinearLayout layout2 = (LinearLayout) findViewById(R.id.graphEDA);
        layout2.addView(graph[2].getGraphView());
        if (numberOfActiveChannels < 3) layout2.setVisibility(View.GONE);
        else layout2.setVisibility(View.VISIBLE);

        graph[3] = new Graph(this, SignalNames[3] + "4");
        LinearLayout layout3 = (LinearLayout) findViewById(R.id.graphACC);
        layout3.addView(graph[3].getGraphView());
        if (numberOfActiveChannels < 4) layout3.setVisibility(View.GONE);
        else layout3.setVisibility(View.VISIBLE);

    }

    public void startPressed(){

        chronometer = new Chronometer(context);

        View graphsView1 = findViewById(R.id.graphEMG);
        ((ViewGroup) graphsView1).removeAllViews();
        View graphsView2 = findViewById(R.id.graphECG);
        ((ViewGroup) graphsView2).removeAllViews();
        View graphsView3 = findViewById(R.id.graphEDA);
        ((ViewGroup) graphsView3).removeAllViews();
        View graphsView4 = findViewById(R.id.graphACC);
        ((ViewGroup) graphsView4).removeAllViews();
        initGraphLayout();
        startRecording();
    }

    /**
     * Handler that receives messages from the service. It receives frames data,
     * error messages and a saved message if service stops correctly
     *
     */
    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BiopluxService.MSG_DATA: {
                    appendDataToGraphs(
                            msg.getData().getDouble(BiopluxService.KEY_X_VALUE),
                            msg.getData().getDoubleArray(BiopluxService.KEY_FRAME_DATA));
                    break;
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }

    /**
     * Bind connection used to bind and unbind with service
     * onServiceConnected() called when the connection with the service has been established,
     * giving us the object we can use to interact with the service. We are
     * communicating with the service using a Messenger, so here we get a
     * client-side representation of that from the raw IBinder object.
     */
    private ServiceConnection bindConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serviceMessenger = new Messenger(service);
            isServiceBounded = true;
            Message msg = Message.obtain(null, BiopluxService.MSG_REGISTER_CLIENT);
            msg.replyTo = activityMessenger;
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
            }
        }

        /**
         *  This is called when the connection with the service has been
         *  unexpectedly disconnected -- that is, its process crashed.
         */
        public void onServiceDisconnected(ComponentName className) {
            serviceMessenger = null;
            isServiceBounded = false;
        }
    };

    void appendDataToGraphs(double xValue, double[] data) {
        if(!serviceError){
            for (int i = 0; i < activeChannels.length; i++) {
                graph[i].getSerie().appendData(new GraphView.GraphViewData(xValue,data[i]), goToEnd, maxDataCount);
            }
        }
    }

    /**
     * Sends recording duration to the service by message when recording is
     * stopped
     */
    private void sendRecordingDuration() {
        if (isServiceBounded && serviceMessenger != null) {
            Message msg = Message.obtain(null, BiopluxService.MSG_END_RECORDING_FLAG, 0, 0);
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
            }
        }else{
        }
    }

    /**
     * Stops and saves the recording in database and data as zip file
     */
    private void stopRecording(){
        sendRecordingDuration();
        unbindFromService();
        stopService(new Intent(this, BiopluxService.class));
        drawState = true;
    }

    /**
     * Starts the recording if mac address is 'test' and recording is not
     * running OR if bluetooth is supported by the device, bluetooth is enabled,
     * mac is other than 'test' and recording is not running. Returns always
     * false for the main thread to be stopped and thus be available for the
     * progress dialog  spinning circle when we test the connection
     */
    private boolean startRecording() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btName.compareTo("test")!= 0){ // 'test' is used to launch device emulator
            if (mBluetoothAdapter == null) {
                return false;
            }
            if (!mBluetoothAdapter.isEnabled()){
                return false;
            }
        }
        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable(){
                    public void run(){
                        if(connectionError){
                            Toast.makeText(context, "Connection Error", Toast.LENGTH_LONG).show();
                        }else{
                            Intent intent = new Intent(context, BiopluxService.class);
                            intent.putExtra(KEY_RECORDING_NAME, recordingName);//recording.getName());
                            intent.putExtra(KEY_CONFIGURATION, newConfiguration);
                            startService(intent);
                            bindToService();
                            drawState = false;
                            Toast.makeText(context, "Good to Go!", Toast.LENGTH_LONG).show();

                            if (btConnectError) Toast.makeText(context, "Bluetooth Connection Error", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        if(btName.compareTo("test")==0 && !isServiceRunning() && !recordingOverride)
        {
            connectionThread.start();
        }
        else if(mBluetoothAdapter.isEnabled() && !isServiceRunning() && !recordingOverride) {
            connectionThread.start();
        }
        return false;
    }

    /**
     * Gets all the processes that are running on the OS and checks whether the
     * bioplux service is running. Returns true if it is running and false
     * otherwise
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BiopluxService.class.getName().equals(service.service.getClassName()) && service.restarting == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attaches connection with the service and passes the recording name and
     * the correspondent configuration to it on its intent
     */
    void bindToService() {
        Intent intent = new Intent(context, BiopluxService.class);
        bindService(intent, bindConnection, 0);
    }

    /**
     * Detach our existing connection with the service
     */
    void unbindFromService() {
        if (isServiceBounded) {
            unbindService(bindConnection);
            isServiceBounded = false;
        }
    }
}