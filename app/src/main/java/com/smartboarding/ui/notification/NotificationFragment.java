package com.smartboarding.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.smartboarding.R;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.Notification;
import com.smartboarding.data.models.NotificationData;
import com.smartboarding.ui.invoice.InvoiceDetailActivity;       // TODO: sửa đúng package/tên thật nếu khác
import com.smartboarding.ui.maintenance.MaintenanceDetailActivity; // TODO: sửa đúng package/tên thật nếu khác
import com.smartboarding.ui.message.ChatActivity;             // dùng nếu bạn mở chat trong cùng Activity qua Fragment transaction

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty, progressBar;
    private TextView tvMarkAllRead;

    private NotificationAdapter adapter;
    private ApiService api;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);
        tvMarkAllRead = view.findViewById(R.id.tvMarkAllRead);

        api = RetrofitClient.getInstance(requireContext()).getApi();

        adapter = new NotificationAdapter((item, position) -> {
            if (!item.isRead) markAsRead(item.id);
            handleNavigation(item);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);

        rvNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null || isLoading || !hasMore) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 3) {
                    currentPage++;
                    loadNotifications(false);
                }
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadNotifications(true);
        });

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());

        loadNotifications(true);
    }

    private void loadNotifications(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        if (reset && currentPage == 1) progressBar.setVisibility(View.VISIBLE);

        api.getNotifications(currentPage, 10).enqueue(new Callback<ApiResponse<NotificationData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<NotificationData>> call, @NonNull Response<ApiResponse<NotificationData>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Notification> list = response.body().getData().notifications;
                    if (list == null) list = new ArrayList<>();

                    if (list.isEmpty()) hasMore = false;

                    if (reset) adapter.setItems(list);
                    else adapter.appendItems(list);

                    updateEmptyState();
                    updateMarkAllVisibility();
                } else {
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<NotificationData>> call, @NonNull Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                updateEmptyState();
                // Log rõ nguyên nhân thật (timeout, parse lỗi, mất mạng...) để debug thay vì chỉ biết "thất bại"
                Log.e("NotificationFragment", "Lỗi tải thông báo", t);
                Toast.makeText(requireContext(), "Không tải được thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateMarkAllVisibility() {
        tvMarkAllRead.setVisibility(adapter.getUnreadCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void markAsRead(String id) {
        adapter.markReadLocally(id);
        updateMarkAllVisibility();

        Map<String, Object> body = new HashMap<>();
        body.put("notificationIds", Collections.singletonList(id));

        api.markNotificationsRead(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                // đã cập nhật local, không cần làm gì thêm
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                // optimistic update — có thể bỏ qua rollback vì đây chỉ là trạng thái đọc, không ảnh hưởng dữ liệu quan trọng
            }
        });
    }

    private void markAllAsRead() {
        if (adapter.getUnreadCount() == 0) return;

        adapter.markAllReadLocally();
        updateMarkAllVisibility();

        Map<String, Object> body = new HashMap<>();
        body.put("all", true);

        api.markNotificationsRead(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                Toast.makeText(requireContext(), "Đã đánh dấu tất cả đã đọc", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Có lỗi xảy ra, thử lại sau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── ĐIỀU HƯỚNG THEO refModel ─────────────────
    private void handleNavigation(Notification item) {
        if (item.refModel == null) {
            openGenericDetail(item);
            return;
        }

        switch (item.refModel) {
            case "MaintenanceRequest":
                if (item.refId != null) {
                    Intent intent = new Intent(requireContext(), MaintenanceDetailActivity.class);
                    intent.putExtra("request_id", item.refId);
                    startActivity(intent);
                } else {
                    openGenericDetail(item);
                }
                break;

            case "Invoice":
                if (item.refId != null) {
                    Intent intent = new Intent(requireContext(), InvoiceDetailActivity.class);
                    intent.putExtra("invoice_id", item.refId);
                    startActivity(intent);
                } else {
                    openGenericDetail(item);
                }
                break;

            case "Message":
            case "Conversation":
                startActivity(new Intent(requireContext(), ChatActivity.class));
                break;

            default:
                // Booking và mọi refModel khác chưa có màn hình riêng -> hiện chi tiết chung
                openGenericDetail(item);
                break;
        }
    }

    private void openGenericDetail(Notification item) {
        Intent intent = new Intent(requireContext(), NotificationDetailActivity.class);
        intent.putExtra(NotificationDetailActivity.EXTRA_TITLE, item.title);
        intent.putExtra(NotificationDetailActivity.EXTRA_BODY, item.body);
        intent.putExtra(NotificationDetailActivity.EXTRA_TIME, formatFullTime(item.createdAt));
        if (item.meta != null) {
            intent.putExtra(NotificationDetailActivity.EXTRA_META_JSON, new Gson().toJson(item.meta));
        }
        startActivity(intent);
    }

    private String formatFullTime(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat out = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            return out.format(in.parse(iso));
        } catch (ParseException e) {
            return iso;
        }
    }
}