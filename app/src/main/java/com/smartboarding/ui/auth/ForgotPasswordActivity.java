package com.smartboarding.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.databinding.ActivityForgotPasswordBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private ApiService api;

    private boolean isOtpStep = false;
    private String submittedEmail;

    private static final Pattern EMAIL_PATTERN = Patterns_EMAIL();
    private android.os.CountDownTimer resendTimer;
    private int originalResendColor;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = RetrofitClient.getInstance(this).getApi();
        originalResendColor = binding.tvResendOtp.getCurrentTextColor();


        binding.btnBack.setOnClickListener(v -> {
            if (isOtpStep) {
                // Quay lại bước nhập email thay vì thoát hẳn
                showEmailStep();
            } else {
                finish();
            }
        });

        binding.btnAction.setOnClickListener(v -> {
            if (isOtpStep) {
                verifyOtpAndReset();
            } else {
                sendOtp();
            }
        });

        binding.tvResendOtp.setOnClickListener(v -> sendOtp());
    }

    private static Pattern Patterns_EMAIL() {
        return android.util.Patterns.EMAIL_ADDRESS;
    }

    // ─── Bước 1: Gửi OTP ─────────────────────────────────
    private void sendOtp() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";

        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(this, "Vui lòng nhập email hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.forgotPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    submittedEmail = email;
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Mã OTP đã được gửi tới email của bạn", Toast.LENGTH_LONG).show();
                    showOtpStep();
                    startResendTimer();

                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Không gửi được mã OTP, thử lại sau", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Bước 2: Xác thực OTP + đặt mật khẩu mới ─────────
    private void verifyOtpAndReset() {
        String otp = binding.etOtp.getText() != null
                ? binding.etOtp.getText().toString().trim() : "";
        String newPassword = binding.etNewPassword.getText() != null
                ? binding.etNewPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null
                ? binding.etConfirmPassword.getText().toString().trim() : "";

        if (otp.isEmpty() || otp.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng xác nhận mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("email", submittedEmail);
        body.put("otp", otp);
        body.put("newPassword", newPassword);

        api.verifyOtpAndResetPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Đặt lại mật khẩu thành công, vui lòng đăng nhập lại",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "OTP không đúng hoặc đã hết hạn";
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOtpStep() {
        isOtpStep = true;
        binding.layoutStepEmail.setVisibility(View.GONE);
        binding.layoutStepOtp.setVisibility(View.VISIBLE);
        binding.tvOtpInfo.setText("Mã OTP đã được gửi tới " + submittedEmail);
        binding.btnAction.setText("Đặt lại mật khẩu");
    }

    private void showEmailStep() {
        isOtpStep = false;
        binding.layoutStepOtp.setVisibility(View.GONE);
        binding.layoutStepEmail.setVisibility(View.VISIBLE);
        binding.btnAction.setText("Gửi mã OTP");
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnAction.setEnabled(!loading);
    }

    private void startResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }

        binding.tvResendOtp.setEnabled(false);
        binding.tvResendOtp.setTextColor(android.graphics.Color.GRAY);

        resendTimer = new android.os.CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvResendOtp.setText("Gửi lại sau " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                binding.tvResendOtp.setEnabled(true);
                binding.tvResendOtp.setText("Gửi lại mã OTP");
                binding.tvResendOtp.setTextColor(originalResendColor);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}