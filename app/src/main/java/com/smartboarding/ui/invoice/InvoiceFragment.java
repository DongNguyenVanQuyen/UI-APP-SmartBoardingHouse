package com.smartboarding.ui.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.smartboarding.databinding.FragmentInvoiceBinding;
import com.smartboarding.viewmodel.DashboardViewModel;

public class InvoiceFragment extends Fragment {
    private FragmentInvoiceBinding binding;
    private DashboardViewModel viewModel;
    private InvoiceAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvoiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        adapter = new InvoiceAdapter(invoice -> {
            Intent intent = new Intent(requireContext(), InvoiceDetailActivity.class);
            intent.putExtra("invoice_id", invoice.id);
            startActivity(intent);
        });

        binding.recyclerInvoices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerInvoices.setAdapter(adapter);

        viewModel.invoices.observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.setData(list);
                binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.loading.observe(getViewLifecycleOwner(), loading ->
                binding.swipeRefresh.setRefreshing(loading));

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadInvoices());
        viewModel.loadInvoices();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadInvoices();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}