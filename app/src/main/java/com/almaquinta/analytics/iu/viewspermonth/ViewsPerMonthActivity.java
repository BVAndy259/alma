package com.almaquinta.analytics.iu.viewspermonth;

import android.os.Bundle;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.data.model.AnalyticsSummary;
import com.almaquinta.analytics.data.repository.AnalyticsRepository;
import com.almaquinta.analytics.data.repository.AnalyticsRepositoryImpl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ViewsPerMonthActivity extends AppCompatActivity {
    private Spinner spinnerStartYear;
    private Spinner spinnerStartMonth;
    private Spinner spinnerEndYear;
    private Spinner spinnerEndMonth;

    private TextView tvSelectedRange;
    private TextView tvTotalViews;
    private TextView tvAverageViews;
    private TextView tvPeakMonth;
    private TextView tvLowestMonth;
    private TextView tvVariation;
    private TextView tvInsight;

    private LinearLayout monthlyBreakdownContainer;

    private AnalyticsRepository repository;
    private List<AnalyticsSummary> allSummaries = new ArrayList<>();

    private final List<String> yearOptions = new ArrayList<>();
    private final List<String> monthOptions = new ArrayList<>();

    private int selectedStartYear;
    private int selectedStartMonth;
    private int selectedEndYear;
    private int selectedEndMonth;

    private boolean filtersInitialized;
    private boolean suppressSelectionEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_views_per_month);
        configureSystemBars();

        View contentView = findViewById(R.id.scrollViewsPerMonth);
        ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupListeners();
        observeData();
    }

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void bindViews() {
        spinnerStartYear = findViewById(R.id.spinnerStartYear);
        spinnerStartMonth = findViewById(R.id.spinnerStartMonth);
        spinnerEndYear = findViewById(R.id.spinnerEndYear);
        spinnerEndMonth = findViewById(R.id.spinnerEndMonth);

        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        tvTotalViews = findViewById(R.id.tvTotalViews);
        tvAverageViews = findViewById(R.id.tvAverageViews);
        tvPeakMonth = findViewById(R.id.tvPeakMonth);
        tvLowestMonth = findViewById(R.id.tvLowestMonth);
        tvVariation = findViewById(R.id.tvVariation);
        tvInsight = findViewById(R.id.tvInsight);

        monthlyBreakdownContainer = findViewById(R.id.monthlyBreakdownContainer);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        repository = new AnalyticsRepositoryImpl();
    }

    private void setupListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSelectionEvents || allSummaries.isEmpty()) return;
                readCurrentSelections();
                if (!isValidRange()) {
                    selectedEndYear = selectedStartYear;
                    selectedEndMonth = selectedStartMonth;
                    applySelectionsToSpinners();
                    Toast.makeText(ViewsPerMonthActivity.this, getString(R.string.views_month_invalid_range), Toast.LENGTH_SHORT).show();
                }
                renderCurrentRange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerStartYear.setOnItemSelectedListener(listener);
        spinnerStartMonth.setOnItemSelectedListener(listener);
        spinnerEndYear.setOnItemSelectedListener(listener);
        spinnerEndMonth.setOnItemSelectedListener(listener);
    }

    private void observeData() {
        repository.observeDashboardData(new AnalyticsRepository.DashboardDataListener() {
            @Override
            public void onDataChanged(List<AnalyticsSummary> summaries, java.util.Map<String, java.util.List<com.almaquinta.analytics.data.model.SourceMetric>> sourcesByPeriod) {
                runOnUiThread(() -> {
                    allSummaries = new ArrayList<>(summaries);
                    if (allSummaries.isEmpty()) {
                        renderEmptyState();
                        return;
                    }

                    if (!filtersInitialized) {
                        setupFilterOptions();
                        setupDefaultRange();
                        applySelectionsToSpinners();
                        filtersInitialized = true;
                    }

                    renderCurrentRange();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(ViewsPerMonthActivity.this, getString(R.string.views_month_load_error, message), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupFilterOptions() {
        Set<Integer> years = new LinkedHashSet<>();
        for (AnalyticsSummary summary : allSummaries) {
            years.add(summary.getYear());
        }

        yearOptions.clear();
        for (Integer year : years) {
            yearOptions.add(String.valueOf(year));
        }

        monthOptions.clear();
        for (int month = 1; month <= 12; month++) {
            monthOptions.add(monthLabel(month));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartYear.setAdapter(yearAdapter);
        spinnerEndYear.setAdapter(yearAdapter);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthOptions);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartMonth.setAdapter(monthAdapter);
        spinnerEndMonth.setAdapter(monthAdapter);
    }

    private void setupDefaultRange() {
        AnalyticsSummary latest = allSummaries.get(0);
        AnalyticsSummary oldestForDefault = allSummaries.get(Math.min(2, allSummaries.size() - 1));

        selectedStartYear = oldestForDefault.getYear();
        selectedStartMonth = oldestForDefault.getMonth();
        selectedEndYear = latest.getYear();
        selectedEndMonth = latest.getMonth();
    }

    private void applySelectionsToSpinners() {
        suppressSelectionEvents = true;

        spinnerStartYear.setSelection(Math.max(0, yearOptions.indexOf(String.valueOf(selectedStartYear))));
        spinnerEndYear.setSelection(Math.max(0, yearOptions.indexOf(String.valueOf(selectedEndYear))));
        spinnerStartMonth.setSelection(Math.max(0, selectedStartMonth - 1));
        spinnerEndMonth.setSelection(Math.max(0, selectedEndMonth - 1));

        suppressSelectionEvents = false;
    }

    private void readCurrentSelections() {
        selectedStartYear = parseYearValue((String) spinnerStartYear.getSelectedItem(), selectedStartYear);
        selectedEndYear = parseYearValue((String) spinnerEndYear.getSelectedItem(), selectedEndYear);
        selectedStartMonth = spinnerStartMonth.getSelectedItemPosition() + 1;
        selectedEndMonth = spinnerEndMonth.getSelectedItemPosition() + 1;
    }

    private void renderCurrentRange() {
        List<AnalyticsSummary> filtered = filterByRange(allSummaries, selectedStartYear, selectedStartMonth, selectedEndYear, selectedEndMonth);
        if (filtered.isEmpty()) {
            renderEmptyState();
            return;
        }

        int totalViews = 0;
        int maxViews = 0;
        int minViews = Integer.MAX_VALUE;
        AnalyticsSummary peakSummary = null;
        AnalyticsSummary lowestSummary = null;

        for (AnalyticsSummary item : filtered) {
            totalViews += item.getVisits();
            if (item.getVisits() > maxViews) {
                maxViews = item.getVisits();
                peakSummary = item;
            }
            if (item.getVisits() < minViews) {
                minViews = item.getVisits();
                lowestSummary = item;
            }
        }

        double averageViews = totalViews / (double) filtered.size();

        tvTotalViews.setText(formatInt(totalViews));
        tvAverageViews.setText(formatInt((int) Math.round(averageViews)));

        if (peakSummary != null) {
            tvPeakMonth.setText(getString(R.string.views_month_peak_format, monthLabel(peakSummary.getMonth()), peakSummary.getYear()));
        }
        if (lowestSummary != null) {
            tvLowestMonth.setText(getString(R.string.views_month_lowest_format, monthLabel(lowestSummary.getMonth()), lowestSummary.getYear()));
        }

        tvSelectedRange.setText(getString(
                R.string.active_new_range_label,
                monthLabel(selectedStartMonth),
                selectedStartYear,
                monthLabel(selectedEndMonth),
                selectedEndYear
        ));

        setVariationValue(filtered);
        tvInsight.setText(buildInsight(filtered, totalViews));
        renderMonthlyBreakdown(filtered, maxViews);
    }

    private void setVariationValue(List<AnalyticsSummary> filtered) {
        if (filtered.size() < 2) {
            tvVariation.setText(getString(R.string.active_new_not_available));
            return;
        }

        AnalyticsSummary first = filtered.get(filtered.size() - 1);
        AnalyticsSummary last = filtered.get(0);

        tvVariation.setText(formatSignedPercent(calculateVariation(first.getVisits(), last.getVisits())));
    }

    private double calculateVariation(int previous, int current) {
        if (previous <= 0) return 0d;
        return (current - previous) / (double) previous;
    }

    private void renderMonthlyBreakdown(List<AnalyticsSummary> filtered, int maxViews) {
        monthlyBreakdownContainer.removeAllViews();

        for (AnalyticsSummary item : filtered) {
            View row = getLayoutInflater().inflate(R.layout.item_views_per_month, monthlyBreakdownContainer, false);
            TextView tvMonthPeriod = row.findViewById(R.id.tvMonthPeriod);
            TextView tvMonthViews = row.findViewById(R.id.tvMonthViews);
            android.widget.ProgressBar pbMonthViews = row.findViewById(R.id.pbMonthViews);

            tvMonthPeriod.setText(getString(R.string.dashboard_month_year, monthLabel(item.getMonth()), item.getYear()));
            tvMonthViews.setText(getString(R.string.views_month_detail, formatInt(item.getVisits())));

            pbMonthViews.setMax(maxViews > 0 ? maxViews : 100);
            pbMonthViews.setProgress(item.getVisits());
            pbMonthViews.setProgressTintList(getColorStateList(R.color.titules));

            monthlyBreakdownContainer.addView(row);
        }
    }

    private String buildInsight(List<AnalyticsSummary> filtered, int totalViews) {
        if (filtered.size() < 2) {
            AnalyticsSummary single = filtered.get(0);
            return getString(R.string.views_month_insight_single, monthLabel(single.getMonth()), single.getYear());
        }

        AnalyticsSummary first = filtered.get(filtered.size() - 1);
        AnalyticsSummary last = filtered.get(0);

        double variation = calculateVariation(first.getVisits(), last.getVisits());
        int peakViews = 0;
        int valleyCount = 0;

        for (AnalyticsSummary item : filtered) {
            peakViews = Math.max(peakViews, item.getVisits());
            if (item.getVisits() < (totalViews / filtered.size()) * 0.7) {
                valleyCount++;
            }
        }

        if (variation > 0.2) {
            return getString(R.string.views_month_insight_growth);
        }
        if (variation < -0.2) {
            return getString(R.string.views_month_insight_decline);
        }
        if (valleyCount > filtered.size() / 2) {
            return getString(R.string.views_month_insight_volatile);
        }
        return getString(R.string.views_month_insight_stable);
    }

    private List<AnalyticsSummary> filterByRange(List<AnalyticsSummary> source, int startYear, int startMonth, int endYear, int endMonth) {
        List<AnalyticsSummary> filtered = new ArrayList<>();
        int startValue = rangeValue(startYear, startMonth);
        int endValue = rangeValue(endYear, endMonth);

        for (int index = source.size() - 1; index >= 0; index--) {
            AnalyticsSummary item = source.get(index);
            int currentValue = rangeValue(item.getYear(), item.getMonth());
            if (currentValue >= startValue && currentValue <= endValue) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean isValidRange() {
        return rangeValue(selectedStartYear, selectedStartMonth) <= rangeValue(selectedEndYear, selectedEndMonth);
    }

    private int rangeValue(int year, int month) {
        return (year * 100) + month;
    }

    private int parseYearValue(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String monthLabel(int month) {
        switch (month) {
            case 1:
                return getString(R.string.month_january);
            case 2:
                return getString(R.string.month_february);
            case 3:
                return getString(R.string.month_march);
            case 4:
                return getString(R.string.month_april);
            case 5:
                return getString(R.string.month_may);
            case 6:
                return getString(R.string.month_june);
            case 7:
                return getString(R.string.month_july);
            case 8:
                return getString(R.string.month_august);
            case 9:
                return getString(R.string.month_september);
            case 10:
                return getString(R.string.month_october);
            case 11:
                return getString(R.string.month_november);
            case 12:
                return getString(R.string.month_december);
            default:
                return getString(R.string.month_unknown);
        }
    }

    private void renderEmptyState() {
        tvTotalViews.setText(getString(R.string.dashboard_placeholder_visits));
        tvAverageViews.setText(getString(R.string.dashboard_placeholder_visits));
        tvPeakMonth.setText(getString(R.string.active_new_not_available));
        tvLowestMonth.setText(getString(R.string.active_new_not_available));
        tvVariation.setText(getString(R.string.active_new_not_available));
        tvSelectedRange.setText(getString(R.string.dashboard_no_data_available));
        tvInsight.setText(getString(R.string.views_month_no_data));
        monthlyBreakdownContainer.removeAllViews();
    }

    private String formatInt(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String formatSignedPercent(double value) {
        return String.format(Locale.US, "%+.1f%%", value * 100d);
    }

    @Override
    protected void onDestroy() {
        repository.removeObservers();
        super.onDestroy();
    }
}

