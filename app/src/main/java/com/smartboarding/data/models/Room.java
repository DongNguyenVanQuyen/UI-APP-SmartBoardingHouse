package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class Room {
    @SerializedName("_id")          public String id;
    @SerializedName("roomNumber")   public String roomNumber;
    @SerializedName("floor")        public Floor floor;
    @SerializedName("price")        public double price;
    @SerializedName("area")         public double area;
    @SerializedName("status")       public String status;
}