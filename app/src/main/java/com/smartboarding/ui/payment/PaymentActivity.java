package com.smartboarding.ui.payment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.PaymentResult;
import com.smartboarding.data.models.PaymentSessionData;
import com.smartboarding.databinding.ActivityPaymentBinding;
import com.smartboarding.utils.FormatUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";

    private ActivityPaymentBinding binding;
    private String invoiceId;
    private double totalAmount, paidAmount;

    private String payToken;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final int POLL_INTERVAL_MS = 3000;
    private boolean isPolling = false;

    // Biến lưu trữ URI ảnh minh chứng thanh toán
    private Uri receiptUri = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickReceipt =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    receiptUri = uri;
                    binding.ivReceipt.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(binding.ivReceipt);
                    binding.btnSelectReceipt.setText("Đổi ảnh khác");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        invoiceId   = getIntent().getStringExtra("invoice_id");
        totalAmount = getIntent().getDoubleExtra("total_amount", 0);
        paidAmount  = getIntent().getDoubleExtra("paid_amount", 0);
        int month   = getIntent().getIntExtra("month", 0);
        int year    = getIntent().getIntExtra("year", 0);

        if (invoiceId == null || invoiceId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin hóa đơn (invoiceId)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnConfirmPay.setOnClickListener(v -> confirmPayment());

        // Sự kiện nút đính kèm ảnh minh chứng
        binding.btnSelectReceipt.setOnClickListener(v -> {
            pickReceipt.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        double remaining = totalAmount - paidAmount;

        if (remaining <= 0) {
            Toast.makeText(this, "Số tiền cần thanh toán không hợp lệ (0đ).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.tvAmount.setText(FormatUtils.formatCurrency(remaining));
        binding.tvInvoiceInfo.setText("Hóa đơn Tháng " + month + "/" + year);
        binding.tvRemainingLabel.setText("Số tiền cần thanh toán: " + FormatUtils.formatCurrency(remaining));

        createPaymentSession(remaining);
    }

    private void createPaymentSession(double amount) {
        binding.progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> body = new HashMap<>();
        body.put("invoiceId", invoiceId);
        body.put("amount", amount);
        body.put("method", "qr");

        RetrofitClient.getInstance(this).getApi()
                .createPaymentSession(body)
                .enqueue(new Callback<ApiResponse<PaymentSessionData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaymentSessionData>> call,
                                           Response<ApiResponse<PaymentSessionData>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PaymentSessionData data = response.body().getData();
                            payToken = data.payToken;
                            loadQR(data.qrUrl);
                            startPolling();
                        } else {
                            String msg = extractErrorMessage(response);
                            Toast.makeText(PaymentActivity.this, msg, Toast.LENGTH_SHORT).show();
                            if (msg != null && msg.contains("đã được thanh toán")) {
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaymentSessionData>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(PaymentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadQR(String qrUrl) {
        Glide.with(this).load(qrUrl).into(binding.ivQrCode);
        binding.tvQrContent.setText("Quét mã bằng app ngân hàng để chuyển khoản");
    }

    private void startPolling() {
        if (isPolling || payToken == null) return;
        isPolling = true;

        pollRunnable = () -> {
            RetrofitClient.getInstance(this).getApi()
                    .getPaymentStatus(payToken)
                    .enqueue(new Callback<ApiResponse<PaymentResult>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<PaymentResult>> call,
                                               Response<ApiResponse<PaymentResult>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                PaymentResult result = response.body().getData();
                                if ("success".equals(result.payment.status)) {
                                    isPolling = false;
                                    showSuccess(result);
                                    return;
                                }
                            }
                            if (isPolling) {
                                pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<PaymentResult>> call, Throwable t) {
                            if (isPolling) pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                        }
                    });
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void confirmPayment() {
        if (payToken == null) {
            Toast.makeText(this, "Chưa có phiên thanh toán để xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnConfirmPay.setEnabled(false);
        binding.btnConfirmPay.setText("Đang xác nhận...");

        Callback<ApiResponse<PaymentResult>> confirmCallback = new Callback<ApiResponse<PaymentResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentResult>> call, Response<ApiResponse<PaymentResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    isPolling = false;
                    showSuccess(response.body().getData());
                } else {
                    binding.btnConfirmPay.setEnabled(true);
                    binding.btnConfirmPay.setText("Xác nhận đã thanh toán");
                    Toast.makeText(PaymentActivity.this, extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentResult>> call, Throwable t) {
                binding.btnConfirmPay.setEnabled(true);
                binding.btnConfirmPay.setText("Xác nhận đã thanh toán");
                Toast.makeText(PaymentActivity.this, "Đang xử lý, vui lòng đợi giây lát...", Toast.LENGTH_SHORT).show();
            }
        };

        // Nếu người dùng chọn đính kèm ảnh minh chứng
        if (receiptUri != null) {
            File file = copyUriToTempFile(receiptUri, "receipt");
            if (file != null) {
                RequestBody rb = RequestBody.create(file, MediaType.parse("image/*"));
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("receiptImage", file.getName(), rb);
                RetrofitClient.getInstance(this).getApi().confirmPaymentWithReceipt(payToken, imagePart).enqueue(confirmCallback);
                return;
            }
        }

        // Nếu không đính kèm ảnh
        RetrofitClient.getInstance(this).getApi().confirmPayment(payToken).enqueue(confirmCallback);
    }

    private File copyUriToTempFile(Uri uri, String prefix) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File outFile = new File(getCacheDir(), prefix + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                return new JSONObject(raw).optString("message", "Có lỗi xảy ra");
            }
        } catch (Exception e) {
            Log.e(TAG, "parse error body failed", e);
        }
        return "Có lỗi xảy ra";
    }

    private void showSuccess(PaymentResult result) {
        binding.layoutQr.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.VISIBLE);
        binding.tvSuccessAmount.setText(FormatUtils.formatCurrency(result.payment.amount));
        binding.tvTransactionId.setText("Mã GD: " + result.payment.transactionId);
        binding.btnDone.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("invoice_id", invoiceId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPolling = false;
        if (pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
    }
}