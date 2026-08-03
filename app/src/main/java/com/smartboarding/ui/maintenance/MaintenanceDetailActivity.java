package com.smartboarding.ui.maintenance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartboarding.R;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MaintenanceRequest;
import com.smartboarding.databinding.ActivityMaintenanceDetailBinding;
import com.smartboarding.utils.FormatUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MaintenanceDetailActivity extends AppCompatActivity {
    private ActivityMaintenanceDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaintenanceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String requestId = getIntent().getStringExtra("request_id");
        binding.btnBack.setOnClickListener(v -> finish());

        if (requestId != null) loadRequest(requestId);
    }

    private void loadRequest(String id) {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi()
                .getMaintenanceById(id)
                .enqueue(new Callback<ApiResponse<MaintenanceRequest>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MaintenanceRequest>> call,
                                           Response<ApiResponse<MaintenanceRequest>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            bind(response.body().getData());
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MaintenanceRequest>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(MaintenanceDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bind(MaintenanceRequest req) {
        binding.tvTitle.setText(req.title);
        binding.tvDescription.setText(req.description);
        binding.tvDate.setText(FormatUtils.formatDate(req.createdAt));
        binding.tvPriority.setText(priorityVi(req.priority));
        binding.tvCategory.setText(categoryVi(req.category));

        if (req.adminNote != null && !req.adminNote.isEmpty()) {
            binding.tvAdminNote.setVisibility(View.VISIBLE);
            binding.tvAdminNoteLabel.setVisibility(View.VISIBLE);
            binding.tvAdminNote.setText(req.adminNote);
        }

        switch (req.status != null ? req.status : "") {
            case "processing":
                binding.tvStatus.setText("🔧 Đang xử lý");
                binding.tvStatus.setTextColor(getColor(R.color.badge_processing_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                break;
            case "completed":
                binding.tvStatus.setText("✅ Hoàn thành");
                binding.tvStatus.setTextColor(getColor(R.color.badge_paid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                if (req.resolvedAt != null) {
                    binding.tvResolvedAt.setVisibility(View.VISIBLE);
                    binding.tvResolvedAt.setText("Hoàn thành: " + FormatUtils.formatDate(req.resolvedAt));
                }
                break;
            case "cancelled":
                binding.tvStatus.setText("Đã hủy");
                binding.tvStatus.setTextColor(getColor(R.color.text_secondary));
                break;
            default:
                binding.tvStatus.setText("⏳ Đang chờ xử lý");
                binding.tvStatus.setTextColor(getColor(R.color.badge_unpaid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }
    }

    private String priorityVi(String p) {
        if (p == null) return "Trung bình";
        switch (p) { case "high": return "Cao"; case "low": return "Thấp"; default: return "Trung bình"; }
    }

    private String categoryVi(String c) {
        if (c == null) return "Khác";
        switch (c) {
            case "electrical": return "Điện"; case "plumbing": return "Nước";
            case "furniture":  return "Nội thất"; default: return "Khác";
        }
    }
}