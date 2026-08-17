package com.smartboarding.ui.maintenance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
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

        // 🟢 BỔ SUNG: Hiển thị tên phòng
        if (req.room != null && req.room.roomNumber != null) {
            binding.tvRoom.setText("Phòng " + req.room.roomNumber);
        } else {
            binding.tvRoom.setText("Phòng: Không xác định");
        }

        if (req.images != null && !req.images.isEmpty()) {
            binding.layoutImagesSection.setVisibility(View.VISIBLE);
            binding.rvImages.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.rvImages.setAdapter(
                    new MaintenanceImageAdapter(req.images, this::showFullScreenImage));
        } else {
            binding.layoutImagesSection.setVisibility(View.GONE);
        }

        if (req.adminNote != null && !req.adminNote.isEmpty()) {
            binding.tvAdminNote.setVisibility(View.VISIBLE);
            binding.tvAdminNoteLabel.setVisibility(View.VISIBLE);
            binding.tvAdminNote.setText(req.adminNote);
        }

        switch (req.status != null ? req.status : "") {
            case "processing":
                binding.tvStatus.setText("Đang xử lý");
                binding.tvStatus.setTextColor(getColor(R.color.badge_processing_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                if (req.updatedAt != null) {
                    binding.tvUpdateAt.setVisibility(View.VISIBLE);
                    binding.tvUpdateAt.setText("Chấp nhận xử lý lúc: " + FormatUtils.formatDate(req.updatedAt));
                }
                break;
            case "completed":
                binding.tvStatus.setText("Hoàn thành");
                binding.tvStatus.setTextColor(getColor(R.color.badge_paid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                if (req.updatedAt != null) {
                    binding.tvUpdateAt.setVisibility(View.VISIBLE);
                    binding.tvUpdateAt.setText("Hoàn thành lúc: " + FormatUtils.formatDate(req.updatedAt));
                }
                break;
            case "cancelled":
                binding.tvStatus.setText("Đã hủy");
                binding.tvStatus.setTextColor(getColor(R.color.text_secondary));
                if (req.updatedAt != null) {
                    binding.tvUpdateAt.setVisibility(View.VISIBLE);
                    binding.tvUpdateAt.setText("Đã hủy lúc: " + FormatUtils.formatDate(req.updatedAt));
                }
                break;
            default:
                binding.tvStatus.setText("Đang chờ xử lý");
                binding.tvStatus.setTextColor(getColor(R.color.badge_unpaid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
                if (req.updatedAt != null) {
                    binding.tvUpdateAt.setVisibility(View.VISIBLE);
                    binding.tvUpdateAt.setText("Chấp nhận xử lý lúc: " + FormatUtils.formatDate(req.updatedAt));
                }
        }
    }

    private void showFullScreenImage(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(getColor(android.R.color.black));
        imageView.setOnClickListener(v -> dialog.dismiss());

        Glide.with(this).load(url).into(imageView);

        dialog.setContentView(imageView);
        dialog.show();
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