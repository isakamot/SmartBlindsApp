package com.example.smartblinds;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.net.InetAddress;
import java.util.List;

public class new_setup_2 extends AppCompatActivity {
    private String access_point_name, password;
    private TextView device_name;
    private EditText device_pass;
    ConnectivityManager conMan;
    NetworkInfo networkInfo;
    WifiManager wifiManager;
    WifiConfiguration config;
    tcp TCP_stuff;
    String device_IP = "192.168.4.1";
    String new_device_IP = "";
    ConnectTask connectTask;
    String rec_msg;
    String close_flag;
    Intent myIntent2;
    boolean connected_flag;
    boolean check_done_flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_blinds);

        device_name = findViewById(R.id.device_name_txt);
        device_pass = findViewById(R.id.device_password);

        final Button connect_btn = findViewById(R.id.connect_btn);
        final Button nxt_btn = findViewById(R.id.connect_nxt_btn);

        connectTask = new ConnectTask();

        access_point_name = getIntent().getStringExtra("DeviceName");
        device_name.setText(access_point_name);
        initialize(getApplicationContext());

        if (!access_point_name.contains("SmartBlinds")){
            connectTask.execute();
        }

        connect_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                password = device_pass.getText().toString();
                if(password.isEmpty()){
                    device_pass.setError("Enter device password");
                    device_pass.requestFocus();
                }
                else{
                    Toast.makeText(new_setup_2.this, "Connecting...", Toast.LENGTH_LONG).show();
                    ConnectionTask();
                }
            }
        });

        nxt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                conMan= (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                networkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo.isConnected()){
                    Log.d("Connected", "Connected");
                }
                if (access_point_name.contains("SmartBlinds") && networkInfo.isConnected() && connected_flag){
                    Toast.makeText(new_setup_2.this, "Success", Toast.LENGTH_SHORT).show();
                    Intent myIntent = new Intent(new_setup_2.this, new_setup.class);
                    myIntent.putExtra("DeviceConnected", "True");
                    startActivity(myIntent);
                }
                else if (networkInfo.isConnected() && connected_flag){
                    Toast.makeText(new_setup_2.this, "Success", Toast.LENGTH_SHORT).show();
                    myIntent2 = new Intent(new_setup_2.this, main.class);
                    get_IP_address();
                }
                else{
                    Toast.makeText(new_setup_2.this, "Connecction Failed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void initialize(Context context){
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean connect_to_device(String device_ssid, String device_password){
        config = new WifiConfiguration();
        config.SSID = String.format("\"%s\"", device_ssid);
        config.preSharedKey = String.format("\"%s\"", device_password);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration i : list){
            if (i.SSID != null && i.SSID.equals(config.SSID)){
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                return true;
            }
        }
        int netId = wifiManager.addNetwork(config);
        if (netId > 0) {
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            return true;
        }
        else {
            return false;
        }
    }

    public class ConnectTask extends AsyncTask<String, String, tcp> {
        @Override
        protected tcp doInBackground(String... message){
            TCP_stuff = new tcp(new tcp.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                }
            });
            TCP_stuff.set_ip(device_IP);
            TCP_stuff.run();
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            rec_msg = values[0];
        }
    }

    public void ConnectionTask(){
        Runnable conn_task = new Runnable() {
            @Override
            public void run() {
                //Connect to Smart Blinds
                if (access_point_name.contains("SmartBlinds")){
                    if (connect_to_device(access_point_name, password)){
                        connected_flag = true;
                    }
                    else{
                        connected_flag = false;
                    }
                    Log.d("Done", "Done");
                }
                //Connect to WiFi
                else{
                    String wifi_credentials = String.format("\""+access_point_name+"\","+"\""+password+"\"");
                    if (TCP_stuff != null){
                        Log.d("Sending", "Sending...");
                        TCP_stuff.sendMessage(wifi_credentials);
                        check_done_flag = false;
                        Check_Flags();
                        while(!check_done_flag);
                        if (close_flag.contains("True")){
                            Log.d("Closed", "Closed");
                            if (connect_to_device(access_point_name, password)){
                                connected_flag = true;
                                Log.d("Connected", "Connected");
                            }
                            else{
                                connected_flag = false;
                                Log.d("msg","Not Connected");
                            }
                        }
                        else{
                            connected_flag = false;
                        }
                        Log.d("Done", "Done");
                    }
                }
            }
        };
        Thread thread = new Thread(conn_task);
        thread.start();
    }

    public void get_IP_address(){
        Runnable IP_runnable = new Runnable() {
            @Override
            public void run() {
                try{
                    //Get Current WiFi's IP address
                    WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                    int ip = wifiInfo.getIpAddress();
                    String subnet = String.format("%d.%d.%d.", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff));

                    int timeout = 10;
                    for (int i = 1; i < 255; i++){
                        String host = subnet + i;
                        InetAddress address = InetAddress.getByName(host);
                        if (address.isReachable(timeout)){
                            if (address.getCanonicalHostName().contains("ESP")){
                                new_device_IP = host;
                                Log.d("MSG", new_device_IP);
                                myIntent2.putExtra("DeviceIP", new_device_IP);
                                startActivity(myIntent2);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Log.e("MSG", "Error", e);
                }
            }
        };
        Thread thread = new Thread(IP_runnable);
        thread.start();
    }

    public void Check_Flags (){
        Runnable running = new Runnable() {
            @Override
            public void run() {
                while(rec_msg == null);
                if (rec_msg.contains("OK")){
                    Log.d("MSG", rec_msg);
                    TCP_stuff.stopClient();
                    connectTask.cancel(true);
                    close_flag = "True";
                }
                else{
                    close_flag = "False";
                }
                check_done_flag = true;
                Log.d("MSG", "Done Check Flag");
            }
        };
        Thread thread = new Thread(running);
        thread.start();
    }
}





