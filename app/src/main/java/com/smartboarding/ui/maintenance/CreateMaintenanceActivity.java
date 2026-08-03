package com.smartboarding.ui.maintenance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MaintenanceRequest;
import com.smartboarding.databinding.ActivityCreateMaintenanceBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateMaintenanceActivity extends AppCompatActivity {
    private ActivityCreateMaintenanceBinding binding;
    private final List<Uri> selectedImages = new ArrayList<>();

    private final ActivityResultLauncher<String[]> pickImages =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null) {
                    selectedImages.clear();
                    selectedImages.addAll(uris);
                    binding.tvImageCount.setText(uris.size() + " ảnh đã chọn");
                    binding.tvImageCount.setVisibility(uris.isEmpty() ? View.GONE : View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openImagePicker();
                else Toast.makeText(this, "Cần quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateMaintenanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Priority spinner
        String[] priorities = {"Thấp", "Trung bình", "Cao"};
        binding.spinnerPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities));
        binding.spinnerPriority.setSelection(1);

        // Category spinner
        String[] categories = {"Điện", "Nước", "Nội thất", "Khác"};
        binding.spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));

        binding.btnSelectImages.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        });

        binding.btnSubmit.setOnClickListener(v -> submitRequest());
    }

    private void openImagePicker() {
        pickImages.launch(new String[]{"image/*"});
    }

    private void submitRequest() {
        String title = binding.etTitle.getText().toString().trim();
        String desc  = binding.etDescription.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề và mô tả", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        String[] priorityValues = {"low", "medium", "high"};
        String priority = priorityValues[binding.spinnerPriority.getSelectedItemPosition()];
        String[] categoryValues = {"electrical", "plumbing", "furniture", "other"};
        String category = categoryValues[binding.spinnerCategory.getSelectedItemPosition()];

        RequestBody rbTitle    = RequestBody.create(title, MediaType.parse("text/plain"));
        RequestBody rbDesc     = RequestBody.create(desc, MediaType.parse("text/plain"));
        RequestBody rbPriority = RequestBody.create(priority, MediaType.parse("text/plain"));
        RequestBody rbCategory = RequestBody.create(category, MediaType.parse("text/plain"));

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : selectedImages) {
            try {
                File file = new File(uri.getPath());
                RequestBody rb = RequestBody.create(file, MediaType.parse("image/*"));
                imageParts.add(MultipartBody.Part.createFormData("images", file.getName(), rb));
            } catch (Exception ignored) {}
        }

        ApiService api = RetrofitClient.getInstance(this).getApi();
        api.createMaintenanceRequest(rbTitle, rbDesc, rbPriority, rbCategory, imageParts)
                .enqueue(new Callback<ApiResponse<MaintenanceRequest>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MaintenanceRequest>> call,
                                           Response<ApiResponse<MaintenanceRequest>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(CreateMaintenanceActivity.this,
                                    "Gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CreateMaintenanceActivity.this,
                                    "Gửi thất bại, thử lại", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MaintenanceRequest>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);

                        t.printStackTrace();

                        Toast.makeText(
                                CreateMaintenanceActivity.this,
                                t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}