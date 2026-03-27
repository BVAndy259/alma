package com.almaquinta.controlusuarios.data.repository;

import com.almaquinta.controlusuarios.data.model.AnalyticsSummary;

public interface AnalyticsRepository {
    AnalyticsSummary getSummary(int year, int month);
}
