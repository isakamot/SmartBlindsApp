package com.example.smartblinds;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class light_config extends AppCompatActivity {
    Spinner OPEN_CLOSE_dark, OPEN_CLOSE_bright;
    FireStore fireStore;
    Data document_data;
    String device_IP, message;
    String bright_selected, dark_selected;
    Button save_btn;
    tcp TCP_stuff;
    ConnectTask connectTask;
    ArrayAdapter<String> spinner_array_adapter;
    Boolean done_bright, done_dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.light);

        OPEN_CLOSE_dark = findViewById(R.id.light_dark_spinner);
        OPEN_CLOSE_bright = findViewById(R.id.light_bright_spinner);
        save_btn = findViewById(R.id.light_config_save);
        bright_selected = "";
        dark_selected = "";
        done_bright = false;
        done_dark = false;
        fireStore = new FireStore();
        connectTask = new ConnectTask();

        //Initialize Spinner
        spinner_array_adapter = new ArrayAdapter<>(
                light_config.this, R.layout.custom_spinner, getResources().getStringArray(R.array.OPEN_CLOSE)
        );
        spinner_array_adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        OPEN_CLOSE_dark.setAdapter(spinner_array_adapter);
        OPEN_CLOSE_bright.setAdapter(spinner_array_adapter);

        //Get Data from Firestore
        Get_Data();

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bright_selected = OPEN_CLOSE_bright.getItemAtPosition(OPEN_CLOSE_bright.getSelectedItemPosition()).toString();
                dark_selected = OPEN_CLOSE_dark.getItemAtPosition(OPEN_CLOSE_dark.getSelectedItemPosition()).toString();
                fireStore.update_data("Bright", bright_selected);
                fireStore.update_data("Dark", dark_selected);
                if(TCP_stuff != null){
                    send_config_data();
                }
            }

        });
    }

    public void send_config_data(){
        Runnable send_data = new Runnable() {
            @Override
            public void run() {

                Log.d("MSG", "Sending Bright condition");
                TCP_stuff.sendMessage("BRIGHT/"+bright_selected+"\r\n");
                while(!done_bright);
                try{
                    Thread.sleep(800);
                }
                catch (Exception e){
                    Log.e("Error", "Error",e);
                }
                Log.d("MSG", "Sending Dark condition");
                TCP_stuff.sendMessage("DARK/"+dark_selected+"\r\n");
                while (!done_dark);
                Log.d("MSG", "DONE");

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(light_config.this, "Successfully Saved", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        };
        Thread thread = new Thread(send_data);
        thread.start();
    }

    @Override
    public void onBackPressed(){
        Log.d("MSG", "Going back");
        TCP_stuff.stopClient();
        connectTask.cancel(true);
        Intent i = new Intent(light_config.this, main.class);
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
                        OPEN_CLOSE_bright.setSelection(spinner_array_adapter.getPosition(document_data.Bright));
                        OPEN_CLOSE_dark.setSelection(spinner_array_adapter.getPosition(document_data.Dark));
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
            if (!TCP_stuff.run()){
                //Show error message
                runOnUiThread(new Runnable() {
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(light_config.this);
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
            if (message.contains("BRI_OK")){
                done_bright = true;
                Log.d("MSG", "DONE bright");
            }
            else if (message.contains("DAR_OK")){
                done_dark = true;
                Log.d("MSG", "DONE dark");
            }
        }
    }
}
