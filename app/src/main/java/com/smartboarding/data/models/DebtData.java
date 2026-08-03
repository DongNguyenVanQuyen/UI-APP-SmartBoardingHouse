package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DebtData {
    @SerializedName("summary")    public Summary summary;
    @SerializedName("overdue")    public List<Invoice> overdue;
    @SerializedName("pending")    public List<Invoice> pending;
    @SerializedName("recentPaid") public List<Invoice> recentPaid;

    public static class Summary {
        @SerializedName("totalDebt")   public double totalDebt;
        @SerializedName("overdueDebt") public double overdueDebt;
        @SerializedName("pendingDebt") public double pendingDebt;
    }
}