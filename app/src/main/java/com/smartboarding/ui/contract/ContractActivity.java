package com.smartboarding.ui.contract;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.smartboarding.R;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.Contract;
import com.smartboarding.databinding.ActivityContractBinding;
import com.smartboarding.utils.FormatUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Hiển thị toàn bộ thông tin hợp đồng thuê phòng của người thuê hiện tại
 * (mã hợp đồng, phòng, thời hạn, tiền cọc, tiền thuê, ngày thu tiền,
 * trạng thái, điều khoản, ngày ký...). Truy cập từ mục "Tài khoản".
 */
public class ContractActivity extends AppCompatActivity {

    private ActivityContractBinding binding;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContractBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.scrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isLoading || !hasMore) return;
            View child = binding.scrollView.getChildAt(binding.scrollView.getChildCount() - 1);
            if (child != null) {
                int diff = (child.getBottom() - (binding.scrollView.getHeight() + scrollY));
                if (diff <= dp(32)) {
                    currentPage++;
                    loadContracts(false);
                }
            }
        });

        loadContracts(true);
    }

    private void loadContracts(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        if (reset) {
            binding.tvEmpty.setVisibility(View.GONE);
        }

        ApiService api = RetrofitClient.getInstance(this).getApi();
        api.getContracts(currentPage, 10).enqueue(new Callback<ApiResponse<List<Contract>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Contract>>> call, Response<ApiResponse<List<Contract>>> response) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Contract> contracts = response.body().getData();
                    if (contracts == null) contracts = new java.util.ArrayList<>();

                    if (contracts.size() < 10) {
                        hasMore = false;
                    }

                    if (reset) {
                        binding.layoutContracts.removeAllViews();
                    }

                    if (contracts.isEmpty() && reset) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        renderContracts(contracts);
                    }
                } else {
                    if (reset) binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Contract>>> call, Throwable t) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                Log.e("CONTRACT_ERROR", Log.getStackTraceString(t));
                if (reset) binding.tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(ContractActivity.this, "Lỗi kết nối mạng, vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderContracts(List<Contract> contracts) {
        for (Contract c : contracts) {
            binding.layoutContracts.addView(buildContractCard(c));
        }
    }

    private CardView buildContractCard(Contract c) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));

        // Header: mã hợp đồng + trạng thái
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp(16);
        headerRow.setLayoutParams(headerParams);

        TextView tvNumber = new TextView(this);
        tvNumber.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        String number = (c.contractNumber != null && !c.contractNumber.isEmpty())
                ? c.contractNumber : "--";
        tvNumber.setText("Hợp đồng " + number);
        tvNumber.setTextColor(getColor(R.color.text_primary));
        tvNumber.setTextSize(16);
        tvNumber.setTypeface(tvNumber.getTypeface(), android.graphics.Typeface.BOLD);

        TextView tvStatus = new TextView(this);
        tvStatus.setPadding(dp(10), dp(4), dp(10), dp(4));
        applyStatusStyle(tvStatus, c.status);

        headerRow.addView(tvNumber);
        headerRow.addView(tvStatus);
        content.addView(headerRow);

        // Thông tin chi tiết
        String roomNumber = c.room != null && c.room.roomNumber != null
                ? c.room.roomNumber : (c.roomNumber != null ? c.roomNumber : "--");

        addInfoRow(content, "Số phòng", roomNumber);
        if (c.tenantName != null && !c.tenantName.isEmpty()) {
            addInfoRow(content, "Người thuê", c.tenantName);
        }
        addInfoRow(content, "Ngày bắt đầu", FormatUtils.formatDate(c.startDate));
        addInfoRow(content, "Ngày kết thúc", FormatUtils.formatDate(c.endDate));
        if (c.paymentDate > 0) {
            addInfoRow(content, "Ngày thu tiền hàng tháng", "Ngày " + c.paymentDate);
        }
        double rentPrice = c.room != null ? c.room.price : 0;
        addInfoRow(content, "Tiền thuê hàng tháng", FormatUtils.formatCurrency(rentPrice));
        addInfoRow(content, "Tiền cọc", FormatUtils.formatCurrency(c.deposit));
        addInfoRow(content, "Ngày ký hợp đồng", FormatUtils.formatDate(c.signedDate));

        if (c.terms != null && !c.terms.trim().isEmpty()) {
            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            dividerParams.topMargin = dp(4);
            dividerParams.bottomMargin = dp(12);
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(getColor(R.color.divider));
            content.addView(divider);

            TextView tvTermsLabel = new TextView(this);
            tvTermsLabel.setText("Điều khoản hợp đồng");
            tvTermsLabel.setTextColor(getColor(R.color.text_secondary));
            tvTermsLabel.setTextSize(12);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.bottomMargin = dp(4);
            tvTermsLabel.setLayoutParams(labelParams);
            content.addView(tvTermsLabel);

            TextView tvTerms = new TextView(this);
            tvTerms.setText(c.terms);
            tvTerms.setTextColor(getColor(R.color.text_primary));
            tvTerms.setTextSize(14);
            tvTerms.setLineSpacing(dp(2), 1f);
            content.addView(tvTerms);
        }

        // Nút tải hợp đồng PDF
        TextView btnDownloadContract = new TextView(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        btnParams.topMargin = dp(16);
        btnDownloadContract.setLayoutParams(btnParams);
        btnDownloadContract.setGravity(android.view.Gravity.CENTER);
        btnDownloadContract.setText("Tải hợp đồng về máy (PDF)");
        btnDownloadContract.setTextColor(getColor(R.color.white));
        btnDownloadContract.setBackgroundResource(R.drawable.gradient_button);
        btnDownloadContract.setTextSize(14);
        btnDownloadContract.setTypeface(btnDownloadContract.getTypeface(), android.graphics.Typeface.BOLD);
        btnDownloadContract.setOnClickListener(v -> exportContractToPdf(c));
        content.addView(btnDownloadContract);

        card.addView(content);
        return card;
    }

    private void applyStatusStyle(TextView tvStatus, String status) {
        String s = status != null ? status : "";
        switch (s) {
            case "active":
                tvStatus.setText(FormatUtils.statusToVietnamese(s));
                tvStatus.setTextColor(getColor(R.color.badge_paid_text));
                tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                break;
            case "terminated":
                tvStatus.setText(FormatUtils.statusToVietnamese(s));
                tvStatus.setTextColor(getColor(R.color.badge_overdue_text));
                tvStatus.setBackgroundResource(R.drawable.bg_badge_overdue);
                break;
            case "expired":
                tvStatus.setText(FormatUtils.statusToVietnamese(s));
                tvStatus.setTextColor(getColor(R.color.badge_expired_text));
                tvStatus.setBackgroundResource(R.drawable.bg_badge_expired);
                break;
            default:
                tvStatus.setText(s.isEmpty() ? "--" : s);
                tvStatus.setTextColor(getColor(R.color.badge_unpaid_text));
                tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }
        tvStatus.setTextSize(12);
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(10);
        row.setLayoutParams(p);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvLabel.setText(label);
        tvLabel.setTextColor(getColor(R.color.text_secondary));
        tvLabel.setTextSize(14);

        TextView tvValue = new TextView(this);
        tvValue.setText(value != null && !value.isEmpty() ? value : "--");
        tvValue.setTextColor(getColor(R.color.text_primary));
        tvValue.setTextSize(14);
        tvValue.setTypeface(tvValue.getTypeface(), android.graphics.Typeface.BOLD);
        tvValue.setGravity(android.view.Gravity.END);

        row.addView(tvLabel);
        row.addView(tvValue);
        container.addView(row);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void exportContractToPdf(Contract c) {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(android.graphics.Color.BLACK);
            titlePaint.setTextSize(16);
            titlePaint.setFakeBoldText(true);

            android.graphics.Paint subTitlePaint = new android.graphics.Paint();
            subTitlePaint.setColor(android.graphics.Color.BLACK);
            subTitlePaint.setTextSize(14);
            subTitlePaint.setFakeBoldText(true);

            android.graphics.Paint textPaint = new android.graphics.Paint();
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextSize(11);

            android.graphics.Paint boldPaint = new android.graphics.Paint();
            boldPaint.setColor(android.graphics.Color.BLACK);
            boldPaint.setTextSize(11);
            boldPaint.setFakeBoldText(true);

            android.graphics.Paint linePaint = new android.graphics.Paint();
            linePaint.setColor(android.graphics.Color.LTGRAY);
            linePaint.setStrokeWidth(1);

            int y = 50;
            canvas.drawText("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", 150, y, subTitlePaint);
            y += 20;
            canvas.drawText("Độc lập - Tự do - Hạnh phúc", 220, y, boldPaint);
            y += 15;
            canvas.drawLine(150, y, 440, y, linePaint);
            y += 35;

            canvas.drawText("HỢP ĐỒNG THUÊ PHÒNG TRỌ", 180, y, titlePaint);
            y += 35;

            canvas.drawText("Hôm nay, ngày " + FormatUtils.formatDate(c.signedDate != null ? c.signedDate : c.startDate) + ", chúng tôi gồm các bên:", 50, y, textPaint);
            y += 25;

            canvas.drawText("BÊN CHO THUÊ (BÊN A): CHỦ PHÒNG TRỌ SMARTBOARDING", 50, y, boldPaint);
            y += 20;
            canvas.drawText("Điện thoại: 0909xxxxxx", 50, y, textPaint);
            y += 25;

            canvas.drawText("BÊN THUÊ PHÒNG (BÊN B): " + (c.tenantName != null ? c.tenantName : "Khách thuê"), 50, y, boldPaint);
            y += 20;
            canvas.drawText("Mã hợp đồng: " + (c.contractNumber != null ? c.contractNumber : "--"), 50, y, textPaint);
            y += 25;

            canvas.drawLine(50, y, 545, y, linePaint);
            y += 25;

            canvas.drawText("Hai bên thống nhất ký kết hợp đồng thuê phòng với các điều khoản sau:", 50, y, textPaint);
            y += 25;

            canvas.drawText("Điều 1: Thông tin phòng thuê", 50, y, boldPaint);
            y += 20;
            String roomNumber = c.room != null && c.room.roomNumber != null ? c.room.roomNumber : (c.roomNumber != null ? c.roomNumber : "--");
            canvas.drawText("- Phòng số: " + roomNumber + " thuộc khu nhà trọ SmartBoarding", 70, y, textPaint);
            y += 20;
            double rentPrice = c.room != null ? c.room.price : 0;
            canvas.drawText("- Tiền thuê phòng hàng tháng: " + FormatUtils.formatCurrency(rentPrice) + " / tháng", 70, y, textPaint);
            y += 20;
            canvas.drawText("- Tiền đặt cọc: " + FormatUtils.formatCurrency(c.deposit), 70, y, textPaint);
            y += 25;

            canvas.drawText("Điều 2: Thời hạn thuê và Thanh toán", 50, y, boldPaint);
            y += 20;
            canvas.drawText("- Thời gian thuê bắt đầu từ: " + FormatUtils.formatDate(c.startDate) + " đến ngày " + FormatUtils.formatDate(c.endDate), 70, y, textPaint);
            y += 20;
            canvas.drawText("- Ngày thu tiền phòng cố định hàng tháng: Ngày " + c.paymentDate + " hàng tháng.", 70, y, textPaint);
            y += 25;

            canvas.drawText("Điều 3: Điều khoản sử dụng và Cam kết", 50, y, boldPaint);
            y += 20;
            String terms = (c.terms != null && !c.terms.trim().isEmpty()) ? c.terms : "Bên B có trách nhiệm bảo quản tài sản phòng thuê, đóng tiền điện nước và dịch vụ đầy đủ hàng tháng, giữ gìn an ninh trật tự chung.";
            canvas.drawText("- " + terms, 70, y, textPaint);
            y += 45;

            canvas.drawText("ĐẠI DIỆN BÊN A (Ký tên)", 80, y, boldPaint);
            canvas.drawText("ĐẠI DIỆN BÊN B (Ký tên)", 380, y, boldPaint);
            y += 50;

            document.finishPage(page);

            // Save using MediaStore for scoped storage (Android 10+)
            String fileName = "HopDongThuePhong_" + roomNumber + "_" + (c.tenantName != null ? c.tenantName.replaceAll("\\s+", "") : "") + ".pdf";

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

                Toast.makeText(this, "Đã lưu hợp đồng vào thư mục Tải về (Downloads)", Toast.LENGTH_LONG).show();

                // Open PDF automatically
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(intent, "Mở hợp đồng với"));
            } else {
                Toast.makeText(this, "Không thể lưu tệp hợp đồng PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất hợp đồng PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}