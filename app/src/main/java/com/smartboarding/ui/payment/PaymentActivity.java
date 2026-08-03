package com.smartboarding.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.PaymentResult;
import com.smartboarding.data.models.PaymentSessionData;
import com.smartboarding.databinding.ActivityPaymentBinding;
import com.smartboarding.utils.FormatUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

        Log.d(TAG, "invoiceId=" + invoiceId
                + " totalAmount=" + totalAmount
                + " paidAmount=" + paidAmount
                + " month=" + month
                + " year=" + year);

        if (invoiceId == null || invoiceId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin hóa đơn (invoiceId)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnConfirmPay.setOnClickListener(v -> confirmPayment());

        double remaining = totalAmount - paidAmount;

        if (remaining <= 0) {
            Toast.makeText(this, "Số tiền cần thanh toán không hợp lệ (0đ)."
                            + " Kiểm tra lại total_amount/paid_amount khi mở màn hình này.",
                    Toast.LENGTH_LONG).show();
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

        Log.d(TAG, "createPaymentSession body=" + body);

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
                            // Trước đây response.body() luôn null khi HTTP 400 → luôn rơi vào
                            // message mặc định "Không tạo được phiên thanh toán" dù server đã
                            // trả đúng lý do. Giờ đọc thẳng errorBody() để lấy message thật.
                            String msg = extractErrorMessage(response);
                            Log.e(TAG, "createPaymentSession failed: code=" + response.code() + " msg=" + msg);
                            Toast.makeText(PaymentActivity.this, msg, Toast.LENGTH_SHORT).show();

                            // Hóa đơn đã thanh toán rồi → không có gì để làm ở màn QR này,
                            // thoát luôn thay vì để người dùng đứng trước màn hình trống.
                            if (msg != null && msg.contains("đã được thanh toán")) {
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaymentSessionData>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "createPaymentSession network error", t);
                        Toast.makeText(PaymentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadQR(String qrUrl) {
        Glide.with(this)
                .load(qrUrl)
                .into(binding.ivQrCode);
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
                            if (isPolling) {
                                pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            }
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

        RetrofitClient.getInstance(this).getApi()
                .confirmPayment(payToken)
                .enqueue(new Callback<ApiResponse<PaymentResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaymentResult>> call,
                                           Response<ApiResponse<PaymentResult>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            isPolling = false;
                            showSuccess(response.body().getData());
                        } else {
                            binding.btnConfirmPay.setEnabled(true);
                            binding.btnConfirmPay.setText("Xác nhận đã thanh toán");
                            String msg = extractErrorMessage(response);
                            Toast.makeText(PaymentActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaymentResult>> call, Throwable t) {
                        // Request có thể bị timeout do server cold-start chậm (Render free tier),
                        // NHƯNG server vẫn có thể đã xử lý xong phía sau. Không báo lỗi gắt gao ở
                        // đây — polling (đang chạy song song mỗi 3s) sẽ tự phát hiện khi thành công
                        // và chuyển màn hình, nên không tắt isPolling ở nhánh này.
                        binding.btnConfirmPay.setEnabled(true);
                        binding.btnConfirmPay.setText("Xác nhận đã thanh toán");
                        Toast.makeText(PaymentActivity.this,
                                "Đang xử lý, vui lòng đợi giây lát...", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Retrofit trả response.body() = null khi HTTP status không phải 2xx.
    // Nội dung JSON thật của lỗi nằm ở response.errorBody() — phải đọc từ đó.
    private String extractErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JSONObject obj = new JSONObject(raw);
                return obj.optString("message", "Có lỗi xảy ra");
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
            // Báo cho màn hình đã mở PaymentActivity biết thanh toán đã xong,
            // để nó tự đóng/refresh thay vì hiển thị dữ liệu hóa đơn cũ.
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