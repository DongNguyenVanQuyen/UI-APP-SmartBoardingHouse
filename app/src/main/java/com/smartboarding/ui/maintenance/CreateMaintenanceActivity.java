package com.smartboarding.ui.maintenance;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MaintenanceRequest;
import com.smartboarding.data.models.RoomOption;
import com.smartboarding.databinding.ActivityCreateMaintenanceBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateMaintenanceActivity extends AppCompatActivity {
    private static final int MAX_IMAGES = 5;

    private ActivityCreateMaintenanceBinding binding;
    private final List<Uri> selectedImages = new ArrayList<>();
    private List<RoomOption> myRooms = new ArrayList<>(); // 🟢 Lưu danh sách phòng của tenant

    // Photo Picker chuẩn của Android
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImages =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedImages.clear();
                    selectedImages.addAll(uris);
                    binding.tvImageCount.setText(uris.size() + " ảnh đã chọn");
                    binding.tvImageCount.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateMaintenanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Load danh sách phòng từ Backend
        loadRooms();

        // Priority spinner
        String[] priorities = {"Thấp", "Trung bình", "Cao"};
        binding.spinnerPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities));
        binding.spinnerPriority.setSelection(1);

        // Category spinner
        String[] categories = {"Điện", "Nước", "Nội thất", "Khác"};
        binding.spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));

        binding.btnSelectImages.setOnClickListener(v -> openImagePicker());

        binding.btnSubmit.setOnClickListener(v -> submitRequest());
    }

    // 🟢 HÀM MỚI: Tải danh sách phòng từ Backend đổ vào Spinner
    private void loadRooms() {
        RetrofitClient.getInstance(this).getApi().getInvoiceRooms().enqueue(new Callback<ApiResponse<List<RoomOption>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RoomOption>>> call, Response<ApiResponse<List<RoomOption>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    myRooms = response.body().getData();

                    if (myRooms != null && !myRooms.isEmpty()) {
                        List<String> roomNames = new ArrayList<>();
                        int selectedIndex = 0;

                        for (int i = 0; i < myRooms.size(); i++) {
                            RoomOption r = myRooms.get(i);
                            roomNames.add("Phòng " + (r.roomNumber != null ? r.roomNumber : ""));
                            // Chọn sẵn phòng người dùng đang xem ở Dashboard
                            if (r.isSelected) selectedIndex = i;
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateMaintenanceActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, roomNames);
                        binding.spinnerRoom.setAdapter(adapter);
                        binding.spinnerRoom.setSelection(selectedIndex);

                        // Chỉ hiển thị khung Chọn Phòng nếu tenant có >= 2 phòng
                        if (myRooms.size() > 1) {
                            binding.tvRoomLabel.setVisibility(View.VISIBLE);
                            binding.spinnerRoom.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvRoomLabel.setVisibility(View.GONE);
                            binding.spinnerRoom.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RoomOption>>> call, Throwable t) {
                // Không làm gì, fallback về rỗng Backend sẽ tự xử lý
            }
        });
    }

    private void openImagePicker() {
        pickImages.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
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

        // 🟢 Lấy contractId từ phòng được chọn trong Spinner
        String currentContractId = "";
        if (myRooms != null && !myRooms.isEmpty() && binding.spinnerRoom.getSelectedItemPosition() >= 0) {
            currentContractId = myRooms.get(binding.spinnerRoom.getSelectedItemPosition()).contractId;
        }

        RequestBody rbTitle    = RequestBody.create(title, MediaType.parse("text/plain"));
        RequestBody rbDesc     = RequestBody.create(desc, MediaType.parse("text/plain"));
        RequestBody rbPriority = RequestBody.create(priority, MediaType.parse("text/plain"));
        RequestBody rbCategory = RequestBody.create(category, MediaType.parse("text/plain"));
        RequestBody rbContract = RequestBody.create(currentContractId, MediaType.parse("text/plain"));

        List<File> tempFiles = new ArrayList<>();
        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : selectedImages) {
            File tempFile = copyUriToTempFile(uri);
            if (tempFile != null) {
                tempFiles.add(tempFile);
                RequestBody rb = RequestBody.create(tempFile, MediaType.parse("image/*"));
                imageParts.add(MultipartBody.Part.createFormData("images", tempFile.getName(), rb));
            }
        }

        ApiService api = RetrofitClient.getInstance(this).getApi();
        // 🟢 Truyền rbContract vào hàm
        api.createMaintenanceRequest(rbTitle, rbDesc, rbPriority, rbCategory, rbContract, imageParts)
                .enqueue(new Callback<ApiResponse<MaintenanceRequest>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MaintenanceRequest>> call,
                                           Response<ApiResponse<MaintenanceRequest>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        cleanupTempFiles(tempFiles);
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
                        cleanupTempFiles(tempFiles);
                        t.printStackTrace();
                        Toast.makeText(CreateMaintenanceActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private File copyUriToTempFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = "maintenance_" + System.currentTimeMillis() + "_" + tempCounter++ + ".jpg";
            File outFile = new File(getCacheDir(), fileName);

            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int tempCounter = 0;

    private void cleanupTempFiles(List<File> files) {
        for (File f : files) {
            if (f != null && f.exists()) f.delete();
        }
    }
}