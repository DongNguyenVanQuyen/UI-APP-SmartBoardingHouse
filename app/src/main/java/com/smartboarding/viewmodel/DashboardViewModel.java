package com.smartboarding.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    // Danh sách hợp đồng của tenant — dùng để hiển thị bộ lọc theo phòng/hợp đồng
    // ở màn hình hóa đơn (1 tenant có thể có nhiều hợp đồng).
    public final MutableLiveData<List<Contract>> contracts = new MutableLiveData<>();
    // Danh sách phòng CHỈ theo hợp đồng còn hiệu lực (active) — dùng cho bộ
    // lọc/chuyển phòng ở màn Hóa đơn. Không dùng "contracts" ở trên nữa vì nó
    // trả về mọi trạng thái hợp đồng (kể cả đã hủy/hết hạn), gây trùng phòng.
    public final MutableLiveData<List<RoomOption>> invoiceRooms = new MutableLiveData<>();
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

    // Chuyển "phòng đang chọn" (dựa vào hợp đồng — bắt buộc hợp đồng còn hiệu
    // lực). Dùng chung cho nút "Chuyển phòng" ở Dashboard: sau khi chuyển,
    // backend trả về ngay dashboard mới nhất theo phòng vừa chọn.
    public void selectDashboardRoom(String contractId) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("contractId", contractId);
        api.selectDashboardRoom(body).enqueue(new Callback<ApiResponse<DashboardData>>() {
            @Override
            public void onResponse(Call<ApiResponse<DashboardData>> call, Response<ApiResponse<DashboardData>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    dashboard.setValue(response.body().getData());
                } else {
                    error.setValue("Không thể chuyển phòng — hợp đồng không hợp lệ hoặc đã hết hiệu lực");
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
        loadInvoices(null);
    }

    // contractId == null -> tải TẤT CẢ hóa đơn của tenant (mọi hợp đồng).
    // contractId != null -> chỉ tải hóa đơn của riêng hợp đồng/phòng đó
    // (dùng cho bộ lọc trên màn hình danh sách hóa đơn).
    public void loadInvoices(@Nullable String contractId) {
        api.getInvoicesFiltered(contractId, null).enqueue(new Callback<ApiResponse<List<Invoice>>>() {
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

    // Tải danh sách hợp đồng để hiển thị bộ lọc theo phòng/hợp đồng.
    public void loadContracts() {
        api.getContracts().enqueue(new Callback<ApiResponse<List<Contract>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Contract>>> call, Response<ApiResponse<List<Contract>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    contracts.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Contract>>> call, Throwable t) {
                // Không chặn màn hình hóa đơn nếu tải hợp đồng lỗi — chỉ đơn giản
                // là không hiển thị được bộ lọc.
            }
        });
    }

    // Danh sách phòng CHỈ theo hợp đồng còn hiệu lực — dùng cho bộ lọc/chuyển
    // phòng ở màn Hóa đơn (thay cho loadContracts() cũ vốn trả về mọi trạng
    // thái hợp đồng và gây trùng phòng khi có hợp đồng đã hủy/hết hạn).
    public void loadInvoiceRooms() {
        api.getInvoiceRooms().enqueue(new Callback<ApiResponse<List<RoomOption>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RoomOption>>> call, Response<ApiResponse<List<RoomOption>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    invoiceRooms.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<RoomOption>>> call, Throwable t) {
                // Không chặn màn hình hóa đơn nếu tải danh sách phòng lỗi.
            }
        });
    }

    // Chuyển phòng đang chọn ngay tại màn Hóa đơn (hợp đồng phải còn hiệu
    // lực) — dùng chung "phòng đang chọn" với Dashboard/chụp công tơ.
    public void selectInvoiceRoom(String contractId) {
        loading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("contractId", contractId);
        api.selectInvoiceRoom(body).enqueue(new Callback<ApiResponse<List<Invoice>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Invoice>>> call, Response<ApiResponse<List<Invoice>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    invoices.setValue(response.body().getData());
                    loadInvoiceRooms(); // reload để cập nhật cờ isSelected
                } else {
                    error.setValue("Không thể chuyển phòng — hợp đồng không hợp lệ hoặc đã hết hiệu lực");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Invoice>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Lỗi kết nối: " + t.getMessage());
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