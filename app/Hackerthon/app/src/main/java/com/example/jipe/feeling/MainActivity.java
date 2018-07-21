package com.example.jipe.feeling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView connectStatusLabel, searchStatusLebal, textAddressList;
    Button btnConnect, btnSend, btnQuit, btnSearch;
    EditText etSend, etConnectAddress;
    // AutoMarqueeTextView etReceived;
    TextView etReceived;

    // device variables
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothSocket btSocket = null;

    private OutputStream outStream = null;

    private InputStream inStream = null;

    // date received thread
    private ReceiveThread rThread=null;

    // received data
    String receiveData="";

    MyHandler handler;

    ArrayList<String> addressList = new ArrayList<String>();
    boolean enableShowAddressList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitComponents();
        InitBluetooth();
        InitHandler();

        SetButtonClickListener();
    }

    public void InitComponents() {
        connectStatusLabel=(TextView)this.findViewById(R.id.textView1);
        searchStatusLebal = this.findViewById(R.id.text_search_status);
        textAddressList = this.findViewById(R.id.editText_address_list);
        btnConnect=(Button)this.findViewById(R.id.button1);
        btnSend=(Button)this.findViewById(R.id.button2);
        btnQuit=this.findViewById(R.id.button3);
        btnSearch = this.findViewById(R.id.button_search);
        etSend=(EditText)this.findViewById(R.id.editText1);
        etReceived=this.findViewById(R.id.receivedDataText);
        etConnectAddress = this.findViewById(R.id.editText_address);

        // setting sscrolling method
        etReceived.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    public void InitBluetooth() {
        // Get a bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "You device do not support bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);
        IntentFilter filter2=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter2);
    }

    public void InitHandler() {
        handler=new MyHandler();
    }

    public void SetButtonClickListener() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBluetoothAdapter.isEnabled())
                {
                    mBluetoothAdapter.enable();
                }
                mBluetoothAdapter.startDiscovery();

                // connect
                new ConnectTask().execute(etConnectAddress.getText().toString());
            }
        });

        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(btSocket!=null)
                {
                    try {
                        btSocket.close();
                        btSocket=null;
                        if(rThread!=null)
                        {
                            rThread.join();
                        }
                        connectStatusLabel.setText("当前连接已断开");
                        // etReceived.setText("");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new SendInfoTask().execute(etSend.getText().toString());
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                // 打开蓝牙
                if(!mBluetoothAdapter.isEnabled())
                {
                    mBluetoothAdapter.enable();
                }
                enableShowAddressList = true;
                mBluetoothAdapter.startDiscovery();
                textAddressList.setText("");
                searchStatusLebal.setText("正在搜索....");
            }
        });
    }

    // Initialize broad cast
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(enableShowAddressList) {
                Log.e("action", action);
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String address = device.getAddress();
                    if(!addressList.contains(address)) {
                        textAddressList.append("\n" + device.getName() + "==>" + address + "\n");
                    }
                } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    searchStatusLebal.setText("Search finished...");
                    enableShowAddressList = false;
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // connect to other bluetooth
    class ConnectTask extends AsyncTask<String,String,String>
    {
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);

            try {
                UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                btSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);

                btSocket.connect();

                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");
            } catch (IOException e) {
                try {
                    btSocket.close();
                    return "Socket create failed:" + e.toString();
                } catch (IOException e2) {
                    Log .e("error","ON RESUME: Unable to close socket during connection failure", e2);
                    return "Socket close failed";
                }
            }
            // Cancle search
            mBluetoothAdapter.cancelDiscovery();

            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("error", "ON RESUME: Output stream creation failed.", e);
                return "Socket stream create failed: " + e.toString();
            }

            return "BlueTooth connected, Socket created";
        }

        @Override
        // This function is run in the main thread
        protected void onPostExecute(String result) {
            // start moniter once connected
            rThread=new ReceiveThread();

            rThread.start();

            connectStatusLabel.setText(result);

            super.onPostExecute(result);
        }
    }

    // Send messagge to bluetooth
    class SendInfoTask extends AsyncTask<String,String,String>
    {
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            connectStatusLabel.setText(result);

            // Cleat the send edit text view
            etSend.setText("");
        }

        @Override
        protected String doInBackground(String... arg0) {
            if(btSocket==null)
            {
                return "connection not created";
            }

            if(arg0[0].length()>0)
            {
                byte[] msgBuffer = arg0[0].getBytes();

                try {
                    outStream.write(msgBuffer);
                } catch (IOException e) {
                    Log.e("error", "ON RESUME: Exception during write.", e);
                    return "Send failed:" + e.toString();
                }
            }
            return "Send successfully";
        }

    }

    // Thread to receive date from blue tooth
    class ReceiveThread extends Thread
    {
        @Override
        public void run() {

            while(btSocket!=null )
            {
                byte[] buff = new byte[1024];
                try {
                    inStream = btSocket.getInputStream();
                    inStream.read(buff);

                    processBuffer(buff,1024);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processBuffer(byte[] buff,int size)
        {
            int length=0;
            for(int i=0;i<size;i++)
            {
                if(buff[i]>'\0')
                {
                    length++;
                }
                else
                {
                    break;
                }
            }

            byte[] newbuff=new byte[length];

            for(int j=0;j<length;j++)
            {
                newbuff[j]=buff[j];
            }

            receiveData = new String(newbuff) + receiveData;
            Log.e("Data", receiveData);
            if(receiveData.length()>10000){
                receiveData = receiveData.substring(0, 100);
            }
            Message msg=Message.obtain();
            msg.what=1;
            handler.sendMessage(msg);
        }
    }

    // update the page
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    int firstLine = receiveData.indexOf('\n');
                    etReceived.setText(receiveData.substring(firstLine<0 ? 0 : firstLine));
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        // 解除注册
        unregisterReceiver(mReceiver);

        try {
            if(rThread!=null)
            {
                btSocket.close();
                btSocket=null;

                rThread.join();
            }

            this.finish();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}