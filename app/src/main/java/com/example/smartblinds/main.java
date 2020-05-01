package com.example.smartblinds;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main extends AppCompatActivity {
    tcp TCP_stuff;
    Button temp_btn, time_btn, light_btn, logout_btn, refresh_btn, manual_btn;
    ConnectTask connectTask;
    String device_IP;
    String msg, temp_data, pos_data, bat_data;
    TextView temp_text, pos_text, battery_text;
    FirebaseAuth firebaseAuth;
    FireStore fireStore;
    Data document_data;
    boolean temp_config, light_config, time_config, manual_flag;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        temp_btn = findViewById(R.id.main_temp_btn);
        time_btn = findViewById(R.id.main_time_btn);
        light_btn = findViewById(R.id.main_light_btn);
        logout_btn = findViewById(R.id.main_logout_btn);
        refresh_btn = findViewById(R.id.main_refresh_btn);
        temp_text = findViewById(R.id.main_temp_txt);
        pos_text = findViewById(R.id.main_position_txt);
        battery_text = findViewById(R.id.main_battery_txt);
        manual_btn = findViewById(R.id.main_manual_btn);
        firebaseAuth = FirebaseAuth.getInstance();
        fireStore = new FireStore();
        connectTask = new ConnectTask();
        temp_config = false;
        time_config = false;
        light_config = false;
        temp_data = "";
        pos_data = "";
        bat_data = "";
        msg = "";
        device_IP = "";

        device_IP = getIntent().getStringExtra("DeviceIP");
        if (device_IP == null) {
            get_IP_from_firestore();
        }
        else{
            fireStore.init();
            fireStore.update_data("DeviceIP", device_IP);
            connectTask.execute();
        }

        msg = "";
        temp_text.setText("--F");
        pos_text.setText("--%");
        battery_text.setText("--%");

        temp_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("TEMP_CONFIG\r\n");
                    temp_config = true;
                    go_to_new_page();
                }
            }
        });

        time_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("TIME_CONFIG\r\n");
                    time_config = true;
                    go_to_new_page();
                }
            }
        });

        light_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("LIGHT_CONFIG\r\n");
                    light_config = true;
                    go_to_new_page();
                }
            }
        });

        manual_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("MANUAL\r\n");
                    manual_flag = true;
                    go_to_new_page();
                }
            }
        });

        refresh_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                GetRefreshData();
            }
        });

        logout_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                FirebaseAuth.getInstance().signOut();
                Intent I = new Intent(main.this, MainActivity.class);
                startActivity(I);
            }
        });

    }

    public void get_IP_from_firestore() {
        Runnable get_data = new Runnable() {
            @Override
            public void run() {
                fireStore.get_data();
                while ((document_data = fireStore.get_data_to_app()) == null);
                device_IP = document_data.DeviceIP;
                Log.d("msg", device_IP);
                connectTask.execute();
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
    }

    public void GetRefreshData(){
        final Handler handler = new Handler();
        Runnable get_data = new Runnable() {
            @Override
            public void run() {
                if (TCP_stuff != null) {

                    //Get Current Temperature Data
                    TCP_stuff.sendMessage("RQ_TEMP\r\n");
                    while (temp_data.equals(""));
                    try{
                        Thread.sleep(1100);
                    }
                    catch (Exception e){
                        Log.e("WAIT", "Error",e);
                    }

                    //Get Current Blind Position Data
                    TCP_stuff.sendMessage("RQ_POS\r\n");
                    while(pos_data.equals(""));
                    try{
                        Thread.sleep(1100);
                    }
                    catch (Exception e){
                        Log.e("WAIT", "Error",e);
                    }

                    //Get Current Battery Data
                    TCP_stuff.sendMessage("RQ_BAT\r\n");
                    while(bat_data.equals(""));

                    try{
                        Thread.sleep(1100);
                    }
                    catch (Exception e){
                        Log.e("WAIT", "Error",e);
                    }

                    send_curr_time();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            temp_text.setText(temp_data+"F");
                            Log.d("Done", "Done Temp");
                            pos_text.setText(pos_data+"%");
                            Log.d("Done", "Done POS");
                            battery_text.setText(bat_data+"%");
                            Log.d("Done", "Done BAT");
                        }
                    });
                }
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
    }

    public void send_curr_time(){
        Runnable send_time = new Runnable() {
            @Override
            public void run() {
                Date currentTime = Calendar.getInstance().getTime();
                String time = currentTime.toString();
                Pattern p = Pattern.compile("[0-9]{2}:[0-9]{2}");
                Matcher m = p.matcher(time);
                if (m.find()){
                    time = m.group(0);
                    Log.d("Time", time);
                    TCP_stuff.sendMessage("CUR_TIME/"+time+"\r\n");
                }
            }
        };
        Thread thread = new Thread(send_time);
        thread.start();
    }

    public void go_to_new_page(){
        final Handler handler = new Handler();
        Runnable close_task = new Runnable() {
            @Override
            public void run() {
                while(!msg.contains("K"));
                TCP_stuff.stopClient();
                connectTask.cancel(true);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (temp_config) {

                            Intent myIntent = new Intent(main.this, temp_config.class);
                            startActivity(myIntent);
                         }
                        else if (light_config){

                            Intent myIntent = new Intent(main.this, light_config.class);
                            startActivity(myIntent);
                        }
                        else if (time_config){

                            Intent myIntent = new Intent(main.this, time_config.class);
                            startActivity(myIntent);
                        }
                        else if (manual_flag){

                            Intent myIntent = new Intent(main.this, manual_control.class);
                            startActivity(myIntent);
                        }
                    }
                });

            }
        };
        new Thread(close_task).start();
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
            if (!TCP_stuff.run() && !(temp_config || light_config || manual_flag || time_config)){
                //Show error message
                runOnUiThread(new Runnable() {
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(main.this);
                        builder.setMessage("Connection Failed Please Restart App");
                        builder.setTitle("Error");
                        builder.create().show();
                    }
                });
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            msg = values[0];
            if (msg.contains("TEMP")){
                temp_data = msg;
                temp_data = temp_data.replace("TEMP", "");
                temp_data = temp_data.replace("\r\n", "");
                Log.d("TEMP", temp_data);
            }
            if (msg.contains("POS")){
                pos_data = msg;
                pos_data = pos_data.replace("POS", "");
                pos_data = pos_data.replace("\r\n", "");
                Log.d("POS", pos_data);
            }
            if (msg.contains("BAT")){
                bat_data = msg;
                bat_data = bat_data.replace("BAT", "");
                bat_data = bat_data.replace("\r\n", "");
                Log.d("BAT", pos_data);
            }
        }
    }

}
