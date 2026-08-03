package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class DashboardData {

    @SerializedName("tenant")
    public Tenant tenant;

    @SerializedName("room")
    public Room room;

    @SerializedName("invoice")
    public Invoice invoice;

    @SerializedName("stats")
    public Stats stats;

    @SerializedName("unreadNotifications")
    public int unreadNotifications;

    @SerializedName("activeMaintenanceRequests")
    public int activeMaintenanceRequests;

    @SerializedName("contract")
    public Contract contract;

    public static class Stats {

        @SerializedName("rentAmount")
        public double rentAmount;

        @SerializedName("electricAmount")
        public double electricAmount;

        @SerializedName("waterAmount")
        public double waterAmount;

        @SerializedName("unpaidCount")
        public int unpaidCount;

        @SerializedName("totalDebt")
        public double totalDebt;
    }
}