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

    // Danh sách phòng (theo hợp đồng còn hiệu lực) mà tenant có thể chuyển
    // tới — dùng cho nút/màn "Chuyển phòng" ở Dashboard.
    @SerializedName("rooms")
    public java.util.List<RoomOption> rooms;

    // true nếu tenant đang thuê từ 2 phòng trở lên cùng lúc — app chỉ nên
    // hiện nút "Chuyển phòng" khi giá trị này = true.
    @SerializedName("hasMultipleRooms")
    public boolean hasMultipleRooms;

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