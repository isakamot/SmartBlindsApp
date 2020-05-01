package com.example.smartblinds;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class temp_config extends AppCompatActivity {
    FireStore fireStore;
    Data document_data;
    EditText temp_close_et, temp_open_et;
    Button save_btn;
    String temp_close, temp_open;
    String device_IP;
    String message;
    tcp TCP_stuff;
    ConnectTask connectTask;
    Boolean done_close;
    Boolean done_open;
    Boolean close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature);

        temp_close_et = findViewById(R.id.tempconfig_close_temp);
        temp_open_et = findViewById(R.id.temp_config_open_temp);
        save_btn = findViewById(R.id.temp_config_save_btn);
        fireStore = new FireStore();
        connectTask = new ConnectTask();
        done_close = false;
        done_open = false;
        close = false;

        Get_Data();

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                temp_close = temp_close_et.getText().toString();
                temp_open = temp_open_et.getText().toString();
                fireStore.update_data("Temp_Close", temp_close);
                fireStore.update_data("Temp_Open", temp_open);
                if(TCP_stuff != null){
                    send_config_data();
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        Log.d("MSG", "Going back");
        close = true;
        TCP_stuff.stopClient();
        connectTask.cancel(true);
        Intent i = new Intent(temp_config.this, main.class);
        startActivity(i);
    }

    public void Get_Data() {
        final Handler handler = new Handler();
        Runnable get_data = new Runnable() {
            @Override
            public void run() {
                fireStore.get_data();
                while ((document_data = fireStore.get_data_to_app()) == null);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        temp_close_et.setText(document_data.Temp_Close);
                        temp_open_et.setText(document_data.Temp_Open);
                        Log.d("Done", "Done");
                    }
                });

                //Connect to Blinds
                device_IP = document_data.DeviceIP;
                connectTask.execute();
            }
        };
        Thread thread = new Thread(get_data);
        thread.start();
    }

    public void send_config_data(){
        Runnable send_data = new Runnable() {
            @Override
            public void run() {

                Log.d("MSG", "Sending temp close");
                TCP_stuff.sendMessage("TEMP_CLOSE/"+temp_close+"\r\n");
                while(!done_close);
                try{
                    Thread.sleep(800);
                }
                catch (Exception e){
                    Log.e("Error", "Error",e);
                }
                Log.d("MSG", "Sending temp open");
                TCP_stuff.sendMessage("TEMP_OPEN/"+temp_open+"\r\n");
                while (!done_open);
                Log.d("MSG", "DONE");

                //Notify user that it is done
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(temp_config.this, "Successfully Saved", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        Thread thread = new Thread(send_data);
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
            //Show error message
            if (!TCP_stuff.run() && !close) {
                //Show error message
                runOnUiThread(new Runnable() {
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(temp_config.this);
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
            message = values[0];
            if (message.contains("CLOSE_OK")){
                done_close = true;
                Log.d("MSG", "DONE close");
            }
            else if (message.contains("OPEN_OK")){
                done_open = true;
                Log.d("MSG", "DONE OPEN");
            }
        }
    }
}


