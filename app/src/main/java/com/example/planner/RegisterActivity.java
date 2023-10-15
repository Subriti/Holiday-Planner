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
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {
    EditText mFullName, mEmail, mPassword, mPhone;
    Button mRegisterBtn;
    FirebaseAuth fAuth;
    FirebaseDatabase database;
    FirebaseFirestore fStore;
    ProgressBar registerSpinner;
    TextInputLayout nameError, emailError, phoneError, passError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mFullName = findViewById(R.id.rrname);
        mEmail = findViewById(R.id.rremail);
        mPassword = findViewById(R.id.rrpassword);
        mPhone = findViewById(R.id.rrphone);

        nameError = findViewById(R.id.nameError);
        phoneError = findViewById(R.id.phoneError);
        emailError = findViewById(R.id.emailError);
        passError = findViewById(R.id.passError);

        mRegisterBtn = findViewById(R.id.rrregister);

        registerSpinner = findViewById(R.id.registerprogress);
        registerSpinner.setVisibility(View.INVISIBLE);

        AppSharedPreferences sharedPreferences = new AppSharedPreferences(this);

        FirebaseApp.initializeApp(RegisterActivity.this);
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableSpinner(true);
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();
                String name = mFullName.getText().toString().trim();
                String phone = mPhone.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    nameError.setError("Name is Required.");
                    enableSpinner(false);
                    return;
                } else {
                    nameError.setError(null);
                }

                if (TextUtils.isEmpty(email)) {
                    emailError.setError("Email is Required.");
                    enableSpinner(false);
                    return;
                } else if (!validEmail(email)) {
                    emailError.setError("Valid-Email is Required.");
                    Toast.makeText(RegisterActivity.this, "Please enter a valid e-mail!", Toast.LENGTH_LONG).show();
                    enableSpinner(false);
                    return;
                } else {
                    emailError.setError(null);
                }

                if (TextUtils.isEmpty(phone)) {
                    phoneError.setError("Phone Number is required.");
                    enableSpinner(false);
                    return;
                } else if (phone.length() != 10) {
                    phoneError.setError("Phone number must have 10 digits");
                    enableSpinner(false);
                    return;
                } else {
                    phoneError.setError(null);
                }

                if (TextUtils.isEmpty(password)) {
                    passError.setError("Password is Required.");
                    enableSpinner(false);
                    return;
                } else if (password.length() < 6) {
                    passError.setError("Password Must be >=6 Characters");
                    enableSpinner(false);
                    return;
                } else {
                    passError.setError(null);
                }

                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            UserModel users = new UserModel(email, password, name, phone, uid, 0);
                            database.getReference().child("Usersregister").child(uid).setValue(users);
                            Toast.makeText(RegisterActivity.this, "Account Created", Toast.LENGTH_SHORT).show();

                            sharedPreferences.setEmail(email);
                            sharedPreferences.setUserID(uid);
                            sharedPreferences.setUsername(name);
                            sharedPreferences.setUserPhone(phone);
                            sharedPreferences.setLoggedIn(true);

                            //after registering, re-directing to dashboard
                            Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            enableSpinner(false);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Please fill in correct details", Toast.LENGTH_SHORT).show();
                            enableSpinner(false);
                        }
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
            registerSpinner.setVisibility(View.VISIBLE);
        } else {
            registerSpinner.setVisibility(View.INVISIBLE);
        }
        mRegisterBtn.setEnabled(!enable);
    }
}