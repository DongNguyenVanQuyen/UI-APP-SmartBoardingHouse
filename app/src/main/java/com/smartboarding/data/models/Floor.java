package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Floor {
    @SerializedName("_id")          public String id;
    @SerializedName("name")         public String name;
    @SerializedName("floorNumber")  public int floorNumber;
}