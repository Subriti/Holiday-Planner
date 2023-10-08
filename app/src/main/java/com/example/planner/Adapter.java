package com.example.planner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    Context context;
    List<Model> modelList;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private Geocoder geocoder;

    public Adapter(Context context, List<Model> modelList) {
        this.context = context;
        this.modelList = modelList;
        this.sharedPreferences = context.getSharedPreferences("edit", Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        geocoder = new Geocoder(context);
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        GeoPoint geoPoint = modelList.get(position).getLocation();
        holder.name.setText(modelList.get(position).getName());
        holder.quntity.setText(modelList.get(position).getQuantity());
        holder.price.setText(" Rs." + modelList.get(position).getPrice());
        holder.description.setText(modelList.get(position).getDescription());

        holder.mapView.getMapAsync(googleMap -> {
            LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(location).title("Marker"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
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

                System.out.println(modelList.get(position).getName());
                System.out.println(modelList.get(position).getDescription());
                System.out.println(modelList.get(position).getQuantity());
                System.out.println(modelList.get(position).getPrice());

                //passing values to the edit Screen
                Model newModel = new Model(modelList.get(position).getName(), modelList.get(position).getQuantity(), modelList.get(position).getPrice(), modelList.get(position).getDescription(), modelList.get(position).getItemImg());
                intent.putExtra("editModel", newModel);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String doc = modelList.get(position).getDocId();

                db.collection("user data").whereEqualTo("docId", doc)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                WriteBatch batch = FirebaseFirestore.getInstance().batch();
                                List<DocumentSnapshot> doc = queryDocumentSnapshots.getDocuments();
                                for (DocumentSnapshot snapshot : doc) {
                                    batch.delete(snapshot.getReference());
                                }
                                batch.commit()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                modelList.remove(position);
                                                Toast.makeText(context, "Deleted ", Toast.LENGTH_SHORT).show();

                                                Intent intent = new Intent(context, AllActivity.class);
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

        /*holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), DetailActivity.class);
                System.out.println(modelList.get(position).getName());
                System.out.println(modelList.get(position).getDescription());
                System.out.println(modelList.get(position).getQuantity());
                System.out.println(modelList.get(position).getPrice());

                //passing values to the edit Screen
                Model newModel= new Model(modelList.get(position).getName(), modelList.get(position).getQuantity(), modelList.get(position).getPrice(), modelList.get(position).getDescription());
                intent.putExtra("viewModel", newModel);
                v.getContext().startActivity(intent);
            }
        });*/
    }

    private void openWhatsApp(String name, String quantity, String price, String desc, String location, String itemImg) {
        try {
            //Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" +"Project Name :- "+ nam );
            Uri uri = Uri.parse("https://api.whatsapp.com/send?text=" + "Item Name: " + name + "\nQuantity: " + quantity + "\nPrice: Rs." + price + "\n Description: " + desc + "\nGeoPoint: " + location + "\nImage: " + itemImg);
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        MapView mapView;
        TextView name, quntity, price, description;
        Button edit, delete, share;

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

        }
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
}
