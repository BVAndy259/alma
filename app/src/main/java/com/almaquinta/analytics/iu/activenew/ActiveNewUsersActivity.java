package com.almaquinta.analytics.iu.activenew;

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

public class ActiveNewUsersActivity extends AppCompatActivity {
    private Spinner spinnerStartYear;
    private Spinner spinnerStartMonth;
    private Spinner spinnerEndYear;
    private Spinner spinnerEndMonth;

    private TextView tvSelectedRange;
    private TextView tvTotalActiveUsers;
    private TextView tvTotalNewUsers;
    private TextView tvAverageActiveUsers;
    private TextView tvAverageNewUsers;
    private TextView tvCaptureRate;
    private TextView tvVariationActive;
    private TextView tvVariationNew;
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
        setContentView(R.layout.activity_active_new_users);
        configureSystemBars();

        View contentView = findViewById(R.id.scrollActiveNew);
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
        tvTotalActiveUsers = findViewById(R.id.tvTotalActiveUsers);
        tvTotalNewUsers = findViewById(R.id.tvTotalNewUsers);
        tvAverageActiveUsers = findViewById(R.id.tvAverageActiveUsers);
        tvAverageNewUsers = findViewById(R.id.tvAverageNewUsers);
        tvCaptureRate = findViewById(R.id.tvCaptureRate);
        tvVariationActive = findViewById(R.id.tvVariationActive);
        tvVariationNew = findViewById(R.id.tvVariationNew);
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
                    Toast.makeText(ActiveNewUsersActivity.this, getString(R.string.active_new_invalid_range), Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(ActiveNewUsersActivity.this, getString(R.string.active_new_load_error, message), Toast.LENGTH_SHORT).show());
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

        int totalActiveUsers = 0;
        int totalNewUsers = 0;
        for (AnalyticsSummary item : filtered) {
            totalActiveUsers += item.getActiveUsers();
            totalNewUsers += item.getNewUsers();
        }

        double averageActiveUsers = totalActiveUsers / (double) filtered.size();
        double averageNewUsers = totalNewUsers / (double) filtered.size();
        double captureRate = totalActiveUsers > 0 ? (totalNewUsers / (double) totalActiveUsers) : 0d;

        tvTotalActiveUsers.setText(formatInt(totalActiveUsers));
        tvTotalNewUsers.setText(formatInt(totalNewUsers));
        tvAverageActiveUsers.setText(formatInt((int) Math.round(averageActiveUsers)));
        tvAverageNewUsers.setText(formatInt((int) Math.round(averageNewUsers)));
        tvCaptureRate.setText(formatPercent(captureRate));
        tvSelectedRange.setText(getString(
                R.string.active_new_range_label,
                monthLabel(selectedStartMonth),
                selectedStartYear,
                monthLabel(selectedEndMonth),
                selectedEndYear
        ));

        setVariationValues(filtered);
        tvInsight.setText(buildInsight(filtered, captureRate));
        renderMonthlyBreakdown(filtered);
    }

    private void setVariationValues(List<AnalyticsSummary> filtered) {
        if (filtered.size() < 2) {
            tvVariationActive.setText(getString(R.string.active_new_not_available));
            tvVariationNew.setText(getString(R.string.active_new_not_available));
            return;
        }

        AnalyticsSummary previous = filtered.get(filtered.size() - 2);
        AnalyticsSummary current = filtered.get(filtered.size() - 1);

        tvVariationActive.setText(formatSignedPercent(calculateVariation(previous.getActiveUsers(), current.getActiveUsers())));
        tvVariationNew.setText(formatSignedPercent(calculateVariation(previous.getNewUsers(), current.getNewUsers())));
    }

    private double calculateVariation(int previous, int current) {
        if (previous <= 0) return 0d;
        return (current - previous) / (double) previous;
    }

    private void renderMonthlyBreakdown(List<AnalyticsSummary> filtered) {
        monthlyBreakdownContainer.removeAllViews();
        int maxActive = 1;

        for (AnalyticsSummary item : filtered) {
            maxActive = Math.max(maxActive, item.getActiveUsers());
        }

        for (AnalyticsSummary item : filtered) {
            View row = getLayoutInflater().inflate(R.layout.item_monthly_active_new, monthlyBreakdownContainer, false);
            TextView tvMonthPeriod = row.findViewById(R.id.tvMonthPeriod);
            TextView tvMonthValues = row.findViewById(R.id.tvMonthValues);
            android.widget.ProgressBar pbMonthActive = row.findViewById(R.id.pbMonthActive);

            tvMonthPeriod.setText(getString(R.string.dashboard_month_year, monthLabel(item.getMonth()), item.getYear()));
            tvMonthValues.setText(getString(
                    R.string.active_new_month_values,
                    formatInt(item.getActiveUsers()),
                    formatInt(item.getNewUsers())
            ));

            pbMonthActive.setMax(maxActive);
            pbMonthActive.setProgress(item.getActiveUsers());
            pbMonthActive.setProgressTintList(getColorStateList(R.color.titules));

            monthlyBreakdownContainer.addView(row);
        }
    }

    private String buildInsight(List<AnalyticsSummary> filtered, double captureRate) {
        AnalyticsSummary latest = filtered.get(filtered.size() - 1);
        if (filtered.size() < 2) {
            return getString(R.string.active_new_insight_single, monthLabel(latest.getMonth()), latest.getYear());
        }

        AnalyticsSummary previous = filtered.get(filtered.size() - 2);
        boolean activeUp = latest.getActiveUsers() >= previous.getActiveUsers();
        boolean newUp = latest.getNewUsers() >= previous.getNewUsers();

        if (activeUp && newUp) {
            return getString(R.string.active_new_insight_growth);
        }
        if (!activeUp && newUp) {
            return getString(R.string.active_new_insight_acquisition);
        }
        if (activeUp) {
            return getString(R.string.active_new_insight_retention);
        }
        if (captureRate > 0.35d) {
            return getString(R.string.active_new_insight_capture_high);
        }
        return getString(R.string.active_new_insight_stable);
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
        tvTotalActiveUsers.setText(getString(R.string.dashboard_placeholder_active));
        tvTotalNewUsers.setText(getString(R.string.dashboard_placeholder_new));
        tvAverageActiveUsers.setText(getString(R.string.dashboard_placeholder_active));
        tvAverageNewUsers.setText(getString(R.string.dashboard_placeholder_new));
        tvCaptureRate.setText(getString(R.string.dashboard_placeholder_interaction));
        tvVariationActive.setText(getString(R.string.active_new_not_available));
        tvVariationNew.setText(getString(R.string.active_new_not_available));
        tvSelectedRange.setText(getString(R.string.dashboard_no_data_available));
        tvInsight.setText(getString(R.string.active_new_no_data));
        monthlyBreakdownContainer.removeAllViews();
    }

    private String formatInt(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value * 100d);
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

