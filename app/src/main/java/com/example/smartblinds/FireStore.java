package com.example.smartblinds;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FireStore extends Activity {
    FirebaseFirestore fb = FirebaseFirestore.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    String email = user.getEmail();
    Map<String, Object> data = new HashMap<>();
    Data document_data;

    public void init(){
       data.put("Email", email);
       data.put("Temp_Close", "0");
       data.put("Temp_Open", "0");
       data.put("Bright", "CLOSE");
       data.put("Dark", "CLOSE");
       data.put("Time_Open", "12:00AM");
       data.put("Time_Close", "11:59PM");
       data.put("CUR_Temp", "--");
       data.put("CUR_Pos", "--");
       data.put("CUR_Bat", "--");

       fb.collection("users").document(email)
               .set(data)
               .addOnSuccessListener(new OnSuccessListener<Void>(){
                   @Override
                   public void onSuccess(Void aVoid){
                       Log.d("MSG", "Successfully saved");
                   }
               })
               .addOnFailureListener(new OnFailureListener() {
                   @Override
                   public void onFailure(@NonNull Exception e) {
                       Log.w("MSG", "Error adding document");
                   }
               });
    }

    public void get_data(){
        DocumentReference documentReference = fb.collection("users").document(email);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task){
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        Log.d("Data", document.getData().toString());
                        document_data = document.toObject(Data.class);
                        Log.d("Success", "Success");
                    }
                    else{
                        Log.d("Creating", "Creating");
                        init();
                    }
                }
                else{
                    Log.d("Fail", "Fail");
                }
            }
        });
    }

    public Data get_data_to_app(){
        return document_data;
    }

    public void update_data(String Key, String Value){
        DocumentReference documentReference = fb.collection("users").document(email);
        documentReference
                .update(Key,Value)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Success", "DocumentSnapshot successfully updated!");

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Error", "Error updating document", e);
                    }
                });
    }
}
