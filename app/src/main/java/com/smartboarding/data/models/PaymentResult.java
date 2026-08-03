package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaymentResult {
    @SerializedName("payment") public Payment payment;
    @SerializedName("invoice") public Invoice invoice;
    @SerializedName("qrData")  public String qrData;
}