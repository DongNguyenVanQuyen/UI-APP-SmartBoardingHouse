package com.smartboarding.ui.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.smartboarding.data.models.RoomOption;
import com.smartboarding.databinding.FragmentInvoiceBinding;
import com.smartboarding.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

public class InvoiceFragment extends Fragment {
    private FragmentInvoiceBinding binding;
    private DashboardViewModel viewModel;
    private InvoiceAdapter adapter;

    // index 0 = "Tất cả phòng" (chỉ lọc hiển thị, KHÔNG đổi phòng đang chọn);
    // các index còn lại tương ứng 1-1 với danh sách phòng trong roomOptions —
    // CHỈ gồm hợp đồng còn hiệu lực (active), lấy từ GET /invoices/rooms
    // (không dùng getContracts() nữa vì trả về mọi trạng thái hợp đồng, gây
    // trùng phòng khi có hợp đồng đã hủy/hết hạn).
    private final List<RoomOption> roomOptions = new ArrayList<>();
    private String selectedContractId = null;
    // Chặn callback onItemSelected tự bắn lại khi ta chủ động setSelection()
    // sau khi danh sách phòng được tải/làm mới.
    private boolean suppressSpinnerCallback = false;

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

        viewModel.invoiceRooms.observe(getViewLifecycleOwner(), this::setupRoomFilter);

        viewModel.loading.observe(getViewLifecycleOwner(), loading ->
                binding.swipeRefresh.setRefreshing(loading));

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadInvoices(selectedContractId);
            viewModel.loadInvoiceRooms();
        });

        viewModel.loadInvoiceRooms();
        viewModel.loadInvoices(selectedContractId);
    }

    // Chỉ hiển thị bộ lọc khi tenant có từ 2 phòng (hợp đồng active) trở lên
    // — với 1 phòng thì lọc không có ý nghĩa và chỉ làm rối giao diện.
    // roomOptions CHỈ gồm hợp đồng còn hiệu lực nên không còn hiện phòng của
    // hợp đồng đã hủy/hết hạn (vd. không còn trùng "Phòng P202" 2 lần).
    private void setupRoomFilter(List<RoomOption> rooms) {
        if (rooms == null || rooms.size() < 2) {
            binding.layoutContractFilter.setVisibility(View.GONE);
            return;
        }

        roomOptions.clear();
        roomOptions.addAll(rooms);

        List<String> labels = new ArrayList<>();
        labels.add("Chọn Phòng");
        int selectedIndex = 0; // 0 = "Tất cả phòng" nếu chưa xác định được phòng đang chọn
        for (int i = 0; i < roomOptions.size(); i++) {
            RoomOption r = roomOptions.get(i);
            labels.add("Phòng " + r.roomNumber);
            if (r.isSelected && selectedContractId == null) {
                // Lần đầu vào màn: tự chọn đúng phòng đang chọn ở Dashboard.
                selectedIndex = i + 1;
                selectedContractId = r.contractId;
            } else if (java.util.Objects.equals(selectedContractId, r.contractId)) {
                selectedIndex = i + 1;
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

                if (position == 0) {
                    // "Tất cả phòng": chỉ lọc hiển thị, KHÔNG đổi phòng đang chọn.
                    selectedContractId = null;
                    viewModel.loadInvoices(null);
                    return;
                }

                RoomOption chosen = roomOptions.get(position - 1);
                if (!java.util.Objects.equals(chosen.contractId, selectedContractId)) {
                    selectedContractId = chosen.contractId;
                    // Chuyển phòng thật sự (đồng bộ với Dashboard/chụp công tơ),
                    // không chỉ lọc hiển thị — đúng yêu cầu "chuyển phòng ở trong invoice".
                    viewModel.selectInvoiceRoom(chosen.contractId);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.layoutContractFilter.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadInvoices(selectedContractId);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}