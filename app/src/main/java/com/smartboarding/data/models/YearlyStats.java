package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YearlyStats {
    @SerializedName("year")        public int year;
    @SerializedName("summary")     public Summary summary;
    @SerializedName("monthlyData") public List<MonthData> monthlyData;
    @SerializedName("utilities")   public Utilities utilities;

    public static class Summary {
        @SerializedName("totalYear") public double totalYear;
        @SerializedName("paidYear")  public double paidYear;
        @SerializedName("debtYear")  public double debtYear;
    }
    public static class MonthData {
        @SerializedName("month")       public int month;
        @SerializedName("totalAmount") public double totalAmount;
        @SerializedName("paidAmount")  public double paidAmount;
        @SerializedName("status")      public String status;
    }
    public static class Utilities {
        @SerializedName("electricTotal") public double electricTotal;
        @SerializedName("waterTotal")    public double waterTotal;
    }
}