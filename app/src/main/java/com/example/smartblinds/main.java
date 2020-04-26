package com.example.smartblinds;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class main extends AppCompatActivity {
    tcp TCP_stuff;
    Button temp_btn, time_btn, light_btn, logout_btn, refresh_btn;
    ConnectTask connectTask;
    String device_IP;
    String msg, temp_data, pos_data;
    TextView temp_text, pos_text;
    FirebaseAuth firebaseAuth;
    FireStore fireStore;
    Data document_data;


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
        firebaseAuth = FirebaseAuth.getInstance();
        fireStore = new FireStore();
        connectTask = new ConnectTask();

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

        temp_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("TEMP_CONFIG\r\n");
                    check_close_flag();
                }
            }
        });

        time_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("TIME_CONFIG\r\n");
                }
            }
        });

        light_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("LIGHT_CONFIG\r\n");
                }
            }
        });

        refresh_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                GetTempData();
                GetPosData();
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

    public void GetTempData(){
        final Handler handler = new Handler();
        Runnable get_data = new Runnable() {
            @Override
            public void run()  {
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("RQ_TEMP\r\n");
                    while(temp_data==null);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            temp_text.setText(temp_data);
                            Log.d("Done", "Done");
                        }
                    });
                }
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
    }

    public void GetPosData(){
        final Handler handler = new Handler();
        Runnable get_data = new Runnable() {
            @Override
            public void run()  {
                if (TCP_stuff != null){
                    TCP_stuff.sendMessage("RQ_POS\r\n");
                    while(pos_data == null);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            pos_text.setText(pos_data);
                            Log.d("Done", "Done");
                        }
                    });
                }
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
    }

    public void check_close_flag(){
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
                        Intent myIntent = new Intent(main.this, temp_config.class);
                        startActivity(myIntent);
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
            TCP_stuff.run();
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
        }
    }


}
