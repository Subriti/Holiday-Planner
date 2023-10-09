package com.example.planner;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;
    EditText mFullName, mFoodItem, mDescription, mPhone;
    Button mSubmitBtn, btnGallery;
    TextView textView;
    FirebaseFirestore fStore;
    AppSharedPreferences sharedPreferences;
    int galleryRequestCode = 1000;
    FirebaseStorage storage;
    StorageReference storageReference;
    ProgressBar progressBar;
    Uri filePath = null;
    ImageView img;
    String downloadURL = "";
    private GoogleMap mMap;
    private final int REQUEST_CODE = 11;
    double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFullName = findViewById(R.id.donorname);
        mFoodItem = findViewById(R.id.fooditem);
        mPhone = findViewById(R.id.phone);
        mDescription = findViewById(R.id.description);
        mSubmitBtn = findViewById(R.id.submit);
        textView = findViewById(R.id.back);
        btnGallery = findViewById(R.id.btnGallery);
        img = findViewById(R.id.picture_to_be_posted);
        progressBar = findViewById(R.id.progressBar2);
        //mMap= findViewById(R.id.google_map);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select picture to post"), galleryRequestCode);
            }
        });

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select picture to post"), galleryRequestCode);
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        sharedPreferences = new AppSharedPreferences(this);

        // Retrieve the Model object from the intent extras
        Model model = (Model) getIntent().getSerializableExtra("editModel");
        System.out.println("Model is: " + model);
        if (model != null) {
            mFullName.setText(model.name);
            mFoodItem.setText(model.quantity);
            mPhone.setText(model.price);
            mDescription.setText(model.description);

            //set map to the saved data location
            latitude = model.latitude;
            longitude = model.longitude;

            downloadURL = model.itemImg;
            System.out.println("Img is: " + model.itemImg);
            if (model.itemImg != null) {
                //getting picture
                Glide.with(this).load(model.itemImg).into(img);
                //hide add button
                btnGallery.setVisibility(View.INVISIBLE);
            }
            //model needs editing
            mSubmitBtn.setText("Update Item");
        }
        else {
            mSubmitBtn.setText("Add Item");
        }

        fStore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mapFragment.getMapAsync(this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        //during edit, apply previously stored location
        if (latitude != 0.0 && longitude != 0.0) {
            LatLng latLng = new LatLng(latitude, longitude);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mLastLocation = new Location("");
            mLastLocation.setLatitude(latitude);
            mLastLocation.setLongitude(longitude);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear(); // Clear existing markers
                mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
                mLastLocation = new Location(""); // Create a new Location object
                mLastLocation.setLatitude(latLng.latitude); // Set latitude of the selected location
                mLastLocation.setLongitude(latLng.longitude); // Set longitude of the selected location
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Selected Location");
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        mMap.addMarker(markerOptions).showInfoWindow();

        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableSpinner(true);
                String fullname = mFullName.getText().toString().trim();
                String fooditem = mFoodItem.getText().toString().trim();
                String description = mDescription.getText().toString().trim();
                String phone = mPhone.getText().toString().trim();

                String uID = sharedPreferences.getUserID();

                if (TextUtils.isEmpty(fullname)) {
                    mFullName.setError("Name is required.");
                    enableSpinner(false);
                    return;
                }

                if (TextUtils.isEmpty(fooditem)) {
                    mFoodItem.setError("Quantity is required.");
                    enableSpinner(false);
                    return;
                }

                //new image selected
                if (filePath != null) {
                    //first upload image to firebase get download URL
                    uploadImage(uID, fullname, fooditem, description, phone);
                }
                // no change in image: change details only
                else {
                    saveDetails(uID, fullname, fooditem, description, phone);
                }
            }
        });
    }

    private void saveDetails(String uID, String fullname, String fooditem, String description, String phone) {

        CollectionReference collectionReference = fStore.collection("user data");

        GeoPoint geoPoint = new GeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        Map<String, Object> user = new HashMap<>();
        user.put("timestamp", FieldValue.serverTimestamp());
        user.put("name", fullname);
        user.put("quantity", fooditem);
        user.put("price", phone);
        user.put("description", description);
        user.put("location", geoPoint);
        //associating user with each item list
        user.put("userId", uID);
        user.put("itemImg", downloadURL);

        if (mSubmitBtn.getText() == "Add Item") {
            String uuid = UUID.randomUUID().toString(); // Generate a random UUID
            user.put("docId", uuid); // Set the generated UUID as the document ID

            collectionReference.document(uuid).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Success!");
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            enableSpinner(false);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error!", e);
                            enableSpinner(false);
                        }
                    });
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences("edit", MODE_PRIVATE);

            // Retrieve data from SharedPreferences
            String doc = sharedPreferences.getString("docId", "");
            collectionReference.document(doc).update(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Success!");
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            enableSpinner(false);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error!", e);
                            enableSpinner(false);
                        }
                    });
        }
    }

    private void uploadImage(String uID, String fullname, String fooditem, String description, String phone) {
        if (filePath != null) {
            // Defining the child of storageReference
            StorageReference ref = storageReference.child(
                    "images/" + UUID.randomUUID().toString()
            );

            // Adding listeners on upload or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Image uploaded successfully
                            Toast.makeText(
                                    MainActivity.this, "Image Uploaded!!", Toast.LENGTH_SHORT
                            ).show();

                            Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                            downloadUrl.addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    downloadURL = "https://" + task.getResult().getEncodedAuthority() + task.getResult().getEncodedPath() + "?alt=media&token=" + task.getResult().getQueryParameters("token").get(0);

                                    // Save the downloadURL to the database
                                    System.out.println("Final download URL: " + downloadURL);
                                    saveDetails(uID, fullname, fooditem, description, phone);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Error, Image not uploaded
                            //progressDialog.dismiss();
                            Toast.makeText(
                                    MainActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT
                            ).show();
                            enableSpinner(false);
                        }
                    });
        } else {
            Toast.makeText(
                    MainActivity.this, "Please select a picture to proceed.", Toast.LENGTH_SHORT
            ).show();
            enableSpinner(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == galleryRequestCode) {
                // For gallery
                if (data != null) {
                    // img.setImageURI(data.getData());
                    filePath = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                MainActivity.this.getContentResolver(), filePath
                        );
                        img.setImageBitmap(bitmap);
                        btnGallery.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new com.google.android.gms.location.LocationRequest();
        mLocationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableSpinner(boolean enable) {
        if (enable) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
        mSubmitBtn.setEnabled(!enable);
    }
}
