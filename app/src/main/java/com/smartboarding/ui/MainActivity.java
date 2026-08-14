package com.smartboarding.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.messaging.FirebaseMessaging;
import com.smartboarding.R;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.databinding.ActivityMainBinding;
import com.smartboarding.ui.dashboard.DashboardFragment;
import com.smartboarding.ui.invoice.InvoiceFragment;
import com.smartboarding.ui.maintenance.MaintenanceFragment;
import com.smartboarding.ui.notification.NotificationFragment;
import com.smartboarding.ui.profile.ProfileFragment;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();
        registerFcmToken();

        // Nếu mở app từ khi bấm vào push notification -> mở thẳng tab Thông báo
        String openTab = getIntent().getStringExtra("open_tab");
        if ("notifications".equals(openTab)) {
            binding.bottomNav.setSelectedItemId(R.id.nav_notification);
        } else {
            loadFragment(new DashboardFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_invoice) {
                fragment = new InvoiceFragment();
            } else if (id == R.id.nav_maintenance) {
                fragment = new MaintenanceFragment();
            } else if (id == R.id.nav_notification) {
                fragment = new NotificationFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new DashboardFragment();
            }
            loadFragment(fragment);
            return true;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Quan trọng: khi MainActivity đã đang chạy sẵn (app ở background),
        // Android sẽ gọi onNewIntent() thay vì onCreate() khi bấm vào push notification.
        // Nếu không cập nhật intent + xử lý ở đây, extra "open_tab" sẽ bị bỏ qua
        // và app không tự chuyển sang tab Thông báo như mong muốn.
        setIntent(intent);

        String openTab = intent.getStringExtra("open_tab");
        if ("notifications".equals(openTab) && binding != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_notification);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();

                    ApiService api = RetrofitClient.getInstance(getApplicationContext()).getApi();
                    Map<String, String> body = new HashMap<>();
                    body.put("fcmToken", token);

                    api.updateFcmToken(body).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                    });
                });
    }
}