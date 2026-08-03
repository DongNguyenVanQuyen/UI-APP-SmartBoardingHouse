package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class PaymentSessionData {
    @SerializedName("paymentId") public String paymentId;
    @SerializedName("payToken")  public String payToken;
    @SerializedName("qrUrl")     public String qrUrl;
    @SerializedName("qrData")    public String qrData;
}