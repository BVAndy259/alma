package com.almaquinta.analytics.data.repository;

import com.almaquinta.analytics.data.model.AnalyticsSummary;

public interface AnalyticsRepository {
    AnalyticsSummary getSummary(int year, int month);
}
