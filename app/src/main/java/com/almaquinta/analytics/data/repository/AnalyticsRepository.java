package com.almaquinta.analytics.data.repository;

import com.almaquinta.analytics.data.model.AnalyticsSummary;
import com.almaquinta.analytics.data.model.SourceMetric;

import java.util.List;
import java.util.Map;

public interface AnalyticsRepository {
    interface DashboardDataListener {
        void onDataChanged(List<AnalyticsSummary> summaries, Map<String, List<SourceMetric>> sourcesByPeriod);
        void onError(String message);
    }

    void observeDashboardData(DashboardDataListener listener);
    void removeObservers();
    AnalyticsSummary getSummary(int year, int month);
}
