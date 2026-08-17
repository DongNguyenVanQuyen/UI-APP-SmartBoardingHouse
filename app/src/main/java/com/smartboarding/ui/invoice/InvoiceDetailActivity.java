package com.smartboarding.ui.invoice;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
        if (invoiceId != null) loadInvoice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PAYMENT && resultCode == RESULT_OK) {
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
                Toast.makeText(InvoiceDetailActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindInvoice(Invoice inv) {
        boolean isDeposit = "deposit".equals(inv.type);
        binding.tvTitle.setText(isDeposit
                ? "Hóa đơn tiền cọc hợp đồng"
                : "Hóa đơn Tháng " + inv.month + "/" + inv.year);
        binding.tvDueDate.setText("Hạn: " + FormatUtils.formatDate(inv.dueDate));
        binding.tvTotalAmount.setText(FormatUtils.formatCurrency(inv.totalAmount));
        binding.tvPaidAmount.setText(FormatUtils.formatCurrency(inv.paidAmount));
        binding.tvRemaining.setText(FormatUtils.formatCurrency(inv.totalAmount - inv.paidAmount));

        binding.btnPay.setVisibility(View.VISIBLE);

        switch (inv.status != null ? inv.status : "") {
            case "paid":
                binding.tvStatus.setText("Đã thanh toán");
                binding.tvStatus.setTextColor(getColor(R.color.badge_paid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                binding.btnPay.setVisibility(View.GONE);

                if (isDeposit && inv.contract != null
                        && inv.contract.status != null
                        && !"active".equals(inv.contract.status)) {
                    Toast.makeText(this,
                            "Hợp đồng đã kết thúc — vui lòng liên hệ quản lý để nhận lại tiền cọc",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case "pending":
                binding.tvStatus.setText("Đang chờ duyệt");
                binding.tvStatus.setTextColor(getColor(R.color.badge_processing_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_processing);
                binding.btnPay.setVisibility(View.GONE); // Đã gửi ảnh chờ duyệt thì ẩn nút QR thanh toán đi
                break;
            case "overdue":
                binding.tvStatus.setText("Quá hạn");
                binding.tvStatus.setTextColor(getColor(R.color.badge_overdue_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_overdue);
                break;
            case "cancelled":
                binding.tvStatus.setText("Đã hủy");
                binding.tvStatus.setTextColor(getColor(R.color.badge_expired_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_expired);
                binding.btnPay.setVisibility(View.GONE);
                Toast.makeText(this,
                        "Hóa đơn này đã bị hủy do hợp đồng liên quan đã kết thúc",
                        Toast.LENGTH_LONG).show();
                break;
            default:
                binding.tvStatus.setText("Chưa thanh toán");
                binding.tvStatus.setTextColor(getColor(R.color.badge_unpaid_text));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }

        buildInvoiceItemRows(inv, binding.layoutItems);

        // HIỂN THỊ ẢNH MINH CHỨNG (NẾU CÓ)
        if (inv.receiptImage != null && !inv.receiptImage.trim().isEmpty()) {
            binding.cardReceipt.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(inv.receiptImage)
                    .into(binding.ivReceipt);

            // Bấm vào ảnh để xem full màn hình
            binding.ivReceipt.setOnClickListener(v -> showFullScreenImage(inv.receiptImage));
        } else {
            binding.cardReceipt.setVisibility(View.GONE);
        }

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

    // HÀM HIỂN THỊ ẢNH FULL MÀN HÌNH
    private void showFullScreenImage(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(getColor(android.R.color.black));
        imageView.setOnClickListener(v -> dialog.dismiss());

        Glide.with(this).load(url).into(imageView);

        dialog.setContentView(imageView);
        dialog.show();
    }

    private void buildInvoiceItemRows(Invoice inv, LinearLayout container) {
        container.removeAllViews();

        if ("deposit".equals(inv.type)) {
            addItemRow(container, "Tiền cọc hợp đồng", inv.depositAmount);
            return;
        }

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