package com.example.smartblinds;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class manual_control extends AppCompatActivity {

    FireStore fireStore;
    Data document_data;
    String device_IP, message;
    Button down_btn, up_btn, pitch_up_btn, pitch_down_btn;
    tcp TCP_stuff;
    ConnectTask connectTask;
    boolean exit_flag;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual);

        down_btn = findViewById(R.id.manual_down);
        up_btn = findViewById(R.id.manual_up);
        pitch_up_btn = findViewById(R.id.manual_pitch_up);
        pitch_down_btn = findViewById(R.id.manual_pitch_down);
        connectTask = new ConnectTask();
        fireStore = new FireStore();
        exit_flag = false;

        Get_Data();

        up_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("UP\r\n");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("SP\r\n");
                    }
                }
                return true;
            }
        });

        down_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("DN\r\n");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("SP\r\n");
                    }
                }
                return true;
            }
        });

        pitch_up_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("PU\r\n");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("SP\r\n");
                    }
                }
                return true;
            }
        });

        pitch_down_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("PD\r\n");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (TCP_stuff != null){
                        TCP_stuff.sendMessage("SP\r\n");
                    }
                }
                return true;
            }
        });

    }

    @Override
    public void onBackPressed(){
        TCP_stuff.sendMessage("QT\r\n");
        exit_thread();
    }

    public void exit_thread(){
        final Handler handler = new Handler();
        Runnable exit_task = new Runnable() {
            @Override
            public void run() {
                while(!message.contains("K"));
                TCP_stuff.stopClient();
                connectTask.cancel(true);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent myIntent = new Intent(manual_control.this, main.class);
                        startActivity(myIntent);
                    }
                });
            }
        };
        Thread thread = new Thread(exit_task);
        thread.start();
    }

    public void Get_Data() {
        Runnable get_data = new Runnable() {
            @Override
            public void run() {
                fireStore.get_data();
                while ((document_data = fireStore.get_data_to_app()) == null);

                //Connect to Blinds
                device_IP = document_data.DeviceIP;
                connectTask.execute();
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
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
            message = values[0];
        }
    }
}
