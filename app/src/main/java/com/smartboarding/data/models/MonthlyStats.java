package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MonthlyStats {
    @SerializedName("summary")   public Summary summary;
    @SerializedName("utilities") public Utilities utilities;
    @SerializedName("breakdown") public List<BreakdownItem> breakdown;

    public static class Summary {
        @SerializedName("totalAmount")   public double totalAmount;
        @SerializedName("paidAmount")    public double paidAmount;
        @SerializedName("invoiceStatus") public String invoiceStatus;
    }
    public static class Utilities {
        @SerializedName("electric") public UtilityItem electric;
        @SerializedName("water")    public UtilityItem water;
    }
    public static class UtilityItem {
        @SerializedName("usage") public double usage;
        @SerializedName("cost")  public double cost;
    }
    public static class BreakdownItem {
        @SerializedName("name")       public String name;
        @SerializedName("amount")     public double amount;
        @SerializedName("percentage") public int percentage;
    }
}