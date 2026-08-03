package com.smartboarding.ui.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.smartboarding.R;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.*;
import com.smartboarding.databinding.ActivityInvoiceDetailBinding;
import com.smartboarding.ui.payment.PaymentActivity;
import com.smartboarding.utils.FormatUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceDetailActivity extends AppCompatActivity {
    private static final int REQ_PAYMENT = 2001;

    private ActivityInvoiceDetailBinding binding;
    private String invoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        invoiceId = getIntent().getStringExtra("invoice_id");
        binding.btnBack.setOnClickListener(v -> finish());

        if (invoiceId != null) loadInvoice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Luôn tải lại để đảm bảo hiển thị đúng trạng thái mới nhất
        // (vd sau khi vừa thanh toán xong ở màn hình khác).
        if (invoiceId != null) loadInvoice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PAYMENT && resultCode == RESULT_OK) {
            // Thanh toán vừa xong — thoát hẳn ra khỏi trang chi tiết hóa đơn
            // (ra danh sách hóa đơn), thay vì hiển thị lại dữ liệu cũ.
            finish();
        }
    }

    private void loadInvoice() {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getInstance(this).getApi();
        api.getInvoiceById(invoiceId).enqueue(new Callback<ApiResponse<Invoice>>() {
            @Override
            public void onResponse(Call<ApiResponse<Invoice>> call, Response<ApiResponse<Invoice>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bindInvoice(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Invoice>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);

                Log.e("INVOICE_ERROR", Log.getStackTraceString(t));

                Toast.makeText(
                        InvoiceDetailActivity.this,
                        t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void bindInvoice(Invoice inv) {
        binding.tvTitle.setText("Hóa đơn Tháng " + inv.month + "/" + inv.year);
        binding.tvDueDate.setText("Hạn: " + FormatUtils.formatDate(inv.dueDate));
        binding.tvTotalAmount.setText(FormatUtils.formatCurrency(inv.totalAmount));
        binding.tvPaidAmount.setText(FormatUtils.formatCurrency(inv.paidAmount));
        binding.tvRemaining.setText(FormatUtils.formatCurrency(inv.totalAmount - inv.paidAmount));

        switch (inv.status != null ? inv.status : "") {
            case "paid":
                binding.tvStatus.setText("Đã thanh toán");
                binding.tvStatus.setTextColor(getColor(R.color.badge_paid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                binding.btnPay.setVisibility(View.GONE);
                break;
            case "overdue":
                binding.tvStatus.setText("Quá hạn");
                binding.tvStatus.setTextColor(getColor(R.color.badge_overdue_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_overdue);
                break;
            default:
                binding.tvStatus.setText("Chưa thanh toán");
                binding.tvStatus.setTextColor(getColor(R.color.badge_unpaid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }

        buildInvoiceItemRows(inv, binding.layoutItems);

        binding.btnPay.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("invoice_id", inv.id);
            intent.putExtra("total_amount", inv.totalAmount);
            intent.putExtra("paid_amount", inv.paidAmount);
            intent.putExtra("month", inv.month);
            intent.putExtra("year", inv.year);
            startActivityForResult(intent, REQ_PAYMENT);
        });

        binding.btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());
    }

    // Hiển thị đầy đủ tiền phòng/điện/nước/dịch vụ từ các field cố định của hóa đơn,
    // rồi mới thêm các khoản phụ phí khác trong items[] (bỏ qua item nào trùng tên
    // với tiền phòng/điện/nước để tránh hiển thị lặp với dữ liệu cũ).
    private void buildInvoiceItemRows(Invoice inv, LinearLayout container) {
        container.removeAllViews();

        if (inv.roomPrice > 0) {
            addItemRow(container, "Tiền phòng", inv.roomPrice);
        }
        if (inv.electricUsage > 0 || inv.electricPrice > 0) {
            double electricTotal = inv.electricUsage * inv.electricPrice;
            addItemRow(container, "Tiền điện (" + (int) inv.electricUsage + " kWh)", electricTotal);
        }
        if (inv.waterUsage > 0 || inv.waterPrice > 0) {
            double waterTotal = inv.waterUsage * inv.waterPrice;
            addItemRow(container, "Tiền nước (" + (int) inv.waterUsage + " m³)", waterTotal);
        }
        if (inv.serviceFee > 0) {
            addItemRow(container, "Phí dịch vụ", inv.serviceFee);
        }

        if (inv.items != null) {
            for (Invoice.InvoiceItem item : inv.items) {
                if (isReservedItemName(item.name)) continue;
                addItemRow(container, item.name, item.total);
            }
        }
    }

    private boolean isReservedItemName(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase();
        return n.equals("tiền phòng") || n.equals("tiền điện") || n.equals("tiền nước");
    }

    private void addItemRow(LinearLayout container, String name, double amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 12);
        row.setLayoutParams(p);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(name);
        tvName.setTextColor(getColor(R.color.text_secondary));
        tvName.setTextSize(15);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(FormatUtils.formatCurrency(amount));
        tvAmt.setTextColor(getColor(R.color.text_primary));
        tvAmt.setTextSize(15);

        row.addView(tvName);
        row.addView(tvAmt);
        container.addView(row);
    }
}