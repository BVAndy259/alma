package com.almaquinta.analytics.data.repository;

import com.almaquinta.analytics.data.model.AnalyticsSummary;
import com.almaquinta.analytics.data.model.SourceMetric;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsRepositoryImpl implements AnalyticsRepository{
    private final DatabaseReference analyticsRef;
    private final Map<String, AnalyticsSummary> cache = new HashMap<>();
    private ValueEventListener analyticsListener;

    public AnalyticsRepositoryImpl() {
        analyticsRef = FirebaseDatabase.getInstance().getReference("estadisticas_sesiones");
    }

    @Override
    public void observeDashboardData(DashboardDataListener listener) {
        removeObservers();
        analyticsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DashboardAccumulator acc = new DashboardAccumulator();

                for (DataSnapshot item : snapshot.child("vistas_x_mes").getChildren()) {
                    int year = asInt(item.child("AÑO").getValue(), 0);
                    int month = monthToInt(asString(item.child("MES").getValue(), ""));
                    if (year == 0 || month == 0) continue;

                    String period = key(year, month);
                    acc.visitsByPeriod.put(period, acc.visitsByPeriod.getOrDefault(period, 0) + asInt(item.child("VISITAS").getValue(), 0));
                    acc.sessionsByPeriod.put(period, acc.sessionsByPeriod.getOrDefault(period, 0) + asInt(item.child("SESIONES DE USUARIOS").getValue(), 0));
                }

                for (DataSnapshot item : snapshot.child("activosYNuevos").getChildren()) {
                    int year = asInt(item.child("AÑO").getValue(), 0);
                    int month = monthToInt(asString(item.child("MES").getValue(), ""));
                    if (year == 0 || month == 0) continue;

                    String period = key(year, month);
                    int active = asInt(item.child("USUARIOS ACTIVOS").getValue(), 0);
                    int newer = asInt(item.child("NUEVOS USUARIOS").getValue(), 0);
                    double rate = parsePercent(asString(item.child("TASA DE INTERACCIÓN").getValue(), "0"));
                    String source = asString(item.child("FUENTE").getValue(), "Sin fuente");

                    acc.activeByPeriod.put(period, acc.activeByPeriod.getOrDefault(period, 0) + active);
                    acc.newByPeriod.put(period, acc.newByPeriod.getOrDefault(period, 0) + newer);
                    acc.rateWeightByPeriod.put(period, acc.rateWeightByPeriod.getOrDefault(period, 0d) + (rate * active));
                    acc.rateBaseByPeriod.put(period, acc.rateBaseByPeriod.getOrDefault(period, 0) + active);

                    Map<String, SourceAggregate> periodSources = acc.sourcesByPeriod.get(period);
                    if (periodSources == null) {
                        periodSources = new HashMap<>();
                        acc.sourcesByPeriod.put(period, periodSources);
                    }
                    SourceAggregate sourceAgg = periodSources.get(source);
                    if (sourceAgg == null) sourceAgg = new SourceAggregate();
                    sourceAgg.active += active;
                    sourceAgg.newUsers += newer;
                    sourceAgg.rateWeight += (rate * active);
                    sourceAgg.rateBase += active;
                    periodSources.put(source, sourceAgg);
                }

                List<AnalyticsSummary> summaries = new ArrayList<>();
                Map<String, List<SourceMetric>> sourcesByPeriod = new LinkedHashMap<>();
                cache.clear();

                for (String period : acc.periods()) {
                    int[] ym = split(period);
                    int visits = acc.visitsByPeriod.getOrDefault(period, 0);
                    int sessions = acc.sessionsByPeriod.getOrDefault(period, 0);
                    int active = acc.activeByPeriod.getOrDefault(period, 0);
                    int newer = acc.newByPeriod.getOrDefault(period, 0);
                    int rateBase = acc.rateBaseByPeriod.getOrDefault(period, 0);
                    double engagement = rateBase > 0 ? acc.rateWeightByPeriod.getOrDefault(period, 0d) / rateBase : 0d;

                    List<SourceMetric> sourceMetrics = mapSourceMetrics(acc.sourcesByPeriod.get(period));
                    String topSource = sourceMetrics.isEmpty() ? "Sin datos" : sourceMetrics.get(0).getSource();

                    AnalyticsSummary summary = new AnalyticsSummary(ym[0], ym[1], visits, sessions, active, newer, engagement, topSource);
                    summaries.add(summary);
                    cache.put(period, summary);
                    sourcesByPeriod.put(period, sourceMetrics);
                }

                Collections.sort(summaries, (a, b) -> {
                    if (b.getYear() != a.getYear()) return b.getYear() - a.getYear();
                    return b.getMonth() - a.getMonth();
                });

                listener.onDataChanged(summaries, sourcesByPeriod);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };

        analyticsRef.addValueEventListener(analyticsListener);
    }

    @Override
    public void removeObservers() {
        if (analyticsListener != null) {
            analyticsRef.removeEventListener(analyticsListener);
            analyticsListener = null;
        }
    }

    @Override
    public AnalyticsSummary getSummary(int year, int month) {
        AnalyticsSummary found = cache.get(key(year, month));
        if (found != null) return found;

        return new AnalyticsSummary(year, month, 0, 0, 0, 0, 0.0, "Sin datos");
    }

    private String key(int year, int month) {
        return year + "-" + month;
    }

    private int[] split(String period) {
        String[] data = period.split("-");
        int year = 0;
        int month = 0;
        if (data.length >= 2) {
            year = asInt(data[0], 0);
            month = asInt(data[1], 0);
        }
        return new int[]{year, month};
    }

    private List<SourceMetric> mapSourceMetrics(Map<String, SourceAggregate> sourceMap) {
        List<SourceMetric> list = new ArrayList<>();
        if (sourceMap == null) return list;

        for (Map.Entry<String, SourceAggregate> entry : sourceMap.entrySet()) {
            SourceAggregate agg = entry.getValue();
            double engagement = agg.rateBase > 0 ? (agg.rateWeight / agg.rateBase) : 0d;
            list.add(new SourceMetric(entry.getKey(), agg.active, agg.newUsers, engagement));
        }

        list.sort((o1, o2) -> Integer.compare(o2.getActiveUsers(), o1.getActiveUsers()));
        return list;
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private int monthToInt(String monthRaw) {
        String m = monthRaw == null ? "" : monthRaw.trim().toLowerCase(Locale.ROOT);
        switch (m) {
            case "enero": return 1;
            case "febrero": return 2;
            case "marzo": return 3;
            case "abril": return 4;
            case "mayo": return 5;
            case "junio": return 6;
            case "julio": return 7;
            case "agosto": return 8;
            case "septiembre": return 9;
            case "octubre": return 10;
            case "noviembre": return 11;
            case "diciembre": return 12;
            default: return 0;
        }
    }

    private double parsePercent(String raw) {
        if (raw == null) return 0d;
        String cleaned = raw.replace("%", "").replace(",", ".").trim();
        try {
            return Double.parseDouble(cleaned) / 100d;
        } catch (Exception ex) {
            return 0d;
        }
    }

    private static class SourceAggregate {
        int active;
        int newUsers;
        double rateWeight;
        int rateBase;
    }

    private static class DashboardAccumulator {
        final Map<String, Integer> visitsByPeriod = new HashMap<>();
        final Map<String, Integer> sessionsByPeriod = new HashMap<>();
        final Map<String, Integer> activeByPeriod = new HashMap<>();
        final Map<String, Integer> newByPeriod = new HashMap<>();
        final Map<String, Double> rateWeightByPeriod = new HashMap<>();
        final Map<String, Integer> rateBaseByPeriod = new HashMap<>();
        final Map<String, Map<String, SourceAggregate>> sourcesByPeriod = new HashMap<>();

        List<String> periods() {
            Map<String, Boolean> all = new HashMap<>();
            for (String key : visitsByPeriod.keySet()) all.put(key, true);
            for (String key : sessionsByPeriod.keySet()) all.put(key, true);
            for (String key : activeByPeriod.keySet()) all.put(key, true);
            for (String key : newByPeriod.keySet()) all.put(key, true);

            List<String> periods = new ArrayList<>(all.keySet());
            periods.sort((p1, p2) -> {
                int[] ym1 = splitPeriod(p1);
                int[] ym2 = splitPeriod(p2);
                if (ym2[0] != ym1[0]) return ym2[0] - ym1[0];
                return ym2[1] - ym1[1];
            });
            return periods;
        }

        private int[] splitPeriod(String period) {
            String[] data = period.split("-");
            int year = 0;
            int month = 0;
            if (data.length >= 2) {
                try {
                    year = Integer.parseInt(data[0]);
                    month = Integer.parseInt(data[1]);
                } catch (Exception ignored) {}
            }
            return new int[]{year, month};
        }
    }
}
