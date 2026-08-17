package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class MeterScanResult {
    @SerializedName("imageUrl")         public String imageUrl;        // Cloudinary URL ảnh đã upload
    // Hợp đồng/phòng mà server đã dùng để chấm ảnh này.
    @SerializedName("contractId")       public String contractId;
    @SerializedName("roomId")           public String roomId;
    @SerializedName("roomNumber")       public String roomNumber;
    @SerializedName("ocrRawText")       public String ocrRawText;      // Raw text Gemini đọc được
    @SerializedName("geminiNote")       public String geminiNote;      // Ghi chú của Gemini
    @SerializedName("suggestedReading") public Double suggestedReading; // null nếu không đọc được
    @SerializedName("alreadySubmitted") public boolean alreadySubmitted;
    @SerializedName("existing")         public MeterReading existing;  // null nếu chưa gửi tháng này
}