package com.smartboarding.ui.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.Invoice;
import com.smartboarding.data.models.RoomOption;
import com.smartboarding.databinding.FragmentInvoiceBinding;
import com.smartboarding.viewmodel.DashboardViewModel;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceFragment extends Fragment {
    private FragmentInvoiceBinding binding;
    private DashboardViewModel viewModel;
    private InvoiceAdapter adapter;
    private ApiService api;

    private final List<RoomOption> roomOptions = new ArrayList<>();
    private String selectedContractId = null;
    private boolean suppressSpinnerCallback = false;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvoiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        api = RetrofitClient.getInstance(requireContext()).getApi();

        adapter = new InvoiceAdapter(invoice -> {
            Intent intent = new Intent(requireContext(), InvoiceDetailActivity.class);
            intent.putExtra("invoice_id", invoice.id);
            startActivity(intent);
        });

        binding.recyclerInvoices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerInvoices.setAdapter(adapter);

        binding.recyclerInvoices.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null || isLoading || !hasMore) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 3) {
                    currentPage++;
                    loadInvoices(false);
                }
            }
        });

        viewModel.invoiceRooms.observe(getViewLifecycleOwner(), this::setupRoomFilter);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadInvoices(true);
            viewModel.loadInvoiceRooms();
        });

        viewModel.loadInvoiceRooms();
        loadInvoices(true);
    }

    private void loadInvoices(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        binding.swipeRefresh.setRefreshing(true);

        api.getInvoicesFiltered(selectedContractId, null, currentPage, 10).enqueue(new Callback<ApiResponse<List<Invoice>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Invoice>>> call, Response<ApiResponse<List<Invoice>>> response) {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Invoice> list = response.body().getData();
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
            public void onFailure(Call<ApiResponse<List<Invoice>>> call, Throwable t) {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi tải hóa đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRoomFilter(List<RoomOption> rooms) {
        if (rooms == null || rooms.size() < 2) {
            binding.layoutContractFilter.setVisibility(View.GONE);
            return;
        }

        roomOptions.clear();
        roomOptions.addAll(rooms);

        List<String> labels = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < roomOptions.size(); i++) {
            RoomOption r = roomOptions.get(i);
            if (r.contractId != null && r.contractId.equals("all")) {
                labels.add(r.roomNumber); // "Tất cả phòng"
            } else {
                labels.add("Phòng " + r.roomNumber);
            }
            if (r.isSelected && selectedContractId == null) {
                selectedIndex = i;
                selectedContractId = r.contractId;
            } else if (java.util.Objects.equals(selectedContractId, r.contractId)) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerContractFilter.setAdapter(spinnerAdapter);

        suppressSpinnerCallback = true;
        binding.spinnerContractFilter.setSelection(selectedIndex, false);
        suppressSpinnerCallback = false;

        binding.spinnerContractFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallback) return;

                RoomOption chosen = roomOptions.get(position);
                if (!java.util.Objects.equals(chosen.contractId, selectedContractId)) {
                    selectedContractId = chosen.contractId;
                    currentPage = 1;
                    hasMore = true;
                    loadInvoices(true);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.layoutContractFilter.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        currentPage = 1;
        hasMore = true;
        loadInvoices(true);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}