package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Invoice {
    @SerializedName("_id")            public String id;
    @SerializedName("room")           public Room room;
    // Hợp đồng phát sinh hóa đơn này — dùng để lọc hóa đơn riêng theo từng
    // hợp đồng khi 1 người dùng có nhiều hợp đồng cùng lúc.
    @SerializedName("contract")       public Contract contract;
    // "rent" = hóa đơn tiền phòng/điện/nước hàng tháng, "deposit" = hóa đơn
    // tiền cọc (chỉ có 1 lần duy nhất cho mỗi hợp đồng).
    @SerializedName("type")           public String type;
    @SerializedName("month")          public int month;
    @SerializedName("year")           public int year;
    @SerializedName("dueDate")        public String dueDate;
    @SerializedName("items")          public List<InvoiceItem> items;

    // Chỉ có giá trị khi type = "deposit": số tiền cọc của hợp đồng.
    @SerializedName("depositAmount")  public double depositAmount;

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
    @SerializedName("receiptImage")   public String receiptImage;

    public static class InvoiceItem {
        @SerializedName("name")      public String name;
        @SerializedName("quantity")  public int quantity;
        @SerializedName("unitPrice") public double unitPrice;

        @SerializedName("total")     public double total;
    }
}