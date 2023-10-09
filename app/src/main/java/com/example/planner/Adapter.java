package com.example.planner;

import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    Context context;
    List<Model> modelList;
    Model model;
    FirebaseStorage storage;
    StorageReference storageReference;
    Boolean expanded = false;
    CollectionReference collectionReference;
    private final FirebaseFirestore db;
    private final SharedPreferences sharedPreferences;
    private final Geocoder geocoder;
    private boolean isChecked;

    public Adapter(Context context, List<Model> modelList) {
        this.context = context;
        this.modelList = modelList;
        this.sharedPreferences = context.getSharedPreferences("edit", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        geocoder = new Geocoder(context);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        //expand approach
        model = modelList.get(position);

        GeoPoint geoPoint = modelList.get(position).getLocation();
        holder.name.setText(modelList.get(position).getName());
        holder.quntity.setText("Qty:  " + modelList.get(position).getQuantity());
        holder.price.setText("Price:  ₹" + modelList.get(position).getPrice());
        holder.description.setText(modelList.get(position).getDescription());
        // Load the image into the imgItem ImageView using Glide
        Glide.with(context).load(modelList.get(position).getItemImg()).placeholder(R.drawable.addtocart) // Optional: Placeholder image while loading
                .error(R.drawable.addtocart) // Optional: Image to display on error
                .into(holder.imgItem);

        holder.mapView.getMapAsync(googleMap -> {
            LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(location).title("Marker"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        });

        isChecked = modelList.get(position).getIsPurchased();

        checkPurchase(model, holder);

        // Check if the item is expanded
        if (expanded) {
            // Show additional information (description, price, quantity, map, buttons)
            holder.description.setVisibility(View.VISIBLE);
            holder.price.setVisibility(View.VISIBLE);
            holder.quntity.setVisibility(View.VISIBLE);
            holder.mapView.setVisibility(View.VISIBLE);
            holder.edit.setVisibility(View.VISIBLE);
            holder.delete.setVisibility(View.VISIBLE);
            holder.share.setVisibility(View.VISIBLE);
            holder.arrow.setVisibility(View.INVISIBLE);
            holder.checkBox.setChecked(isChecked);
        } else {
            // Hide additional information
            holder.description.setVisibility(View.GONE);
            holder.price.setVisibility(View.GONE);
            holder.quntity.setVisibility(View.GONE);
            holder.mapView.setVisibility(View.GONE);
            holder.edit.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
            holder.share.setVisibility(View.GONE);
            holder.arrow.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(isChecked);
        }

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isChecked = holder.checkBox.isChecked();
                modelList.get(position).setIsPurchased(isChecked);

                //editData(modelList.get(position), position);
                editData(position);
                //editData(model);
            }
        });

        // Set click listener to expand/collapse the item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle the expanded state
                model.setIsExpanded(!model.getIsExpanded());
                expanded = model.getIsExpanded();
                notifyItemChanged(position);
            }
        });

        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String doc = modelList.get(position).getDocId();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("docId", doc);
                // Replace "key" with a unique identifier for your data
                editor.apply();
                //Intent intent = new Intent(context, ViewAllActivity.class);
                Intent intent = new Intent(context, MainActivity.class);

                //passing values to the edit Screen
                Model newModel = new Model(modelList.get(position).getName(), modelList.get(position).getQuantity(), modelList.get(position).getPrice(), modelList.get(position).getDescription(), modelList.get(position).getItemImg(), modelList.get(position).location.getLatitude(), modelList.get(position).location.getLongitude());
                intent.putExtra("editModel", newModel);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String doc = modelList.get(position).getDocId();

                db.collection("user data").whereEqualTo("docId", doc).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        List<DocumentSnapshot> doc = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot snapshot : doc) {
                            batch.delete(snapshot.getReference());
                        }
                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                modelList.remove(position);
                                Toast.makeText(context, "Deleted ", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(context, DashboardActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            }
                        });
                    }
                });

            }
        });

        holder.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = modelList.get(position).getName();
                String quantity = modelList.get(position).getQuantity();
                String price = modelList.get(position).getPrice();
                String desc = modelList.get(position).getDescription();
                GeoPoint location = modelList.get(position).getLocation();
                String itemImg = modelList.get(position).getItemImg();

                System.out.println("Stored itmImg link: " + itemImg);
                String encodedUrl = encodeURL(itemImg);
                itemImg = encodedUrl;

                String placeName = getAddressFromLatLng(location.getLatitude(), location.getLongitude());
                openWhatsApp(name, quantity, price, desc, placeName, itemImg);
            }
        });
    }

    private void openWhatsApp(String name, String quantity, String price, String desc, String location, String itemImg) {
        try {
            Uri uri = Uri.parse("https://api.whatsapp.com/send?text=" + "Item Name: " + name + "\n\nQuantity: " + quantity + "\n\nPrice: Rs." + price + "\n\nDescription: " + desc + "\n\nGeoPoint: " + location + "\n\nImage: " + itemImg);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Handle exceptions (e.g., WhatsApp not installed)
            e.printStackTrace();
        }
    }

    public String getAddressFromLatLng(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Construct the address from individual components
                StringBuilder addressText = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressText.append(address.getAddressLine(i)).append(", ");
                }
                return addressText.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }

    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Handle the encoding exception
            e.printStackTrace();
            return url; // Return the original URL as a fallback
        }
    }

    public void checkPurchase(Model model, ViewHolder holder) {
        if (model.getIsPurchased() && isChecked) {
            holder.isPurchased.setText("Purchased");
            holder.isPurchased.setBackgroundColor(Color.GREEN);
            holder.checkBox.isChecked();
        } else {
            holder.isPurchased.setText("Not Purchased");
            holder.isPurchased.setBackgroundColor(Color.RED);
            holder.checkBox.setChecked(false);
        }
    }

    public void editData(Model model) {
        collectionReference = db.collection("user data");
        SharedPreferences sharedPreferences = context.getSharedPreferences("edit", MODE_PRIVATE);

        // Retrieve data from SharedPreferences
        String doc = sharedPreferences.getString("docId", "");

        // Create a new document with the updated purchase status
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("timestamp", FieldValue.serverTimestamp());
        updatedData.put("name", model.name);
        updatedData.put("quantity", model.quantity);
        updatedData.put("price", model.price);
        updatedData.put("description", model.description);
        updatedData.put("location", model.location);
        updatedData.put("userId", model.userId);
        updatedData.put("itemImg", model.itemImg);
        updatedData.put("isPurchased", model.isPurchased); // Updated purchase status

        // Add the new document to Firestore with a unique document ID
        collectionReference.add(updatedData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // New document added successfully, now delete the old one
                collectionReference.document(doc).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Item purchase details updated!", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged(); // Refresh the RecyclerView
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Failed to delete old item", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error deleting old document", e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Failed to add updated item", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding updated document", e);
            }
        });
    }

    public void editData(Model model, int position) {
        collectionReference = db.collection("user data");
        SharedPreferences sharedPreferences = context.getSharedPreferences("edit", MODE_PRIVATE);

        // Retrieve data from SharedPreferences
        String doc = sharedPreferences.getString("docId", "");

        Map<String, Object> updates = new HashMap<>();
        updates.put("timestamp", FieldValue.serverTimestamp());
        updates.put("name", model.name);
        updates.put("quantity", model.quantity);
        updates.put("price", model.price);
        updates.put("description", model.description);
        updates.put("location", model.location);
        updates.put("userId", model.userId);
        updates.put("itemImg", model.itemImg);
        updates.put("isPurchased", model.isPurchased); // Use the isPurchased property from the model

        // Update the item in the modelList
        for (int i = 0; i < modelList.size(); i++) {
            if (modelList.get(i).getDocId().equals(doc)) {
                // Update the properties of the item in the list
                modelList.get(i).setName(model.name);
                modelList.get(i).setQuantity(model.quantity);
                modelList.get(i).setPrice(model.price);
                modelList.get(i).setDescription(model.description);
                modelList.get(i).setLocation(model.location);
                modelList.get(i).setUserId(model.userId);
                modelList.get(i).setItemImg(model.itemImg);
                modelList.get(i).setIsPurchased(model.isPurchased);
                break; // Exit the loop once the item is found and updated
            }
        }

        // Notify the adapter of the data change
        notifyDataSetChanged();

        // Update the document in Firestore
        collectionReference.document(doc).update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(context, "Item purchase details updated!", Toast.LENGTH_SHORT).show();

                String doc = modelList.get(position).getDocId();

                db.collection("user data").whereEqualTo("docId", doc).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        List<DocumentSnapshot> doc = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot snapshot : doc) {
                            batch.delete(snapshot.getReference());
                        }
                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                modelList.remove(position);
                                notifyDataSetChanged();
                                //Toast.makeText(context, "Deleted old product item", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Failed to update item purchase details!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating document", e);
            }
        });
    }

    public void editData(int position) {
        collectionReference = db.collection("user data");
        SharedPreferences sharedPreferences = context.getSharedPreferences("edit", MODE_PRIVATE);

        // Retrieve data from SharedPreferences
        String doc = sharedPreferences.getString("docId", "");

        Map<String, Object> user = new HashMap<>();
        user.put("timestamp", FieldValue.serverTimestamp());
        user.put("name", model.name);
        user.put("quantity", model.quantity);
        user.put("price", model.price);
        user.put("description", model.description);
        user.put("location", model.location);
        //associating user with each item list
        user.put("userId", model.userId);
        user.put("itemImg", model.itemImg);
        user.put("isPurchased", isChecked);

        try{
            // Update the document in Firestore
            collectionReference.document(doc).update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Update the model in the list
                    modelList.get(position).setIsPurchased(isChecked); // Assuming you want to mark it as purchased
                    notifyDataSetChanged(); // Refresh the RecyclerView
                    Toast.makeText(context, "Item purchase details updated!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, "Failed to update item purchase details!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                }
            });
        }catch (Exception e){
            Toast.makeText(context, "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        MapView mapView;
        TextView name, quntity, price, description, arrow, isPurchased;
        ImageView edit, delete, share, imgItem;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mapView = itemView.findViewById(R.id.google_map);
            mapView.onCreate(null);
            mapView.onResume();
            name = itemView.findViewById(R.id.name);
            quntity = itemView.findViewById(R.id.quntity);
            price = itemView.findViewById(R.id.price);
            description = itemView.findViewById(R.id.des);
            edit = itemView.findViewById(R.id.edit);
            delete = itemView.findViewById(R.id.delete);
            share = itemView.findViewById(R.id.share);
            imgItem = itemView.findViewById(R.id.imgItem);
            arrow = itemView.findViewById(R.id.arrow);
            isPurchased = itemView.findViewById(R.id.isPurchased);
            checkBox = itemView.findViewById(R.id.checkbox);

            description.setVisibility(View.GONE);
            price.setVisibility(View.GONE);
            quntity.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            edit.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
            share.setVisibility(View.GONE);
        }
    }
}