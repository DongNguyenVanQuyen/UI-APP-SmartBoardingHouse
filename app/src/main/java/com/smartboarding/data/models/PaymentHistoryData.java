package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaymentHistoryData {
    @SerializedName("payments") public List<Payment> payments;
}