package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class Contract {
    @SerializedName("_id")          public String id;
    @SerializedName("room")         public Room room;
    @SerializedName("startDate")    public String startDate;
    @SerializedName("endDate")      public String endDate;
    @SerializedName("deposit")      public double deposit;
    @SerializedName("monthlyRent")  public double monthlyRent;
    @SerializedName("status")       public String status;
    @SerializedName("terms")        public String terms;
    @SerializedName("signedDate")   public String signedDate;
}