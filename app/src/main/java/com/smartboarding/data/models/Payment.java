package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class Payment {
    @SerializedName("_id")            public String id;
    @SerializedName("amount")         public double amount;
    @SerializedName("method")         public String method;
    @SerializedName("status")         public String status;
    @SerializedName("transactionId")  public String transactionId;
    @SerializedName("qrData")         public String qrData;
    @SerializedName("paidAt")         public String paidAt;
    @SerializedName("createdAt")      public String createdAt;
}