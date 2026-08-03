package com.smartboarding.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel {
    private final ApiService api;

    public final MutableLiveData<DashboardData> dashboard = new MutableLiveData<>();
    public final MutableLiveData<List<Invoice>> invoices = new MutableLiveData<>();
    public final MutableLiveData<List<MaintenanceRequest>> maintenanceRequests = new MutableLiveData<>();
    public final MutableLiveData<NotificationData> notifications = new MutableLiveData<>();
    public final MutableLiveData<DebtData> debts = new MutableLiveData<>();
    public final MutableLiveData<RoomData> currentRoom = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        api = RetrofitClient.getInstance(application).getApi();
    }

    public void loadDashboard() {
        loading.setValue(true);
        api.getDashboard().enqueue(new Callback<ApiResponse<DashboardData>>() {
            @Override
            public void onResponse(Call<ApiResponse<DashboardData>> call, Response<ApiResponse<DashboardData>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    dashboard.setValue(response.body().getData());
                } else {
                    error.setValue("Không thể tải dashboard");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<DashboardData>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void loadInvoices() {
        api.getInvoices().enqueue(new Callback<ApiResponse<List<Invoice>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Invoice>>> call, Response<ApiResponse<List<Invoice>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    invoices.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Invoice>>> call, Throwable t) {
                error.setValue("Lỗi tải hóa đơn");
            }
        });
    }

    public void loadMaintenanceRequests() {
        api.getMaintenanceRequests().enqueue(new Callback<ApiResponse<List<MaintenanceRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MaintenanceRequest>>> call, Response<ApiResponse<List<MaintenanceRequest>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    maintenanceRequests.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<MaintenanceRequest>>> call, Throwable t) {
                error.setValue("Lỗi tải yêu cầu sửa chữa");
            }
        });
    }

    public void loadNotifications() {
        api.getNotifications(1, 10).enqueue(new Callback<ApiResponse<NotificationData>>() {
            @Override
            public void onResponse(Call<ApiResponse<NotificationData>> call, Response<ApiResponse<NotificationData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    notifications.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<NotificationData>> call, Throwable t) {}
        });
    }

    public void loadCurrentRoom() {
        api.getCurrentRoom().enqueue(new Callback<ApiResponse<RoomData>>() {
            @Override
            public void onResponse(Call<ApiResponse<RoomData>> call, Response<ApiResponse<RoomData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentRoom.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<RoomData>> call, Throwable t) {}
        });
    }

    public void loadDebts() {
        api.getDebts().enqueue(new Callback<ApiResponse<DebtData>>() {
            @Override
            public void onResponse(Call<ApiResponse<DebtData>> call, Response<ApiResponse<DebtData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    debts.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<DebtData>> call, Throwable t) {}
        });
    }

    // ⚠️ DEBUG ONLY — gọi API xóa Invoice/Payment/MeterReading của tenant hiện tại
    // cho 1 tháng cụ thể (mặc định tháng/năm hiện tại), để test lại luồng "hóa đơn mới".
    // Nhớ gỡ nút gọi hàm này trước khi release.
    public void debugAdvanceMonth() {
        loading.setValue(true);
        Map<String, Object> body = new HashMap<>(); // rỗng = backend tự dùng tháng/năm hiện tại
        api.clearMonthData(body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    loadDashboard(); // load lại để thấy dashboard đã reset về "chưa có hóa đơn"
                } else {
                    error.setValue("Không thể xóa dữ liệu tháng test");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}