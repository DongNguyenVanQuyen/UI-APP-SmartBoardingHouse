package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

// Một phòng mà tenant có thể chuyển tới — CHỈ gồm các hợp đồng còn hiệu lực
// (status "active"). Trả về trong DashboardData.rooms và từ GET /invoices/rooms.
// Dùng cho màn "chuyển phòng" ở Dashboard và bộ lọc/chuyển phòng ở màn Hóa đơn.
public class RoomOption {
    @SerializedName("contractId")     public String contractId;
    @SerializedName("contractNumber") public String contractNumber;
    @SerializedName("roomId")         public String roomId;
    @SerializedName("roomNumber")     public String roomNumber;
    @SerializedName("monthlyRent")    public double monthlyRent;
    @SerializedName("startDate")      public String startDate;
    @SerializedName("endDate")        public String endDate;
    // true nếu đây là phòng đang được chọn hiện tại (đồng bộ Dashboard/Invoice/công tơ).
    @SerializedName("isSelected")     public boolean isSelected;
}