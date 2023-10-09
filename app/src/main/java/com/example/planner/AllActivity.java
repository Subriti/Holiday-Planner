package com.example.planner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.core.Tag;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllActivity extends AppCompatActivity {
    FirebaseFirestore db;

    RecyclerView recyclerView;
    Adapter adapter;
    List<Model> modelList;

    private ProgressBar progressBar;
    TextView currentUser, logout, noItem;
    AppSharedPreferences sharedPreferences;
    FloatingActionButton floatingActionButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);

        db =FirebaseFirestore.getInstance();
        recyclerView=findViewById(R.id.rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        sharedPreferences = new AppSharedPreferences(this);

        floatingActionButton=findViewById(R.id.fab);
        progressBar = findViewById(R.id.progressBar);
        currentUser= findViewById(R.id.currentUser);
        logout= findViewById(R.id.logout);
        noItem= findViewById(R.id.noItem);

        //noItem.setVisibility(View.INVISIBLE);

        //currentUser.setText(sharedPreferences.getUsername() + "'s Holiday Requirements");
        currentUser.setText("Requirements");

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AllActivity.this, MainActivity.class));
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferences.setLoggedIn(false);
                sharedPreferences.setUserPhone("");
                sharedPreferences.setUsername("");
                sharedPreferences.setUserID("");
                sharedPreferences.setEmail("");
                startActivity(new Intent(AllActivity.this, SplashScreen.class));
                finish();
            }
        });

        modelList = new ArrayList<>();
        adapter = new Adapter(getApplicationContext(),modelList);
        recyclerView.setAdapter(adapter);

        loadFirestoreData();
    }

    private void loadFirestoreData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("user data")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int documentCount = task.getResult().size();
                            System.out.println( "Number of documents: " + documentCount);

                            modelList.clear();

                            for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                Model workmodel = documentSnapshot.toObject(Model.class);
                                if (workmodel != null) {
                                    System.out.println("Document content: " + workmodel.toString());
                                    if (workmodel.userId.equals(sharedPreferences.getUserID())) {
                                        modelList.add(workmodel);
                                    }
                                }
                            }

                            adapter.notifyDataSetChanged();
                            noItem.setVisibility(View.INVISIBLE);
                            progressBar.setVisibility(View.GONE);

                            if (modelList.size() == 0) {
                                noItem.setVisibility(View.VISIBLE);
                            } else {
                                noItem.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            System.out.println("Error getting documents: " + task.getException());
                        }
                    }
                });
    }
}