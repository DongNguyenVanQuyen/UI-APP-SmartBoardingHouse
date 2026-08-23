package com.smartboarding.ui.meter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
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

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MeterReading;
import com.smartboarding.data.models.MeterReadingPrevious;
import com.smartboarding.data.models.MeterRoomOption;
import com.smartboarding.data.models.MeterScanResult;
import com.smartboarding.databinding.ActivityMeterReadingBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeterReadingActivity extends AppCompatActivity {

    private static final String TAG = "MeterReadingActivity";

    private ActivityMeterReadingBinding binding;
    private ImageCapture imageCapture;
    private Uri capturedImageUri;

    private double previousReadingValue = 0;
    private String existingReadingId = null;
    private String scannedImageUrl = null;
    private String scannedOcrRawText = null;
    private File capturedFile = null;

    // Danh sách phòng (hợp đồng) tenant có thể chọn để ghi chỉ số, và phòng
    // đang được chọn hiện tại. selectedContractId == null nghĩa là tenant chỉ
    // có đúng 1 hợp đồng — server sẽ tự chọn hợp đồng đó, không cần chọn.
    private final List<MeterRoomOption> roomOptions = new ArrayList<>();
    private String selectedContractId = null;

    private final ActivityResultLauncher<String> requestCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Toast.makeText(this, "Cần quyền camera để chụp ảnh công tơ", Toast.LENGTH_SHORT).show();
            });

    // Chọn ảnh có sẵn từ thư viện thay vì chụp trực tiếp.
    // GetContent() dùng Storage Access Framework / Photo Picker hệ thống nên không cần xin quyền runtime.
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try {
                    File file = copyUriToCacheFile(uri);
                    showCapturedImageAndScan(file, uri);
                } catch (IOException e) {
                    Log.e(TAG, "Lỗi đọc ảnh từ thư viện", e);
                    Toast.makeText(this, "Không đọc được ảnh đã chọn", Toast.LENGTH_SHORT).show();
                }
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

        // Tải danh sách phòng trước — nếu tenant có nhiều phòng thì hiện Spinner
        // chọn phòng, sau đó mới tải chỉ số kỳ trước theo đúng phòng đã chọn.
        loadRoomOptions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCamera.launch(Manifest.permission.CAMERA);
        }

        binding.btnCapture.setOnClickListener(v -> captureAndScan());
        binding.btnPickImage.setOnClickListener(v -> pickImage.launch("image/*"));
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

    /**
     * Tải danh sách phòng (theo hợp đồng active) mà tenant có thể chọn để ghi
     * chỉ số. Nếu tenant chỉ có 1 phòng thì ẩn Spinner và để server tự chọn
     * (tương thích ngược). Nếu có từ 2 phòng trở lên thì hiện Spinner chọn
     * phòng — người dùng PHẢI chọn phòng trước khi chụp/nhập chỉ số, vì mỗi
     * lần đổi phòng dữ liệu (chỉ số kỳ trước, ảnh đã gửi tháng này...) phải
     * được lấy lại đúng theo phòng đó, không được dùng lẫn dữ liệu phòng khác.
     */
    private void loadRoomOptions() {
        RetrofitClient.getInstance(this).getApi()
                .getMeterRooms()
                .enqueue(new Callback<ApiResponse<List<MeterRoomOption>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<MeterRoomOption>>> call,
                                           Response<ApiResponse<List<MeterRoomOption>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            roomOptions.clear();
                            roomOptions.addAll(response.body().getData());
                        }
                        setupRoomSpinner();
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<MeterRoomOption>>> call, Throwable t) {
                        Log.e(TAG, "Không tải được danh sách phòng", t);
                        // Không chặn màn hình — vẫn cho thao tác bình thường như khi
                        // chỉ có 1 phòng (server sẽ tự chọn hợp đồng active duy nhất).
                        setupRoomSpinner();
                    }
                });
    }

    private void setupRoomSpinner() {
        if (roomOptions.size() < 2) {
            // Chỉ 0 hoặc 1 phòng — không cần chọn, để server tự xử lý.
            binding.layoutRoomSelect.setVisibility(View.GONE);
            selectedContractId = roomOptions.size() == 1 ? roomOptions.get(0).contractId : null;
            loadPreviousReading();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (MeterRoomOption r : roomOptions) {
            labels.add("Phòng " + r.roomNumber);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, labels);
        binding.spinnerRoom.setAdapter(spinnerAdapter);
        binding.layoutRoomSelect.setVisibility(View.VISIBLE);

        binding.spinnerRoom.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedContractId = roomOptions.get(position).contractId;
                // Đổi phòng -> xoá dữ liệu ảnh/scan cũ và tải lại chỉ số kỳ
                // trước cho ĐÚNG phòng vừa chọn, tránh lẫn dữ liệu giữa các phòng.
                resetScanState();
                loadPreviousReading();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Mặc định chọn phòng đang được chọn ở Dashboard
        int defaultPosition = 0;
        for (int i = 0; i < roomOptions.size(); i++) {
            if (roomOptions.get(i).isSelected) {
                defaultPosition = i;
                break;
            }
        }
        binding.spinnerRoom.setSelection(defaultPosition);
        selectedContractId = roomOptions.get(defaultPosition).contractId;
        loadPreviousReading();
    }

    private void loadPreviousReading() {
        String typeVal = binding.spinnerType.getSelectedItemPosition() == 0 ? "electric" : "water";

        binding.tvPreviousReading.setText("Đang tải chỉ số kỳ trước...");

        RetrofitClient.getInstance(this).getApi()
                .getPreviousReading(typeVal, selectedContractId)
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
                        } else {
                            // Trường hợp hiếm: danh sách phòng tải lỗi trước đó nên chưa
                            // chọn được phòng, mà tenant lại có nhiều phòng -> server từ
                            // chối (400) và yêu cầu chọn phòng. Báo rõ cho người dùng.
                            String msg = response.body() != null && response.body().getMessage() != null
                                    ? response.body().getMessage()
                                    : "Không tải được chỉ số kỳ trước";
                            binding.tvPreviousReading.setText(msg);
                            if (response.code() == 400) {
                                Toast.makeText(MeterReadingActivity.this,
                                        "Bạn đang thuê nhiều phòng — vui lòng thử lại để chọn phòng",
                                        Toast.LENGTH_LONG).show();
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
                        // Giới hạn độ phân giải chụp — nếu không set, CameraX mặc định chọn độ phân giải
                        // tối đa của cảm biến (vd 4096x3072 ~ 12MP), khiến decode/hiển thị ảnh cực nặng
                        // (dễ out-of-memory hoặc treo khi hiển thị 2 ImageView cùng lúc) và upload rất chậm.
                        .setTargetResolution(new Size(1600, 1200))
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
                        binding.btnCapture.setEnabled(true);
                        capturedImageUri = Uri.fromFile(capturedFile);
                        showCapturedImageAndScan(capturedFile, capturedImageUri);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        binding.btnCapture.setEnabled(true);
                        Log.e(TAG, "Chụp ảnh thất bại", e);
                        binding.tvOcrResult.setText("Chụp thất bại, thử lại"); // vẫn ở màn camera
                    }
                });
    }

    /**
     * Hiển thị ảnh (vừa chụp hoặc vừa chọn từ thư viện) rồi chuyển sang form nhập + gọi Gemini scan.
     * Dùng Glide thay vì ImageView.setImageURI() trực tiếp: setImageURI decode ảnh ở ĐỘ PHÂN GIẢI GỐC,
     * với ảnh camera lớn (vài nghìn x vài nghìn pixel) rất dễ out-of-memory hoặc treo UI, khiến ảnh
     * "không hiện ra". Glide tự downsample theo kích thước ImageView nên nhẹ và luôn hiển thị được.
     */
    private void showCapturedImageAndScan(File file, Uri displayUri) {
        capturedFile = file;
        capturedImageUri = displayUri;
        scannedImageUrl = null;
        scannedOcrRawText = null;

        Glide.with(this).load(displayUri).into(binding.ivCaptured);
        binding.ivCaptured.setVisibility(View.VISIBLE);

        // Chuyển sang form nhập — từ đây dùng tvOcrResultForm
        binding.layoutCamera.setVisibility(View.GONE);
        binding.layoutManual.setVisibility(View.VISIBLE);

        Glide.with(this).load(displayUri).into(binding.ivCapturedInForm);
        binding.ivCapturedInForm.setVisibility(View.VISIBLE);

        callScanApi(file);
    }

    /** Copy nội dung ảnh chọn từ thư viện (content://) ra 1 file thật trong cache để upload multipart. */
    private File copyUriToCacheFile(Uri uri) throws IOException {
        File out = new File(getCacheDir(), "meter_pick_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Không mở được ảnh đã chọn");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }
        return out;
    }

    private void callScanApi(File imageFile) {
        binding.tvOcrResultForm.setVisibility(View.VISIBLE);
        binding.tvOcrResultForm.setText("Gemini đang đọc số công tơ...");
        binding.btnSubmit.setEnabled(false);

        String typeVal = binding.spinnerType.getSelectedItemPosition() == 0 ? "electric" : "water";

        RequestBody rbType = RequestBody.create(typeVal, MediaType.parse("text/plain"));
        RequestBody rbContract = selectedContractId != null
                ? RequestBody.create(selectedContractId, MediaType.parse("text/plain")) : null;
        RequestBody rbImage = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part imagePart =
                MultipartBody.Part.createFormData("image", imageFile.getName(), rbImage);

        RetrofitClient.getInstance(this).getApi()
                .scanMeterImage(rbType, rbContract, imagePart)
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
                            Log.e(TAG, "Scan API trả lỗi: code=" + response.code()
                                    + " body=" + safeErrorBody(response));
                            binding.tvOcrResultForm.setText(
                                    "Không đọc được số bằng AI (server lỗi), vui lòng nhập tay số trên công tơ");
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<MeterScanResult>> call, Throwable t) {
                        binding.btnSubmit.setEnabled(true);
                        Log.e(TAG, "Lỗi kết nối khi gọi scan API", t);
                        binding.tvOcrResultForm.setText("Lỗi kết nối khi đọc ảnh, vui lòng nhập tay");
                    }
                });
    }

    private String safeErrorBody(Response<?> response) {
        try {
            return response.errorBody() != null ? response.errorBody().string() : "(rỗng)";
        } catch (IOException e) {
            return "(không đọc được error body)";
        }
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
        // Phòng đã chọn — đảm bảo chỉ số/hóa đơn được lưu đúng cho phòng đó,
        // không lẫn với hợp đồng khác của cùng tenant.
        RequestBody rbContract = selectedContractId != null
                ? RequestBody.create(selectedContractId, MediaType.parse("text/plain")) : null;

        if (scannedImageUrl != null) {
            RequestBody rbImageUrl = RequestBody.create(scannedImageUrl, MediaType.parse("text/plain"));
            RequestBody rbOcr = scannedOcrRawText != null
                    ? RequestBody.create(scannedOcrRawText, MediaType.parse("text/plain")) : null;

            RetrofitClient.getInstance(this).getApi()
                    .createMeterReadingWithUrl(rbType, rbReading, rbImageUrl, rbOcr, rbContract)
                    .enqueue(buildCreateCallback());
        } else if (capturedFile != null) {
            RequestBody rbImage = RequestBody.create(capturedFile, MediaType.parse("image/jpeg"));
            MultipartBody.Part imagePart =
                    MultipartBody.Part.createFormData("image", capturedFile.getName(), rbImage);

            RetrofitClient.getInstance(this).getApi()
                    .createMeterReading(rbType, rbReading, rbContract, imagePart)
                    .enqueue(buildCreateCallback());
        } else {
            // Nhập tay, không có ảnh — vẫn gửi được, chỉ cần bỏ trống phần ảnh.
            RetrofitClient.getInstance(this).getApi()
                    .createMeterReading(rbType, rbReading, rbContract, null)
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
                    String errorMsg = "Lưu thất bại";
                    try {
                        if (response.code() == 400) {
                            errorMsg = "Số chỉ số mới đang thấp hơn số cũ, vui lòng kiểm tra lại";
                        } else if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            org.json.JSONObject jObj = new org.json.JSONObject(errStr);
                            if (jObj.has("message")) {
                                errorMsg = jObj.getString("message");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error", e);
                    }
                    Toast.makeText(MeterReadingActivity.this,
                            errorMsg, Toast.LENGTH_LONG).show();
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