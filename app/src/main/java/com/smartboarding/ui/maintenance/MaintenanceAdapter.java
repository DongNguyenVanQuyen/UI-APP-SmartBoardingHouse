package com.smartboarding.ui.maintenance;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartboarding.R;
import com.smartboarding.data.models.MaintenanceRequest;
import com.smartboarding.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceAdapter extends RecyclerView.Adapter<MaintenanceAdapter.VH> {
    public interface OnClick { void onClick(MaintenanceRequest req); }
    private List<MaintenanceRequest> data = new ArrayList<>();
    private final OnClick listener;

    public MaintenanceAdapter(OnClick listener) { this.listener = listener; }
    public void setData(List<MaintenanceRequest> list) { data = list; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MaintenanceRequest req = data.get(pos);
        h.tvTitle.setText(req.title);
        h.tvDesc.setText(req.description);
        h.tvDate.setText(FormatUtils.formatDate(req.createdAt) + " • Ưu tiên: " + priorityVi(req.priority));

        switch (req.status != null ? req.status : "") {
            case "processing":
                h.tvStatus.setText("🔧 Đang xử lý");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_processing_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                h.tvResolvedAt.setVisibility(View.GONE);
                break;
            case "completed":
                h.tvStatus.setText("✅ Hoàn thành");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_paid_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                if (req.resolvedAt != null) {
                    h.tvResolvedAt.setVisibility(View.VISIBLE);
                    h.tvResolvedAt.setText("Hoàn thành: " + FormatUtils.formatDate(req.resolvedAt));
                }
                break;
            case "cancelled":
                h.tvStatus.setText("Đã hủy");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.text_secondary));
                h.tvStatus.setBackgroundResource(R.drawable.bg_input);
                h.tvResolvedAt.setVisibility(View.GONE);
                break;
            default:
                h.tvStatus.setText("⏳ Đang chờ");
                h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.badge_unpaid_text));
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
                h.tvResolvedAt.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(req));
    }

    private String priorityVi(String p) {
        if (p == null) return "Trung bình";
        switch (p) {
            case "high":   return "Cao";
            case "low":    return "Thấp";
            default:       return "Trung bình";
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvDate, tvStatus, tvResolvedAt;
        VH(View v) {
            super(v);
            tvTitle      = v.findViewById(R.id.tvTitle);
            tvDesc       = v.findViewById(R.id.tvDesc);
            tvDate       = v.findViewById(R.id.tvDate);
            tvStatus     = v.findViewById(R.id.tvStatus);
            tvResolvedAt = v.findViewById(R.id.tvResolvedAt);
        }
    }
}