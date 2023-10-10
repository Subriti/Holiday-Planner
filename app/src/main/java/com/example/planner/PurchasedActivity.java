package com.example.planner;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class PurchasedActivity extends AppCompatActivity {
    FirebaseFirestore db;
    RecyclerView recyclerView;
    PurchaseAdapter adapter;
    List<Model> modelList;
    private ProgressBar progressBar;
    TextView noItem, backBtn;
    AppSharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchased);

        db =FirebaseFirestore.getInstance();
        recyclerView=findViewById(R.id.rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        sharedPreferences = new AppSharedPreferences(this);

        progressBar = findViewById(R.id.progressBar);
        backBtn= findViewById(R.id.purchaseBack);
        noItem= findViewById(R.id.noItem);

        modelList = new ArrayList<>();
        adapter = new PurchaseAdapter(getApplicationContext(),modelList);
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadFirestoreData();
    }

    private void loadFirestoreData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("purchase data")
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