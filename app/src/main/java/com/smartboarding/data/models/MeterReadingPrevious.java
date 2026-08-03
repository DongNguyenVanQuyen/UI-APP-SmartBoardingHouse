package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class MeterReadingPrevious {
    @SerializedName("type")             public String type;
    @SerializedName("month")            public int month;
    @SerializedName("year")             public int year;
    @SerializedName("previousReading")  public double previousReading;
    @SerializedName("alreadySubmitted") public boolean alreadySubmitted;
    @SerializedName("existing")         public MeterReading existing; // null nếu chưa gửi tháng này
}