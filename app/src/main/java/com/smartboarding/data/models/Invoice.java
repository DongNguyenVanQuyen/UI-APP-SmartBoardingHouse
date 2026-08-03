package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Invoice {
    @SerializedName("_id")            public String id;
    @SerializedName("room")           public Room room;
    @SerializedName("month")          public int month;
    @SerializedName("year")           public int year;
    @SerializedName("dueDate")        public String dueDate;
    @SerializedName("items")          public List<InvoiceItem> items;

    // Các khoản phí cố định — trước đây thiếu nên app không hiển thị được
    // tiền phòng/điện/nước khi items[] rỗng, dù server đã trả đủ dữ liệu.
    @SerializedName("roomPrice")      public double roomPrice;
    @SerializedName("electricUsage")  public double electricUsage;
    @SerializedName("electricPrice")  public double electricPrice;
    @SerializedName("waterUsage")     public double waterUsage;
    @SerializedName("waterPrice")     public double waterPrice;
    @SerializedName("serviceFee")     public double serviceFee;

    @SerializedName("totalAmount")    public double totalAmount;
    @SerializedName("paidAmount")     public double paidAmount;
    @SerializedName("status")        public String status;
    @SerializedName("note")          public String note;
    @SerializedName("createdAt")    public String createdAt;

    public static class InvoiceItem {
        @SerializedName("name")      public String name;
        @SerializedName("quantity")  public int quantity;
        @SerializedName("unitPrice") public double unitPrice;
        @SerializedName("total")     public double total;
    }
}