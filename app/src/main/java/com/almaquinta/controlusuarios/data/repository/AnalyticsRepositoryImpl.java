package com.almaquinta.controlusuarios.data.repository;

import com.almaquinta.controlusuarios.data.model.AnalyticsSummary;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsRepositoryImpl implements AnalyticsRepository{
    private final Map<String, AnalyticsSummary> data = new HashMap<>();

    public AnalyticsRepositoryImpl() {
        data.put(key(2026, 2), new AnalyticsSummary(2026, 2, 439, 277, 256, 240, 0.685, "google / cpc"));
        data.put(key(2025, 10), new AnalyticsSummary(2025, 10, 636, 333, 167, 166, 0.4706, "google / cpc"));
        data.put(key(2025, 9), new AnalyticsSummary(2025, 9, 568, 260, 133, 126, 0.5354, "(direct) / (none)"));
    }

    @Override
    public AnalyticsSummary getSummary(int year, int month) {
        AnalyticsSummary found = data.get(key(year, month));
        if (found != null) return found;

        return new AnalyticsSummary(year, month, 0, 0, 0, 0, 0.0, "Sin datos");
    }

    private String key(int year, int month) {
        return year + "-" + month;
    }
}
