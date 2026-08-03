package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class RoomData {
    @SerializedName("room")     public Room room;
    @SerializedName("floor")    public Floor floor;
    @SerializedName("contract") public ContractSummary contract;

    public static class ContractSummary {
        @SerializedName("_id")          public String id;
        @SerializedName("startDate")    public String startDate;
        @SerializedName("endDate")      public String endDate;
        @SerializedName("monthlyRent")  public double monthlyRent;
    }
}