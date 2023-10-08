package com.example.planner;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

public class Model implements Serializable {
    String description;
    String docId;
    String quantity;
    GeoPoint location;
    String name;
    String price;
    Timestamp timestamp;
    String userId;
    String itemImg;

    public Model() {
    }

    public Model(String userId, String description, String docId, String quantity, GeoPoint location, String name, String price, Timestamp timestamp, String itemImg) {
        this.userId=userId;
        this.description = description;
        this.docId = docId;
        this.quantity = quantity;
        this.location = location;
        this.name = name;
        this.price = price;
        this.timestamp = timestamp;
        this.itemImg= itemImg;
    }

    public Model(String userId, String description, String docId, String quantity, GeoPoint location, String name, String price, Timestamp timestamp) {
        this.userId=userId;
        this.description = description;
        this.docId = docId;
        this.quantity = quantity;
        this.location = location;
        this.name = name;
        this.price = price;
        this.timestamp = timestamp;
    }

    public Model(String name, String quantity, String price, String description, String itemImg) {
        this.description = description;
        this.quantity = quantity;
        this.name = name;
        this.price = price;
        this.itemImg=itemImg;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {this.userId = userId;}
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getItemImg() {
        return itemImg;
    }

    public void setItemImg(String itemImg) {
        this.itemImg = itemImg;
    }
}
