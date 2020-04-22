package com.example.smartblinds;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseUser;

public class signin extends AppCompatActivity {
    EditText email, password;
    Button login_btn;
    FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        firebaseAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.Login_email_txt);
        password = findViewById(R.id.Login_password_txt);
        login_btn = findViewById(R.id.login_btn);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    Toast.makeText(signin.this, "User logged in", Toast.LENGTH_SHORT).show();
                    Intent I = new Intent(signin.this, main.class);
                    startActivity(I);
                }
                else{
                    Toast.makeText(signin.this, "Login to continue", Toast.LENGTH_SHORT).show();
                }
            }
        };

        login_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String user_string = email.getText().toString();
                String pass_string = password.getText().toString();
                if (user_string.isEmpty()){
                    email.setError("Provide your Email first");
                    email.requestFocus();
                }
                else if (pass_string.isEmpty()){
                    password.setError("Enter password");
                    password.requestFocus();
                }
                else if (user_string.isEmpty() && pass_string.isEmpty()) {
                    Toast.makeText(signin.this, "Fields Empty!", Toast.LENGTH_SHORT).show();
                }
                else if (!(user_string.isEmpty() && pass_string.isEmpty())) {
                    firebaseAuth.signInWithEmailAndPassword(user_string,pass_string).addOnCompleteListener(signin.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(signin.this, "Not sucessfull", Toast.LENGTH_SHORT).show();
                            } else {
                                startActivity(new Intent(signin.this, main.class));
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(signin.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
