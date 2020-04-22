package com.example.smartblinds;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class tcp{

    private String device_ip;
    Socket socket;
    PrintWriter printWriter;
    BufferedReader bufferedReader;
    private OnMessageReceived mMessageListener = null;
    String mServerMessage;
    boolean mRun;
    String msg;
    tcp TCP_stuff;

    public void set_ip (String ip){
        device_ip = ip;
    }


    public tcp(OnMessageReceived listener){
        mMessageListener = listener;
    }

    public void sendMessage(final String message){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (printWriter != null){
                    printWriter.println(message);
                    printWriter.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopClient(){
        mRun = false;
        if (printWriter != null){
            printWriter.flush();
            printWriter.close();
        }
        mMessageListener = null;
        bufferedReader = null;
        printWriter = null;
        mServerMessage = null;
    }

    public void run() {
        mRun = true;
        try {
            InetAddress serverAddr = InetAddress.getByName(device_ip);
            Log.d("TCP Client", "C: Connecting...");
            Socket socket = new Socket(serverAddr, 80);
            try {
                printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (mRun) {
                    mServerMessage = bufferedReader.readLine();
                    if (mServerMessage != null && mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessage);
                    }
                }
            } catch (Exception e) {
                Log.e("TCP", "Error", e);
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            Log.e("TCP", "Error", e);
        }
    }

    public interface OnMessageReceived{
        public void messageReceived(String message);
    }

}
