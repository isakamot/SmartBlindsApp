package com.example.smartblinds;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class sign_up extends AppCompatActivity {

    EditText email, password;
    Button sign_up_btn;
    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        firebaseAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.signup_email_txt);
        password = findViewById(R.id.signup_password_txt);
        sign_up_btn = findViewById(R.id.sign_up_btn);

        sign_up_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                final String email_str = email.getText().toString();
                final String password_str = password.getText().toString();
                if (email_str.isEmpty()) {
                    email.setError("Provide your email first");
                    email.requestFocus();
                }
                else if (password_str.isEmpty()){
                    password.setError("Set your password");
                    password.requestFocus();
                }
                else if (password_str.isEmpty() && email_str.isEmpty()){
                    Toast.makeText(sign_up.this, "Fields Empty!", Toast.LENGTH_SHORT).show();
                }
                else if (!(password_str.isEmpty() && email_str.isEmpty())){
                    firebaseAuth.createUserWithEmailAndPassword(email_str, password_str).addOnCompleteListener(sign_up.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("user", email_str);
                            Log.d("pass", password_str);
                            if(!task.isSuccessful()){
                                Toast.makeText(sign_up.this.getApplicationContext(), "Sign up unsucessful:" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Intent myIntent = new Intent(sign_up.this, new_setup.class);
                                myIntent.putExtra("DeviceConnected", "False");
                                startActivity(myIntent);
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(sign_up.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
