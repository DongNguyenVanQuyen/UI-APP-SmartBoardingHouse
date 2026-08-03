package com.smartboarding.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.AuthData;
import com.smartboarding.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {
    private final ApiService api;
    private final SessionManager session;

    public final MutableLiveData<AuthData> authResult = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> logoutResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> otpSent = new MutableLiveData<>();
    public final MutableLiveData<Boolean> resetSuccess = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        api = RetrofitClient.getInstance(application).getApi();
        session = SessionManager.getInstance(application);
    }

    public void login(String email, String password) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        api.login(body).enqueue(new Callback<ApiResponse<AuthData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthData>> call, Response<ApiResponse<AuthData>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthData data = response.body().getData();
                    session.saveAuthData(
                            data.accessToken, data.refreshToken,
                            data.tenant.id, data.tenant.fullName, data.tenant.email
                    );
                    if (data.tenant.avatar != null) session.saveAvatar(data.tenant.avatar);
                    authResult.setValue(data);
                } else {
                    try {
                        errorMessage.setValue(response.body() != null ? response.body().getMessage() : "Đăng nhập thất bại");
                    } catch (Exception e) {
                        errorMessage.setValue("Lỗi kết nối");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthData>> call, Throwable t) {
                loading.setValue(false);
                errorMessage.setValue("Không thể kết nối server: " + t.getMessage());
            }
        });
    }

    public void register(String fullName, String email, String phone, String password) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("fullName", fullName);
        body.put("email", email);
        body.put("phone", phone);
        body.put("password", password);

        api.register(body).enqueue(new Callback<ApiResponse<AuthData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthData>> call, Response<ApiResponse<AuthData>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthData data = response.body().getData();
                    session.saveAuthData(data.accessToken, data.refreshToken,
                            data.tenant.id, data.tenant.fullName, data.tenant.email);
                    authResult.setValue(data);
                } else {
                    errorMessage.setValue(response.body() != null ? response.body().getMessage() : "Đăng ký thất bại");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthData>> call, Throwable t) {
                loading.setValue(false);
                errorMessage.setValue("Không thể kết nối server");
            }
        });
    }

    public void logout() {
        api.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                session.logout();
                logoutResult.setValue(true);
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                session.logout();
                logoutResult.setValue(true);
            }
        });
    }

    public void changePassword(String current, String newPass) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", current);
        body.put("newPassword", newPass);
        api.changePassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    errorMessage.setValue("SUCCESS");
                } else {
                    errorMessage.setValue(response.body() != null ? response.body().getMessage() : "Thất bại");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                loading.setValue(false);
                errorMessage.setValue("Lỗi kết nối");
            }
        });
    }

    public void sendOtp(String email) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.forgotPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    otpSent.setValue(true);
                } else {
                    errorMessage.setValue(response.body() != null ? response.body().getMessage() : "Không gửi được mã OTP");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                loading.setValue(false);
                errorMessage.setValue("Không thể kết nối server");
            }
        });
    }

    public void verifyOtpAndReset(String email, String otp, String newPassword) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);
        body.put("newPassword", newPassword);

        api.verifyOtpAndResetPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    resetSuccess.setValue(true);
                } else {
                    errorMessage.setValue(response.body() != null ? response.body().getMessage() : "OTP không đúng hoặc đã hết hạn");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                loading.setValue(false);
                errorMessage.setValue("Không thể kết nối server");
            }
        });
    }
}