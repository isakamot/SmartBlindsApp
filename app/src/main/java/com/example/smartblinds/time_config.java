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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class time_config extends AppCompatActivity {
    Spinner AM_PM_open, AM_PM_close;
    FireStore fireStore;
    Data document_data;
    String device_IP, message;
    String open_hr_str, open_min_str, close_hr_str, close_min_str;
    String op_hr, op_min, cl_hr, cl_min;
    String open_am_pm_str, close_am_pm_str;
    String am_pm_open_str, am_pm_close_str;
    Button save_btn;
    TextView open_hr, close_hr, open_min, close_min;
    tcp TCP_stuff;
    ConnectTask connectTask;
    ArrayAdapter<String> spinner_array_adapter;
    Boolean done_open, done_close;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time);

        AM_PM_open = findViewById(R.id.time_open_spiner);
        AM_PM_close = findViewById(R.id.time_close_spinner);
        save_btn = findViewById(R.id.time_config_save);
        open_hr = findViewById(R.id.time_open_hr);
        open_min = findViewById(R.id.time_open_min);
        close_hr = findViewById(R.id.time_close_hr);
        close_min = findViewById(R.id.time_close_min);
        fireStore = new FireStore();
        connectTask = new ConnectTask();
        done_close = false;
        done_open = false;

        //Initialize Spinner
        spinner_array_adapter = new ArrayAdapter<>(
                time_config.this, R.layout.custom_spinner, getResources().getStringArray(R.array.AM_PM)
        );
        spinner_array_adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        AM_PM_close.setAdapter(spinner_array_adapter);
        AM_PM_open.setAdapter(spinner_array_adapter);

        //Get Data from Firestore
        Get_Data();

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get data from user
                am_pm_open_str = AM_PM_open.getItemAtPosition(AM_PM_open.getSelectedItemPosition()).toString();
                am_pm_close_str = AM_PM_close.getItemAtPosition(AM_PM_close.getSelectedItemPosition()).toString();
                op_hr = open_hr.getText().toString();
                op_min = open_min.getText().toString();
                cl_hr = close_hr.getText().toString();
                cl_min = close_min.getText().toString();

                if (Integer.valueOf(op_hr) > 12 || Integer.valueOf(op_min) > 59 || Integer.valueOf(cl_hr) > 12 || Integer.valueOf(cl_min) > 59){
                    Toast.makeText(time_config.this, "Invalid Time Entered", Toast.LENGTH_LONG).show();
                }

                else{
                    //Save to firestore
                    if (op_hr.length() == 1){
                        op_hr = "0"+op_hr;
                    }
                    if (op_min.length() == 1){
                        op_min = "0"+op_min;
                    }
                    if (cl_hr.length() == 1){
                        cl_hr = "0"+cl_hr;
                    }
                    if (cl_min.length() == 1){
                        cl_min = "0"+cl_min;
                    }
                    fireStore.update_data("Time_Open",op_hr+":"+op_min+am_pm_open_str);
                    fireStore.update_data("Time_Close", cl_hr+":"+cl_min+am_pm_close_str);

                    //Send to blinds
                    if(TCP_stuff != null) {
                        send_config_data();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        Log.d("MSG", "Going back");
        TCP_stuff.stopClient();
        connectTask.cancel(true);
        Intent i = new Intent(time_config.this, main.class);
        startActivity(i);
    }

    public void send_config_data(){
        Runnable send_data = new Runnable() {
            @Override
            public void run() {

                Log.d("MSG", "Sending Open time");
                TCP_stuff.sendMessage("TM_OP/"+op_hr+":"+op_min+am_pm_open_str+"\r\n");
                Log.d("MSG", "Done sending");
                while(!done_open);
                try{
                    Thread.sleep(800);
                }
                catch (Exception e){
                    Log.e("Error", "Error",e);
                }
                Log.d("MSG", "Sending Closing condition");
                TCP_stuff.sendMessage("TM_CL/"+cl_hr+":"+cl_min+am_pm_close_str+"\r\n");
                while (!done_close);
                Log.d("MSG", "DONE");

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(time_config.this, "Successfully Saved", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        Thread thread = new Thread(send_data);
        thread.start();
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

                        //Configure AM or PM
                        open_am_pm_str = document_data.Time_Open.substring(Math.max(document_data.Time_Open.length() - 2, 0));
                        close_am_pm_str = document_data.Time_Close.substring(Math.max(document_data.Time_Close.length() - 2, 0));
                        AM_PM_open.setSelection(spinner_array_adapter.getPosition(open_am_pm_str));
                        AM_PM_close.setSelection(spinner_array_adapter.getPosition(close_am_pm_str));

                        //Configure Time
                        String open_time = document_data.Time_Open.substring(0, document_data.Time_Open.length() - 2);
                        String close_time = document_data.Time_Close.substring(0, document_data.Time_Close.length() - 2);
                        Log.d("CLOSE time", close_time);
                        Log.d("OPEN time", open_time);
                        open_hr_str = open_time.split(":")[0];
                        open_min_str = open_time.split(":")[1];
                        close_hr_str = close_time.split(":")[0];
                        close_min_str = close_time.split(":")[0];
                        open_hr.setText(open_hr_str);
                        open_min.setText(open_min_str);
                        close_hr.setText(close_hr_str);
                        close_min.setText(close_min_str);

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
            //Show error message
            runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(time_config.this);
                    builder.setMessage("Connection Failed Please Restart App");
                    builder.setTitle("Error");
                    builder.create().show();
                }
            });
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            message = values[0];
            if (message.contains("OP_OK")){
                done_open = true;
                Log.d("MSG", "DONE open");
            }
            else if (message.contains("CL_OK")){
                done_close = true;
                Log.d("MSG", "DONE close");
            }
        }
    }

}
