package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class Contract {
    @SerializedName("_id")             public String id;
    @SerializedName("contractNumber")  public String contractNumber;
    @SerializedName("room")            public Room room;
    @SerializedName("tenantName")      public String tenantName;
    @SerializedName("roomNumber")      public String roomNumber;
    @SerializedName("startDate")       public String startDate;
    @SerializedName("endDate")         public String endDate;
    @SerializedName("paymentDate")     public int paymentDate;
    @SerializedName("deposit")         public double deposit;
    @SerializedName("status")          public String status;
    @SerializedName("terms")           public String terms;
    @SerializedName("signedDate")      public String signedDate;
}