package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MonthlyReport {
    @SerializedName("period")   public Period period;
    @SerializedName("invoice")  public Invoice invoice;
    @SerializedName("payments") public List<Payment> payments;

    public static class Period {
        @SerializedName("month") public int month;
        @SerializedName("year")  public int year;
        @SerializedName("label") public String label;
    }
}