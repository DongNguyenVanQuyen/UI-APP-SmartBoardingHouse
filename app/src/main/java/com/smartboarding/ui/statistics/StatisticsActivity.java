package com.smartboarding.ui.statistics;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.YearlyStats;
import com.smartboarding.databinding.ActivityStatisticsBinding;
import com.smartboarding.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private int currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        binding.tvYear.setText(String.valueOf(currentYear));
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnPrevYear.setOnClickListener(v -> {
            currentYear--;
            binding.tvYear.setText(String.valueOf(currentYear));
            loadStats();
        });
        binding.btnNextYear.setOnClickListener(v -> {
            currentYear++;
            binding.tvYear.setText(String.valueOf(currentYear));
            loadStats();
        });

        loadStats();
    }

    private void loadStats() {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi()
                .getYearlyStats(currentYear)
                .enqueue(new Callback<ApiResponse<YearlyStats>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<YearlyStats>> call,
                                           Response<ApiResponse<YearlyStats>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            bindStats(response.body().getData());
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<YearlyStats>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void bindStats(YearlyStats stats) {
        // Summary
        binding.tvTotalYear.setText(FormatUtils.formatCurrency(stats.summary.totalYear));
        binding.tvPaidYear.setText(FormatUtils.formatCurrency(stats.summary.paidYear));
        binding.tvDebtYear.setText(FormatUtils.formatCurrency(stats.summary.debtYear));
        binding.tvElectricTotal.setText(FormatUtils.formatCurrency(stats.utilities.electricTotal));
        binding.tvWaterTotal.setText(FormatUtils.formatCurrency(stats.utilities.waterTotal));

        // Bar chart
        if (stats.monthlyData == null) return;
        List<BarEntry> entries = new ArrayList<>();
        String[] months = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};

        for (YearlyStats.MonthData md : stats.monthlyData) {
            entries.add(new BarEntry(md.month - 1, (float) md.totalAmount));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu (đ)");
        dataSet.setColor(0xFF6C5CE7);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        binding.barChart.setData(barData);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setFitBars(true);

        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        binding.barChart.getAxisRight().setEnabled(false);
        binding.barChart.animateY(800);
        binding.barChart.invalidate();
    }
}