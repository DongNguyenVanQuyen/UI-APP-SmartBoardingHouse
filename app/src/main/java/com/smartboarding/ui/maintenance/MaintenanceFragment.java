package com.smartboarding.ui.maintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.MaintenanceRequest;
import com.smartboarding.databinding.FragmentMaintenanceBinding;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MaintenanceFragment extends Fragment {
    private FragmentMaintenanceBinding binding;
    private MaintenanceAdapter adapter;
    private ApiService api;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMaintenanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = RetrofitClient.getInstance(requireContext()).getApi();

        adapter = new MaintenanceAdapter(req -> {
            Intent intent = new Intent(requireContext(), MaintenanceDetailActivity.class);
            intent.putExtra("request_id", req.id);
            startActivity(intent);
        });

        binding.recyclerMaintenance.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMaintenance.setAdapter(adapter);

        binding.recyclerMaintenance.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null || isLoading || !hasMore) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 3) {
                    currentPage++;
                    loadRequests(false);
                }
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadRequests(true);
        });

        binding.fabCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateMaintenanceActivity.class)));

        loadRequests(true);
    }

    private void loadRequests(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        binding.swipeRefresh.setRefreshing(true);

        api.getMaintenanceRequests(currentPage, 10).enqueue(new Callback<ApiResponse<List<MaintenanceRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MaintenanceRequest>>> call, Response<ApiResponse<List<MaintenanceRequest>>> response) {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<MaintenanceRequest> list = response.body().getData();
                    if (list == null) list = new ArrayList<>();

                    if (list.size() < 10) hasMore = false;

                    if (reset) {
                        adapter.setData(list);
                    } else {
                        adapter.appendData(list);
                    }

                    binding.tvEmpty.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MaintenanceRequest>>> call, Throwable t) {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi tải yêu cầu sửa chữa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        currentPage = 1;
        hasMore = true;
        loadRequests(true);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}