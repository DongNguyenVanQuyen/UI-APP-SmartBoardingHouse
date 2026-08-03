package com.smartboarding.ui.maintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.smartboarding.databinding.FragmentMaintenanceBinding;
import com.smartboarding.viewmodel.DashboardViewModel;

public class MaintenanceFragment extends Fragment {
    private FragmentMaintenanceBinding binding;
    private DashboardViewModel viewModel;
    private MaintenanceAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMaintenanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        adapter = new MaintenanceAdapter(req -> {
            Intent intent = new Intent(requireContext(), MaintenanceDetailActivity.class);
            intent.putExtra("request_id", req.id);
            startActivity(intent);
        });

        binding.recyclerMaintenance.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMaintenance.setAdapter(adapter);

        viewModel.maintenanceRequests.observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.setData(list);
                binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadMaintenanceRequests());
        viewModel.loading.observe(getViewLifecycleOwner(), l -> binding.swipeRefresh.setRefreshing(l));
        viewModel.loadMaintenanceRequests();

        binding.fabCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateMaintenanceActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadMaintenanceRequests();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}