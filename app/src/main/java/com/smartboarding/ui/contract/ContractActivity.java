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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContractBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        loadContracts();
    }

    private void loadContracts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getInstance(this).getApi();
        api.getContracts().enqueue(new Callback<ApiResponse<List<Contract>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Contract>>> call, Response<ApiResponse<List<Contract>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Contract> contracts = response.body().getData();
                    if (contracts == null || contracts.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        renderContracts(contracts);
                    }
                } else {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Contract>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e("CONTRACT_ERROR", Log.getStackTraceString(t));
                binding.tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(ContractActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderContracts(List<Contract> contracts) {
        binding.layoutContracts.removeAllViews();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}