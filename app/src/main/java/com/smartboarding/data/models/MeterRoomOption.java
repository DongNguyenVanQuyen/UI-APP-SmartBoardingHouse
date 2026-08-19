package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

// Một phòng/hợp đồng mà tenant có thể chọn để ghi chỉ số công tơ.
// Trả về từ GET /meter-readings/rooms — dùng để hiển thị Spinner chọn phòng
// khi tenant đang có nhiều hợp đồng thuê cùng lúc.
public class MeterRoomOption {
    @SerializedName("contractId")     public String contractId;
    @SerializedName("contractNumber") public String contractNumber;
    @SerializedName("roomId")         public String roomId;
    @SerializedName("roomNumber")     public String roomNumber;
    @SerializedName("isSelected")     public boolean isSelected;
}