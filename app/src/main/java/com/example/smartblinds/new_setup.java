package com.example.smartblinds;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class new_setup extends AppCompatActivity {

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private ListView blists;
    private ArrayList<String> arrayList = new ArrayList<>();
    private String access_point_name;
    private boolean success;
    private String device_flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smart_blinds_scan);

        device_flag = getIntent().getStringExtra("DeviceConnected");

        final View next_btn_view = findViewById(R.id.scan_next_btn);
        final View title_view = findViewById(R.id.scan_title_txt);
        final View title_view2 = findViewById(R.id.scan_title_txt2);
        Button scan_btn = findViewById(R.id.scan_scan_btn);
        Button next_btn = findViewById(R.id.scan_next_btn);
        blists = findViewById(R.id.blinds_list);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();

        if (device_flag.contentEquals("True")){
            title_view.setVisibility(View.GONE);
            title_view2.setVisibility(View.VISIBLE);
        }
        else if (device_flag.contentEquals("False")){
            title_view.setVisibility(View.VISIBLE);
            title_view2.setVisibility(View.GONE);
        }


        next_btn_view.setVisibility(View.GONE);

        success = wifiManager.startScan();
        if (success){
            Toast.makeText(new_setup.this, "Scanning...", Toast.LENGTH_SHORT).show();
        }

        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                arrayList.clear();
                success = wifiManager.startScan();
                if (success){
                    Toast.makeText(new_setup.this, "Scanning...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        blists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                access_point_name = ((TextView) view).getText().toString();
                Toast.makeText(new_setup.this, "Selected: " + access_point_name, Toast.LENGTH_SHORT).show();
                next_btn_view.setVisibility(View.VISIBLE);
            }
        });

        next_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (device_flag.contentEquals("False")){
                    Intent myIntent = new Intent(view.getContext(), new_setup_2.class);
                    myIntent.putExtra("DeviceName", access_point_name);
                    startActivityForResult(myIntent, 0);
                }
                else if (device_flag.contentEquals("True")){
                    Intent myIntent = new Intent(view.getContext(), new_setup_2.class);
                    myIntent.putExtra("DeviceName", access_point_name);
                    startActivityForResult(myIntent, 0);
                }
            }
        });
    }

    protected void onPause(){
        unregisterReceiver(wifiScanReceiver);
        super.onPause();
    }

    protected void onResume(){
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiScanReceiver extends BroadcastReceiver{
        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent){
            List<ScanResult> blind_list = wifiManager.getScanResults();
            ArrayAdapter arrayAdapter = new ArrayAdapter(c, android.R.layout.simple_list_item_1, arrayList);
            blists.setAdapter(arrayAdapter);

            for(ScanResult scanResult : blind_list){

                if (device_flag.contentEquals("False")){
                    if (scanResult.SSID.contains("SmartBlinds")){
                        arrayList.add(scanResult.SSID);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
                else if (device_flag.contentEquals("True")){
                    if (!scanResult.SSID.contains("SmartBlinds") && !scanResult.SSID.isEmpty()){
                        arrayList.add(scanResult.SSID);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }
}
