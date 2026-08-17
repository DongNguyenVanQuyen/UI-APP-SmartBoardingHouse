package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class MeterReadingPrevious {
    @SerializedName("type")             public String type;
    @SerializedName("month")            public int month;
    @SerializedName("year")             public int year;
    // Hợp đồng/phòng mà server đã dùng để trả về chỉ số này — dùng lại khi
    // gửi /scan hoặc lưu chỉ số để đảm bảo đúng phòng đã chọn.
    @SerializedName("contractId")       public String contractId;
    @SerializedName("roomId")           public String roomId;
    @SerializedName("roomNumber")       public String roomNumber;
    @SerializedName("previousReading")  public double previousReading;
    @SerializedName("alreadySubmitted") public boolean alreadySubmitted;
    @SerializedName("existing")         public MeterReading existing; // null nếu chưa gửi tháng này
}