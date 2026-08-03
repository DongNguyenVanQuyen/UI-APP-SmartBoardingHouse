package com.smartboarding.ui.meter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MeterReading;
import com.smartboarding.data.models.MeterReadingPrevious;
import com.smartboarding.data.models.MeterScanResult;
import com.smartboarding.databinding.ActivityMeterReadingBinding;

import java.io.File;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeterReadingActivity extends AppCompatActivity {

    private ActivityMeterReadingBinding binding;
    private ImageCapture imageCapture;
    private Uri capturedImageUri;

    private double previousReadingValue = 0;
    private String existingReadingId = null;
    private String scannedImageUrl = null;
    private String scannedOcrRawText = null;
    private File capturedFile = null;

    private final ActivityResultLauncher<String> requestCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Toast.makeText(this, "Cần quyền camera để chụp ảnh công tơ", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeterReadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        String[] types = {"Điện", "Nước"};
        binding.spinnerType.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        // Thêm listener để tự tải lại chỉ số kỳ trước khi đổi loại công tơ
        binding.spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                resetScanState();
                loadPreviousReading();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        loadPreviousReading();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCamera.launch(Manifest.permission.CAMERA);
        }

        binding.btnCapture.setOnClickListener(v -> captureAndScan());
        binding.btnManual.setOnClickListener(v -> {
            binding.layoutCamera.setVisibility(View.GONE);
            binding.layoutManual.setVisibility(View.VISIBLE);
        });
        binding.btnSubmit.setOnClickListener(v -> submitReading());
    }
    /**
     * Quay lại màn hình camera để chụp lại — dùng khi muốn chụp lại ảnh cùng loại,
     * hoặc sau khi đổi loại công tơ (điện/nước) và cần chụp ảnh tương ứng.
     */
    public void onRetakeClicked(View v) {
        resetScanState();
        binding.layoutManual.setVisibility(View.GONE);
        binding.layoutCamera.setVisibility(View.VISIBLE);
        binding.ivCaptured.setVisibility(View.GONE);
        binding.tvOcrResult.setText("Hướng camera vào màn số công tơ rồi bấm Chụp");
    }

    /** Xoá dữ liệu ảnh/scan cũ để không bị lẫn giữa điện và nước. */
    private void resetScanState() {
        capturedImageUri = null;
        capturedFile = null;
        scannedImageUrl = null;
        scannedOcrRawText = null;
        binding.ivCapturedInForm.setVisibility(View.GONE);
        binding.tvOcrResultForm.setVisibility(View.GONE);
        binding.etCurrentReading.setText("");
    }

    private void loadPreviousReading() {
        String typeVal = binding.spinnerType.getSelectedItemPosition() == 0 ? "electric" : "water";

        binding.tvPreviousReading.setText("Đang tải chỉ số kỳ trước...");

        RetrofitClient.getInstance(this).getApi()
                .getPreviousReading(typeVal)
                .enqueue(new Callback<ApiResponse<MeterReadingPrevious>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MeterReadingPrevious>> call,
                                           Response<ApiResponse<MeterReadingPrevious>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            MeterReadingPrevious data = response.body().getData();
                            previousReadingValue = data.previousReading;

                            binding.tvPreviousReading.setText(
                                    "Chỉ số kỳ trước: " + data.previousReading
                                            + " | Tháng " + data.month + "/" + data.year);

                            if (data.alreadySubmitted && data.existing != null) {
                                existingReadingId = data.existing.id;
                                binding.layoutAlreadySubmitted.setVisibility(View.VISIBLE);
                                binding.tvAlreadySubmitted.setText(
                                        "⚠ Tháng này bạn đã gửi chỉ số "
                                                + (typeVal.equals("electric") ? "điện" : "nước")
                                                + ": " + data.existing.currentReading
                                                + "\nChụp lại nếu muốn cập nhật.");
                            } else {
                                existingReadingId = null;
                                binding.layoutAlreadySubmitted.setVisibility(View.GONE);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MeterReadingPrevious>> call, Throwable t) {
                        binding.tvPreviousReading.setText("Không tải được chỉ số kỳ trước");
                    }
                });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
                        preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi động camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ─── Bước 2: Chụp ảnh → gọi /scan → Gemini đọc số ───────────────────────
    private void captureAndScan() {
        if (imageCapture == null) return;

        capturedFile = new File(getCacheDir(), "meter_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(capturedFile).build();

        binding.btnCapture.setEnabled(false);
        binding.tvOcrResult.setText("Đang chụp..."); // vẫn ở màn camera lúc này

        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        capturedImageUri = Uri.fromFile(capturedFile);
                        binding.ivCaptured.setImageURI(capturedImageUri);
                        binding.ivCaptured.setVisibility(View.VISIBLE);
                        binding.btnCapture.setEnabled(true);

                        // Chuyển sang form nhập — từ đây dùng tvOcrResultForm
                        binding.layoutCamera.setVisibility(View.GONE);
                        binding.layoutManual.setVisibility(View.VISIBLE);
                        binding.ivCapturedInForm.setImageURI(capturedImageUri);
                        binding.ivCapturedInForm.setVisibility(View.VISIBLE);

                        callScanApi(capturedFile);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        binding.btnCapture.setEnabled(true);
                        binding.tvOcrResult.setText("Chụp thất bại, thử lại"); // vẫn ở màn camera
                    }
                });
    }

    private void callScanApi(File imageFile) {
        binding.tvOcrResultForm.setVisibility(View.VISIBLE);
        binding.tvOcrResultForm.setText("Gemini đang đọc số công tơ...");
        binding.btnSubmit.setEnabled(false);

        String typeVal = binding.spinnerType.getSelectedItemPosition() == 0 ? "electric" : "water";

        RequestBody rbType = RequestBody.create(typeVal, MediaType.parse("text/plain"));
        RequestBody rbImage = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part imagePart =
                MultipartBody.Part.createFormData("image", imageFile.getName(), rbImage);

        RetrofitClient.getInstance(this).getApi()
                .scanMeterImage(rbType, imagePart)
                .enqueue(new Callback<ApiResponse<MeterScanResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MeterScanResult>> call,
                                           Response<ApiResponse<MeterScanResult>> response) {
                        binding.btnSubmit.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            MeterScanResult scan = response.body().getData();

                            scannedImageUrl = scan.imageUrl;
                            scannedOcrRawText = scan.ocrRawText;

                            if (scan.suggestedReading != null) {
                                binding.etCurrentReading.setText(String.valueOf(scan.suggestedReading));
                                binding.tvOcrResultForm.setText(
                                        "Gemini đọc được: " + scan.suggestedReading
                                                + "\nKiểm tra lại và sửa nếu cần");
                            } else {
                                binding.tvOcrResultForm.setText(
                                        "Không đọc được số rõ trong ảnh, vui lòng nhập tay");
                            }

                            if (scan.alreadySubmitted && scan.existing != null) {
                                existingReadingId = scan.existing.id;
                                binding.layoutAlreadySubmitted.setVisibility(View.VISIBLE);
                                binding.tvAlreadySubmitted.setText(
                                        "⚠ Tháng này bạn đã gửi: "
                                                + scan.existing.currentReading
                                                + "\nBấm Lưu để cập nhật bằng ảnh và số mới.");
                            }
                        } else {
                            binding.tvOcrResultForm.setText("Không đọc được, vui lòng nhập tay");
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MeterScanResult>> call, Throwable t) {
                        binding.btnSubmit.setEnabled(true);
                        binding.tvOcrResultForm.setText("Lỗi kết nối khi đọc ảnh, vui lòng nhập tay");
                    }
                });
    }

    private void submitReading() {
        String currentStr = binding.etCurrentReading.getText() != null
                ? binding.etCurrentReading.getText().toString().trim() : "";

        if (currentStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập chỉ số hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (existingReadingId != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Cập nhật chỉ số")
                    .setMessage("Bạn đã gửi chỉ số tháng này rồi. Bạn có muốn cập nhật thành "
                            + currentStr + " không?")
                    .setPositiveButton("Cập nhật", (d, w) -> doUpdate(currentStr))
                    .setNegativeButton("Huỷ", null)
                    .show();
        } else {
            doCreate(currentStr);
        }
    }

    private void doCreate(String currentStr) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        String typeVal = binding.spinnerType.getSelectedItemPosition() == 0 ? "electric" : "water";

        RequestBody rbType    = RequestBody.create(typeVal,    MediaType.parse("text/plain"));
        RequestBody rbReading = RequestBody.create(currentStr, MediaType.parse("text/plain"));

        if (scannedImageUrl != null) {
            RequestBody rbImageUrl = RequestBody.create(scannedImageUrl, MediaType.parse("text/plain"));
            RequestBody rbOcr = scannedOcrRawText != null
                    ? RequestBody.create(scannedOcrRawText, MediaType.parse("text/plain")) : null;

            RetrofitClient.getInstance(this).getApi()
                    .createMeterReadingWithUrl(rbType, rbReading, rbImageUrl, rbOcr)
                    .enqueue(buildCreateCallback());
        } else if (capturedFile != null) {
            RequestBody rbImage = RequestBody.create(capturedFile, MediaType.parse("image/jpeg"));
            MultipartBody.Part imagePart =
                    MultipartBody.Part.createFormData("image", capturedFile.getName(), rbImage);

            RetrofitClient.getInstance(this).getApi()
                    .createMeterReading(rbType, rbReading, imagePart)
                    .enqueue(buildCreateCallback());
        } else {
            // Nhập tay, không có ảnh — vẫn gửi được, chỉ cần bỏ trống phần ảnh.
            RetrofitClient.getInstance(this).getApi()
                    .createMeterReading(rbType, rbReading, null)
                    .enqueue(buildCreateCallback());
        }
    }

    private void doUpdate(String currentStr) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        RequestBody rbReading = RequestBody.create(currentStr, MediaType.parse("text/plain"));

        MultipartBody.Part imagePart = null;
        if (capturedFile != null && scannedImageUrl == null) {
            RequestBody rb = RequestBody.create(capturedFile, MediaType.parse("image/jpeg"));
            imagePart = MultipartBody.Part.createFormData("image", capturedFile.getName(), rb);
        }

        RequestBody rbImageUrl = scannedImageUrl != null
                ? RequestBody.create(scannedImageUrl, MediaType.parse("text/plain")) : null;

        RetrofitClient.getInstance(this).getApi()
                .updateMeterReading(existingReadingId, rbReading, rbImageUrl, imagePart)
                .enqueue(new Callback<ApiResponse<MeterReading>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MeterReading>> call,
                                           Response<ApiResponse<MeterReading>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            MeterReading r = response.body().getData();
                            Toast.makeText(MeterReadingActivity.this,
                                    "Đã cập nhật! Tiêu thụ: " + r.usage
                                            + " | Chi phí: " + (long) r.totalCost + "đ",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(MeterReadingActivity.this,
                                    "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MeterReading>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(MeterReadingActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Callback<ApiResponse<MeterReading>> buildCreateCallback() {
        return new Callback<ApiResponse<MeterReading>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeterReading>> call,
                                   Response<ApiResponse<MeterReading>> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    MeterReading r = response.body().getData();
                    Toast.makeText(MeterReadingActivity.this,
                            "Đã lưu! Tiêu thụ: " + r.usage
                                    + " | Chi phí: " + (long) r.totalCost + "đ",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else if (response.code() == 409) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(MeterReadingActivity.this,
                            "Tháng này đã có chỉ số. Bấm Lưu lại để cập nhật.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MeterReadingActivity.this,
                            "Lưu thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<MeterReading>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(MeterReadingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public void onTypeChanged(View v) {
        loadPreviousReading();
    }
}