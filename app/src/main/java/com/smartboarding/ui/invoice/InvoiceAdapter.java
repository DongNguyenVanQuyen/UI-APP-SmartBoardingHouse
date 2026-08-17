package com.smartboarding.ui.invoice;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartboarding.R;
import com.smartboarding.data.models.Invoice;
import com.smartboarding.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.VH> {
    public interface OnClickListener { void onClick(Invoice invoice); }

    private List<Invoice> data = new ArrayList<>();
    private final OnClickListener listener;

    public InvoiceAdapter(OnClickListener listener) { this.listener = listener; }

    public void setData(List<Invoice> list) { data = list; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Invoice inv = data.get(pos);
        boolean isDeposit = "deposit".equals(inv.type);

        h.tvMonth.setText(isDeposit ? "Tiền cọc hợp đồng" : "Tháng " + inv.month + "/" + inv.year);
        h.tvAmount.setText(FormatUtils.formatCurrency(inv.totalAmount));
        h.tvDueDate.setText("Hạn: " + FormatUtils.formatDate(inv.dueDate));

        // Hiển thị nhãn phòng + loại hóa đơn để phân biệt khi xem chung
        // hóa đơn của nhiều hợp đồng cùng lúc (chế độ "Tất cả phòng").
        String room = inv.room != null ? inv.room.roomNumber : null;
        if (room != null) {
            h.tvRoomTag.setText(isDeposit ? "Phòng " + room + " · Tiền cọc" : "Phòng " + room);
            h.tvRoomTag.setVisibility(View.VISIBLE);
        } else {
            h.tvRoomTag.setVisibility(View.GONE);
        }

        // Trong phương thức onBindViewHolder của InvoiceAdapter.java
        switch (inv.status != null ? inv.status : "") {
            case "paid":
                h.tvStatus.setText("Đã thanh toán");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_paid_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                break;
            case "pending":
                h.tvStatus.setText("Đang chờ duyệt");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_processing_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                break;
            case "overdue":
                h.tvStatus.setText("Quá hạn");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_overdue_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_overdue);
                break;
            case "cancelled":
                h.tvStatus.setText("Đã hủy");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_expired_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_expired);
                break;
            default:
                h.tvStatus.setText("Chưa thanh toán");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_unpaid_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(inv));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMonth, tvAmount, tvDueDate, tvStatus, tvRoomTag;
        VH(View v) {
            super(v);
            tvMonth    = v.findViewById(R.id.tvMonth);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvDueDate  = v.findViewById(R.id.tvDueDate);
            tvStatus   = v.findViewById(R.id.tvStatus);
            tvRoomTag  = v.findViewById(R.id.tvRoomTag);
        }
    }
}