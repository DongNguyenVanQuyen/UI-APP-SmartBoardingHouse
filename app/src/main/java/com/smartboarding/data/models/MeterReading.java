package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class MeterReading {
    @SerializedName("_id")             public String id;
    @SerializedName("type")            public String type;
    @SerializedName("currentReading")  public double currentReading;
    @SerializedName("previousReading") public double previousReading;
    @SerializedName("usage")           public double usage;
    @SerializedName("unitPrice")       public double unitPrice;
    @SerializedName("totalCost")       public double totalCost;
    @SerializedName("imageUrl")        public String imageUrl;
    @SerializedName("month")           public int month;
    @SerializedName("year")            public int year;
    @SerializedName("ocrRawText")      public String ocrRawText;
    @SerializedName("readingDate")     public String readingDate;
    @SerializedName("isVerified")      public boolean isVerified;
    @SerializedName("roomNumber")      public String roomNumber;
}