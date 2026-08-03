package com.smartboarding.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    public static String formatCurrency(double amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format((long) amount) + " đ";
    }

    public static String formatShort(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fK", amount / 1_000);
        }
        return String.valueOf((long) amount);
    }

    public static String formatDate(String isoDate) {
        if (isoDate == null) return "--";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = sdf.parse(isoDate);
            SimpleDateFormat out = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            return out.format(date);
        } catch (Exception e) {
            try {
                // Thử format khác
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf2.parse(isoDate);
                SimpleDateFormat out = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                return out.format(date);
            } catch (Exception ex) {
                return isoDate;
            }
        }
    }

    public static String formatDateTime(String isoDate) {
        if (isoDate == null) return "--";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = sdf.parse(isoDate);
            SimpleDateFormat out = new SimpleDateFormat("HH:mm - d/M/yyyy", Locale.getDefault());
            return out.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static String formatTime(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = sdf.parse(isoDate);
            SimpleDateFormat out = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return out.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    public static String statusToVietnamese(String status) {
        if (status == null) return "--";
        switch (status) {
            case "paid":       return "Đã thanh toán";
            case "unpaid":     return "Chưa thanh toán";
            case "partial":    return "Thanh toán một phần";
            case "overdue":    return "Quá hạn";
            case "pending":    return "Đang chờ";
            case "processing": return "Đang xử lý";
            case "completed":  return "Hoàn thành";
            case "cancelled":  return "Đã hủy";
            case "active":     return "Đang hiệu lực";
            case "expired":    return "Hết hạn";
            case "electric":   return "Điện";
            case "water":      return "Nước";
            default:           return status;
        }
    }
}