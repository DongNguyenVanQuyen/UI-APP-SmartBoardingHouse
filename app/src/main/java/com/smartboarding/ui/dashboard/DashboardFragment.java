package com.smartboarding.ui.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.smartboarding.R;
import com.smartboarding.data.models.DashboardData;
import com.smartboarding.data.models.Invoice;
import com.smartboarding.data.models.RoomOption;
import com.smartboarding.databinding.FragmentDashboardBinding;
import com.smartboarding.ui.message.ChatActivity;
import com.smartboarding.ui.invoice.InvoiceDetailActivity;
import com.smartboarding.ui.meter.MeterReadingActivity;
import com.smartboarding.ui.payment.PaymentActivity;
import com.smartboarding.utils.FormatUtils;
import com.smartboarding.utils.SessionManager;
import com.smartboarding.viewmodel.DashboardViewModel;

import java.util.List;

public class DashboardFragment extends Fragment {
    private static final int REQ_PAYMENT = 1001;

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupObservers();
        setupClickListeners();
        viewModel.loadDashboard();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadDashboard();
            binding.swipeRefresh.setRefreshing(false);
        });
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_primary, R.color.blue_primary);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PAYMENT && resultCode == Activity.RESULT_OK) {
            // Thanh toán vừa xong ở PaymentActivity — load lại dashboard để cập nhật
            // trạng thái hóa đơn mới nhất thay vì hiển thị dữ liệu cũ.
            if (viewModel != null) viewModel.loadDashboard();
        }
    }

    private void setupObservers() {
        viewModel.dashboard.observe(getViewLifecycleOwner(), data -> {
            // NÚT DEBUG 1: In xem data có bị null không
            Log.d("DASHBOARD_DEBUG", "Nhận data từ ViewModel: " + (data == null ? "NULL (LỖI PARSE)" : "CÓ DATA"));

            if (data == null || !isAdded()) {
                if (data == null) Toast.makeText(requireContext(), "Lỗi: Data bị null do parse JSON thất bại", Toast.LENGTH_LONG).show();
                return;
            }
            bindDashboard(data);
        });

        viewModel.loading.observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
        });

        viewModel.error.observe(getViewLifecycleOwner(), msg -> {
            // NÚT DEBUG 2: In chi tiết lỗi ra logcat và màn hình
            Log.e("DASHBOARD_DEBUG", "Lỗi từ ViewModel: " + msg);
            if (msg != null && !msg.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), "Lỗi API: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void bindDashboard(DashboardData data) {

        // Nút "Chọn phòng" — hiện khi tenant có từ 2 phòng active trở lên.
        List<RoomOption> rooms = data.rooms;
        boolean multi = (rooms != null && rooms.size() > 1) || data.hasMultipleRooms;
        if (multi && rooms != null && !rooms.isEmpty()) {
            binding.btnSwitchRoom.setVisibility(View.VISIBLE);
            binding.btnSwitchRoom.setText("Chọn phòng");
            binding.btnSwitchRoom.setTag(rooms);
        } else {
            binding.btnSwitchRoom.setVisibility(View.GONE);
            binding.btnSwitchRoom.setTag(null);
        }

        if (data.tenant != null) {
            binding.tvTenantName.setText(data.tenant.fullName);

            if (data.tenant.avatar != null) {
                Glide.with(this)
                        .load(data.tenant.avatar)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivAvatar);
            }
        } else {
            binding.tvTenantName.setText(
                    SessionManager.getInstance(requireContext()).getUserName()
            );
        }

        // Phòng đang chọn: ưu tiên data.room từ BE.
        String roomNumber = null;
        String floorText = null;
        double rentPrice = 0;

        if (data.room != null) {
            roomNumber = data.room.roomNumber;
            if (data.room.floor != null) {
                floorText = "Tầng " + data.room.floor.floorNumber;
            }
            rentPrice = data.room.price;
        } else if (rooms != null) {
            RoomOption selected = null;
            for (RoomOption r : rooms) {
                if (r != null && r.isSelected) {
                    selected = r;
                    break;
                }
            }
            if (selected == null && !rooms.isEmpty()) {
                selected = rooms.get(0);
            }
            if (selected != null) {
                roomNumber = selected.roomNumber;
                rentPrice = selected.monthlyRent;
            }
        }

        if (roomNumber != null && !roomNumber.isEmpty()) {
            binding.tvRoomNumber.setText(roomNumber);
        } else {
            binding.tvRoomNumber.setText("--");
        }

        if (floorText != null) {
            binding.tvFloor.setText(floorText);
        } else {
            binding.tvFloor.setText(multi ? "Nhấn «Chọn phòng» để đổi" : "--");
        }

        if (rentPrice > 0) {
            binding.tvRentAmount.setText(FormatUtils.formatShort(rentPrice));
        } else if (data.stats != null && data.stats.rentAmount > 0) {
            binding.tvRentAmount.setText(FormatUtils.formatShort(data.stats.rentAmount));
        } else {
            binding.tvRentAmount.setText("--");
        }

        if (data.invoice != null) {
            binding.tvTotalAmount.setText(
                    FormatUtils.formatCurrency(data.invoice.totalAmount)
            );

            setInvoiceStatus(data.invoice.status);

            // Hiển thị đầy đủ tiền phòng/điện/nước/dịch vụ
            buildInvoiceItemRows(data.invoice, binding.layoutInvoiceItems);

            binding.btnPayNow.setTag(data.invoice);
            binding.btnInvoiceDetail.setTag(data.invoice.id);

        } else {
            binding.tvTotalAmount.setText("0 đ");
            binding.tvInvoiceStatus.setText("Chưa có hóa đơn");
            binding.layoutInvoiceItems.removeAllViews();
            binding.btnPayNow.setTag(null);
            binding.btnInvoiceDetail.setTag(null);
        }

        if (data.stats != null) {
            if (data.stats.electricAmount > 0) {
                binding.tvElectricAmount.setText(
                        FormatUtils.formatShort(data.stats.electricAmount)
                );
            } else {
                binding.tvElectricAmount.setText("Chưa chụp");
            }

            if (data.stats.waterAmount > 0) {
                binding.tvWaterAmount.setText(
                        FormatUtils.formatShort(data.stats.waterAmount)
                );
            } else {
                binding.tvWaterAmount.setText("Chưa chụp");
            }

            binding.tvUnpaidCount.setText(
                    String.valueOf(data.stats.unpaidCount)
            );

            // HIỂN THỊ THẺ NHẮC NHỞ THANH TOÁN
            if (data.stats.unpaidCount > 0) {
                binding.cardReminder.setVisibility(View.VISIBLE);
                String debtText = FormatUtils.formatCurrency(data.stats.totalDebt);
                binding.tvReminderText.setText("Bạn có " + data.stats.unpaidCount + " hóa đơn chưa thanh toán. Tổng nợ: " + debtText);
            } else {
                binding.cardReminder.setVisibility(View.GONE);
            }
        } else {
            binding.cardReminder.setVisibility(View.GONE);
        }

        Log.d("DASHBOARD",
                "room = " + (data.room == null ? "NULL" : roomNumber)
                        + ", rooms=" + (rooms == null ? 0 : rooms.size())
                        + ", multi=" + multi);

        Log.d("DASHBOARD",
                "invoice = " + (data.invoice == null ? "NULL" : "OK"));
    }

    private void buildInvoiceItemRows(Invoice inv, LinearLayout container) {
        container.removeAllViews();

        if (inv.roomPrice > 0) {
            addInvoiceItemRow(container, "Tiền phòng", inv.roomPrice);
        }
        if (inv.electricUsage > 0 || inv.electricPrice > 0) {
            double electricTotal = inv.electricUsage * inv.electricPrice;
            addInvoiceItemRow(container, "Tiền điện (" + (int) inv.electricUsage + " kWh)", electricTotal);
        }
        if (inv.waterUsage > 0 || inv.waterPrice > 0) {
            double waterTotal = inv.waterUsage * inv.waterPrice;
            addInvoiceItemRow(container, "Tiền nước (" + (int) inv.waterUsage + " m³)", waterTotal);
        }
        if (inv.serviceFee > 0) {
            addInvoiceItemRow(container, "Phí dịch vụ", inv.serviceFee);
        }

        if (inv.items != null) {
            for (Invoice.InvoiceItem item : inv.items) {
                if (isReservedItemName(item.name)) continue;
                addInvoiceItemRow(container, item.name, item.total);
            }
        }
    }

    private boolean isReservedItemName(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase();
        return n.equals("tiền phòng") || n.equals("tiền điện") || n.equals("tiền nước");
    }

    private void addInvoiceItemRow(LinearLayout container, String name, double amount) {
        if (!isAdded()) return;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView tvName = new TextView(requireContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(name);
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvName.setTextSize(14);

        TextView tvAmount = new TextView(requireContext());
        tvAmount.setText(FormatUtils.formatCurrency(amount));
        tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvAmount.setTextSize(14);

        row.addView(tvName);
        row.addView(tvAmount);
        container.addView(row);
    }

    private void setInvoiceStatus(String status) {
        if (status == null || !isAdded()) return;

        switch (status) {
            case "paid":
                binding.tvInvoiceStatus.setText("Đã thanh toán");
                binding.tvInvoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_paid_text));
                binding.tvInvoiceStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                break;
            case "pending":
                binding.tvInvoiceStatus.setText("Đang chờ duyệt");
                binding.tvInvoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_processing_text));
                binding.tvInvoiceStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                break;
            case "overdue":
                binding.tvInvoiceStatus.setText("Quá hạn");
                binding.tvInvoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_overdue_text));
                binding.tvInvoiceStatus.setBackgroundResource(R.drawable.bg_badge_overdue);
                break;
            default:
                binding.tvInvoiceStatus.setText("Chưa thanh toán");
                binding.tvInvoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_unpaid_text));
                binding.tvInvoiceStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }
    }

    private void setupClickListeners() {
        binding.btnPayNow.setOnClickListener(v -> {
            Object tag = v.getTag();
            if (tag instanceof Invoice) {
                Invoice invoice = (Invoice) tag;

                // Không cho vào trang QR nếu đang chờ thanh toán
                if ("pending".equals(invoice.status)) {
                    Toast.makeText(requireContext(), "Hóa đơn này đang chờ xử lý", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Không cho vào trang QR nếu hóa đơn đã thanh toán rồi.
                if ("paid".equals(invoice.status)) {
                    Toast.makeText(requireContext(), "Hóa đơn này đã được thanh toán", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Không cho thanh toán nếu đã bị hủy
                if ("cancelled".equals(invoice.status)) {
                    Toast.makeText(requireContext(), "Liên hệ Admin để mở thanh toán lại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(requireContext(), PaymentActivity.class);
                intent.putExtra("invoice_id", invoice.id);
                intent.putExtra("total_amount", invoice.totalAmount);
                intent.putExtra("paid_amount", invoice.paidAmount);
                intent.putExtra("month", invoice.month);
                intent.putExtra("year", invoice.year);
                startActivityForResult(intent, REQ_PAYMENT);
            }
        });

        binding.btnInvoiceDetail.setOnClickListener(v -> {
            String invoiceId = (String) v.getTag();
            if (invoiceId != null) {
                Intent intent = new Intent(requireContext(), InvoiceDetailActivity.class);
                intent.putExtra("invoice_id", invoiceId);
                startActivity(intent);
            }
        });

        binding.btnSwitchRoom.setOnClickListener(v -> {
            Object tag = v.getTag();
            if (tag instanceof List) {
                //noinspection unchecked
                showSwitchRoomDialog((List<RoomOption>) tag);
            }
        });

        binding.cardMeter.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MeterReadingActivity.class)));

        binding.cardChat.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatActivity.class)));

        binding.cardInvoice.setOnClickListener(v ->
                requireActivity().findViewById(R.id.nav_invoice).performClick());

        binding.cardMaintenance.setOnClickListener(v ->
                requireActivity().findViewById(R.id.nav_maintenance).performClick());

        binding.ivAvatar.setOnClickListener(v -> {
            requireActivity().findViewById(R.id.nav_profile).performClick();
        });

        binding.btnDebugAdvanceMonth.setOnClickListener(v -> {
            viewModel.debugAdvanceMonth();
            Toast.makeText(requireContext(), "Đang tạo hóa đơn tháng test...", Toast.LENGTH_SHORT).show();
        });
    }

    private void showSwitchRoomDialog(List<RoomOption> rooms) {
        if (rooms == null || rooms.isEmpty() || !isAdded()) return;

        String[] labels = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            RoomOption r = rooms.get(i);
            String label = "Phòng " + (r.roomNumber != null ? r.roomNumber : "?");
            if (r.contractNumber != null && !r.contractNumber.isEmpty()) {
                label += " · HĐ " + r.contractNumber;
            }
            if (r.isSelected) {
                label += " (đang chọn)";
            }
            labels[i] = label;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn phòng")
                .setItems(labels, (dialog, which) -> {
                    RoomOption chosen = rooms.get(which);
                    if (chosen.isSelected) {
                        Toast.makeText(requireContext(), "Phòng này đang được chọn", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (chosen.contractId == null || chosen.contractId.isEmpty()) {
                        Toast.makeText(requireContext(), "Thiếu thông tin hợp đồng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.selectDashboardRoom(chosen.contractId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadDashboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}