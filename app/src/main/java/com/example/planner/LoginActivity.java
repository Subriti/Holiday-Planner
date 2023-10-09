package com.example.planner;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    EditText mEmail, mPassword;
    Button mLoginBtn;
    FirebaseAuth fAuth;
    FirebaseDatabase database;
    ProgressBar loginSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mEmail = findViewById(R.id.remail);
        mPassword = findViewById(R.id.rpassword);
        mLoginBtn = findViewById(R.id.rlogin);

        loginSpinner = findViewById(R.id.loginprogress);
        loginSpinner.setVisibility(View.INVISIBLE);

        AppSharedPreferences sharedPreferences = new AppSharedPreferences(this);

        FirebaseApp.initializeApp(LoginActivity.this);
        fAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableSpinner(true);
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    mEmail.setError("Email is Required.");
                    enableSpinner(false);
                    return;
                }
                else {
                    if (!validEmail(email)) {
                        mEmail.setError("Valid-Email is Required.");
                        Toast.makeText(LoginActivity.this, "Please enter a valid e-mail!", Toast.LENGTH_LONG).show();
                        enableSpinner(false);
                    }
                }

                if (TextUtils.isEmpty(password)) {
                    mPassword.setError("Password is Required.");
                    enableSpinner(false);
                    return;
                }

                if (password.length() < 6) {
                    mPassword.setError("Password Must be >=6 Characters");
                    enableSpinner(false);
                    return;
                }

                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            database.getReference().child("Usersregister").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    UserModel user = snapshot.getValue(UserModel.class);
                                    //int usertype = snapshot.getValue(Integer.class);
                                    int usertype = user.getUsertype();
                                    if (usertype == 0) {
                                        sharedPreferences.setEmail(email);
                                        sharedPreferences.setUserID(uid);
                                        sharedPreferences.setUsername(user.getName());
                                        sharedPreferences.setUserPhone(user.getPhone());
                                        sharedPreferences.setLoggedIn(true);

                                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    if (usertype == 1) {
                                       /* Intent intent = new Intent(Login_Page.this, Admin_Page.class);
                                        startActivity(intent);
                                        finish();*/
                                        Toast.makeText(LoginActivity.this, "Enter Service Email", Toast.LENGTH_SHORT).show();
                                    }
                                    enableSpinner(false);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    enableSpinner(false);
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                        enableSpinner(false);
                    }
                });
            }
        });
    }

    public boolean validEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void enableSpinner(boolean enable) {
        if (enable) {
            loginSpinner.setVisibility(View.VISIBLE);
        } else {
            loginSpinner.setVisibility(View.INVISIBLE);
        }
        mLoginBtn.setEnabled(!enable);
    }

}