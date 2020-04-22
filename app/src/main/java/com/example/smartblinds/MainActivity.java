package com.example.smartblinds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //For Existing User
        Button login_btn = findViewById(R.id.start_login_btn);
        login_btn.setOnClickListener(new View.OnClickListener(){
            public  void onClick(View view){
                Intent myIntent = new Intent(view.getContext(), signin.class);
                startActivityForResult(myIntent,0);
            }
        });

        //For New User
        Button sign_up_btn = findViewById(R.id.start_new_user_btn);
        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent2 = new Intent(view.getContext(), sign_up.class);
                startActivityForResult(myIntent2, 0);
            }
        });
    }
}
