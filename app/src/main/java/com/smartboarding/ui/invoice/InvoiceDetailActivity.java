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
                Toast.makeText(InvoiceDetailActivity.this, "Lỗi kết nối mạng, vui lòng thử lại.", Toast.LENGTH_LONG).show();
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
                        "Hóa đơn đã bị hủy hãy liên hệ với chủ nhà để được hỗ trợ",
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

        binding.btnDownload.setOnClickListener(v -> exportInvoiceToPdf(inv));
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

    private void exportInvoiceToPdf(Invoice inv) {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(android.graphics.Color.parseColor("#6C5CE7")); // purple_primary
            titlePaint.setTextSize(20);
            titlePaint.setFakeBoldText(true);

            android.graphics.Paint textPaint = new android.graphics.Paint();
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextSize(12);

            android.graphics.Paint boldPaint = new android.graphics.Paint();
            boldPaint.setColor(android.graphics.Color.BLACK);
            boldPaint.setTextSize(12);
            boldPaint.setFakeBoldText(true);

            android.graphics.Paint linePaint = new android.graphics.Paint();
            linePaint.setColor(android.graphics.Color.LTGRAY);
            linePaint.setStrokeWidth(1);

            int y = 50;
            canvas.drawText("HÓA ĐƠN THANH TOÁN", 50, y, titlePaint);
            y += 30;
            canvas.drawText("Mã hóa đơn: " + (inv.id != null ? inv.id : "--"), 50, y, textPaint);
            y += 20;
            canvas.drawText("Tháng/Năm: " + inv.month + "/" + inv.year, 50, y, textPaint);
            y += 20;
            canvas.drawText("Hạn thanh toán: " + FormatUtils.formatDate(inv.dueDate), 50, y, textPaint);
            y += 30;

            canvas.drawLine(50, y, 545, y, linePaint);
            y += 20;

            canvas.drawText("Phòng: " + (inv.room != null ? inv.room.roomNumber : "--"), 50, y, boldPaint);
            y += 25;

            // Draw table header
            canvas.drawText("Khoản mục", 50, y, boldPaint);
            canvas.drawText("Thành tiền", 450, y, boldPaint);
            y += 10;
            canvas.drawLine(50, y, 545, y, linePaint);
            y += 20;

            // Items
            if ("deposit".equals(inv.type)) {
                canvas.drawText("Tiền cọc hợp đồng", 50, y, textPaint);
                canvas.drawText(FormatUtils.formatCurrency(inv.depositAmount), 450, y, textPaint);
                y += 25;
            } else {
                if (inv.roomPrice > 0) {
                    canvas.drawText("Tiền phòng", 50, y, textPaint);
                    canvas.drawText(FormatUtils.formatCurrency(inv.roomPrice), 450, y, textPaint);
                    y += 25;
                }
                if (inv.electricUsage > 0 || inv.electricPrice > 0) {
                    double electricTotal = inv.electricUsage * inv.electricPrice;
                    canvas.drawText("Tiền điện (" + (int) inv.electricUsage + " kWh)", 50, y, textPaint);
                    canvas.drawText(FormatUtils.formatCurrency(electricTotal), 450, y, textPaint);
                    y += 25;
                }
                if (inv.waterUsage > 0 || inv.waterPrice > 0) {
                    double waterTotal = inv.waterUsage * inv.waterPrice;
                    canvas.drawText("Tiền nước (" + (int) inv.waterUsage + " m³)", 50, y, textPaint);
                    canvas.drawText(FormatUtils.formatCurrency(waterTotal), 450, y, textPaint);
                    y += 25;
                }
                if (inv.serviceFee > 0) {
                    canvas.drawText("Phí dịch vụ", 50, y, textPaint);
                    canvas.drawText(FormatUtils.formatCurrency(inv.serviceFee), 450, y, textPaint);
                    y += 25;
                }
                if (inv.items != null) {
                    for (Invoice.InvoiceItem item : inv.items) {
                        if (isReservedItemName(item.name)) continue;
                        canvas.drawText(item.name, 50, y, textPaint);
                        canvas.drawText(FormatUtils.formatCurrency(item.total), 450, y, textPaint);
                        y += 25;
                    }
                }
            }

            canvas.drawLine(50, y, 545, y, linePaint);
            y += 25;

            // Totals
            canvas.drawText("Tổng cộng:", 350, y, boldPaint);
            canvas.drawText(FormatUtils.formatCurrency(inv.totalAmount), 450, y, boldPaint);
            y += 20;
            canvas.drawText("Đã trả:", 350, y, textPaint);
            canvas.drawText(FormatUtils.formatCurrency(inv.paidAmount), 450, y, textPaint);
            y += 20;
            canvas.drawText("Còn nợ:", 350, y, textPaint);
            canvas.drawText(FormatUtils.formatCurrency(inv.totalAmount - inv.paidAmount), 450, y, textPaint);
            y += 30;

            // Status
            canvas.drawText("Trạng thái: " + ("paid".equals(inv.status) ? "ĐÃ THANH TOÁN" : "CHƯA THANH TOÁN"), 50, y, boldPaint);

            document.finishPage(page);

            // Save using MediaStore for scoped storage (Android 10+)
            String fileName = "HoaDon_" + (inv.room != null ? inv.room.roomNumber : "") + "_" + inv.month + "_" + inv.year + ".pdf";

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                java.io.OutputStream out = getContentResolver().openOutputStream(uri);
                document.writeTo(out);
                document.close();
                if (out != null) out.close();

                Toast.makeText(this, "Đã lưu hóa đơn vào thư mục Tải về (Downloads)", Toast.LENGTH_LONG).show();

                // Open PDF automatically
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Mở hóa đơn với"));
            } else {
                Toast.makeText(this, "Không thể lưu tệp PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}