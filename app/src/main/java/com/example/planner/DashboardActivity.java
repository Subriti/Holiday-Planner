package com.example.planner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    FirebaseFirestore db;
    RecyclerView recyclerView;
    Adapter adapter;
    List<Model> modelList;
    TextView currentUser, logout, noItem, username, useremail, userphone;
    AppSharedPreferences sharedPreferences;
    FloatingActionButton floatingActionButton;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        sharedPreferences = new AppSharedPreferences(this);

        floatingActionButton = findViewById(R.id.fab);
        progressBar = findViewById(R.id.progressBar);
        currentUser = findViewById(R.id.currentUser);
        logout = findViewById(R.id.logout);
        noItem = findViewById(R.id.noItem);
        username = findViewById(R.id.username);
        useremail = findViewById(R.id.useremail);
        userphone = findViewById(R.id.userphone);

        username.setText(sharedPreferences.getUsername());
        useremail.setText("Email: " + sharedPreferences.getEmail());
        userphone.setText("Phone: " + sharedPreferences.getUserPhone());

        currentUser.setText("Requirements");

        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open, R.string.close);

        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        // Set a click listener for the menu button
        ImageView menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSidebar();
            }
        });

        // Set a click listener for the close button in the sidebar
        ImageView closeSidebarButton = findViewById(R.id.close_sidebar_button);
        closeSidebarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSidebar();
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        modelList = new ArrayList<>();
        adapter = new Adapter(getApplicationContext(), modelList);
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
                            System.out.println("Number of documents: " + documentCount);

                            modelList.clear();

                            for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                Model workmodel = documentSnapshot.toObject(Model.class);
                                if (workmodel != null) {
                                    System.out.println("Document content: " + workmodel);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    private void toggleSidebar() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void logout() {
        sharedPreferences.setLoggedIn(false);
        sharedPreferences.setUserPhone("");
        sharedPreferences.setUsername("");
        sharedPreferences.setUserID("");
        sharedPreferences.setEmail("");
        startActivity(new Intent(DashboardActivity.this, SplashScreen.class));
        finish();
    }
}