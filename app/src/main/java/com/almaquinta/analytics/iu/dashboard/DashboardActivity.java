package com.almaquinta.analytics.iu.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Button;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.almaquinta.analytics.data.model.SourceMetric;
import com.almaquinta.analytics.data.repository.AnalyticsRepository;
import com.almaquinta.analytics.data.repository.AnalyticsRepositoryImpl;
import com.almaquinta.analytics.iu.activenew.ActiveNewUsersActivity;
import com.almaquinta.analytics.iu.main.MainActivity;
import com.almaquinta.analytics.iu.register.RegisterActivity;
import com.almaquinta.analytics.iu.viewspermonth.ViewsPerMonthActivity;
import com.almaquinta.analytics.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;

public class DashboardActivity extends AppCompatActivity {
    private static final int DASHBOARD_MONTH_LIMIT = 3;

    private TextView tvTotalVisitsValue, tvSessionsValue, tvAssetsValue, tvNewValue, tvInteractionValue, tvSourceValue;
    private TextView userDash, userRoleDash;
    private TextView tvDashboardReport, tvSelectedPeriod, tvSourcesList;
    private Spinner spinnerYear, spinnerMonth;
    private LinearLayout trafficBarsContainer;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private AnalyticsRepository repository;
    private Button btnRegister, btnAdvanced;

    private DrawerLayout drawerLayout;
    private List<AnalyticsSummary> allSummaries = new ArrayList<>();
    private Map<String, List<SourceMetric>> sourcesByPeriod = new HashMap<>();

    private List<String> yearOptions = new ArrayList<>();
    private List<String> monthOptions = new ArrayList<>();
    private int selectedYear = 0;
    private int selectedMonth = 0;
    private boolean initializedFilters = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        configureSystemBars();

        drawerLayout = findViewById(R.id.drawer_layout);
        View rootView = findViewById(R.id.scrollContent);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            bindValues();
        }
    }

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        }
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

    private void bindValues() {
        tvTotalVisitsValue = findViewById(R.id.tvTotalVisitsValue);
        tvSessionsValue = findViewById(R.id.tvSessionsValue);
        tvAssetsValue = findViewById(R.id.tvAssetsValue);
        tvNewValue = findViewById(R.id.tvNewValue);
        tvInteractionValue = findViewById(R.id.tvInteractionValue);
        tvSourceValue = findViewById(R.id.tvSourceValue);
        tvDashboardReport = findViewById(R.id.tvDashboardReport);
        tvSelectedPeriod = findViewById(R.id.tvSelectedPeriod);
        tvSourcesList = findViewById(R.id.tvSourcesList);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        trafficBarsContainer = findViewById(R.id.trafficBarsContainer);

        userDash = findViewById(R.id.userDash);
        userRoleDash = findViewById(R.id.userRoleDash);

        repository = new AnalyticsRepositoryImpl();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        btnRegister = findViewById(R.id.btnRegister);
        btnAdvanced = findViewById(R.id.btnActiveNew);
        Button btnLogOut = findViewById(R.id.btnLogOut);

        btnRegister.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RegisterActivity.class)));
        btnAdvanced.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ActiveNewUsersActivity.class)));
        btnLogOut.setOnClickListener(view -> logOut());
        Button btnViewsPerMonth = findViewById(R.id.btnViewsPerMonth);
        if (btnViewsPerMonth != null) {
            btnViewsPerMonth.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ViewsPerMonthActivity.class)));
        }

        loadUserData();
        observeDashboard();

        ImageView ivMenu = findViewById(R.id.ivMenu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        View navView = findViewById(R.id.nav_view);
        if (navView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupFilterListeners();
    }

    private void logOut() {
        firebaseAuth.signOut();
        SessionManager.getInstance().logout();

        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void observeDashboard() {
        repository.observeDashboardData(new AnalyticsRepository.DashboardDataListener() {
            @Override
            public void onDataChanged(List<AnalyticsSummary> summaries, Map<String, List<SourceMetric>> byPeriod) {
                allSummaries = summaries;
                sourcesByPeriod = byPeriod;
                runOnUiThread(() -> {
                    if (!initializedFilters) {
                        buildFilterOptions();
                        initializedFilters = true;
                    }
                    applyCurrentFilter();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(DashboardActivity.this, "No se pudieron cargar estadisticas: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadUserData() {
        AppUser currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            setupUserUI(currentUser);
        } else if (firebaseUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Usuarios")
                    .child(firebaseUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = getString(snapshot, "nombre", "");
                    String lastName = getString(snapshot, "apellido", "");
                    String email = getString(snapshot, "correo", firebaseUser.getEmail());
                    String roleRaw = getString(snapshot, "role", "");
                    Object activeObj = snapshot.child("active").getValue();
                    boolean active = true;
                    if (activeObj instanceof Boolean) active = (Boolean) activeObj;
                    else if (activeObj instanceof String) active = Boolean.parseBoolean((String) activeObj);

                    UserRole role = UserRole.EMPLOYEE;
                    if ("ADMIN".equalsIgnoreCase(roleRaw)) role = UserRole.ADMIN;
                    else if ("COORDINATOR".equalsIgnoreCase(roleRaw)) role = UserRole.COORDINATOR;

                    AppUser appUser = new AppUser(lastName, firebaseUser.getUid(), name, email, role, active);
                    SessionManager.getInstance().setCurrentUser(appUser);
                    setupUserUI(appUser);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private String getString(DataSnapshot snap, String key, String def) {
        Object val = snap.child(key).getValue();
        return val != null ? String.valueOf(val) : def;
    }

    private void setupUserUI(AppUser user) {
        String msg = "Hola, " + user.getName().trim() + " " + user.getLastName().trim();
        userDash.setText(msg);

        String rol = "Empleado";
        if (user.getRole() == UserRole.ADMIN) rol = "Administrador";
        else if (user.getRole() == UserRole.COORDINATOR) rol = "Coordinador";

        userRoleDash.setText(rol);

        boolean canManage = SessionManager.getInstance().isAdmin();
        if (btnRegister != null) btnRegister.setVisibility(canManage ? View.VISIBLE : View.GONE);
        if (btnAdvanced != null) btnAdvanced.setVisibility(canManage ? View.VISIBLE : View.GONE);
    }

    private void setupFilterListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.spinnerYear) {
                    selectedYear = parseYearOption(yearOptions.get(position));
                } else if (parent.getId() == R.id.spinnerMonth) {
                    selectedMonth = parseMonthOption(monthOptions.get(position));
                }
                applyCurrentFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerYear.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);
    }

    private void buildFilterOptions() {
        Set<Integer> years = new LinkedHashSet<>();
        for (AnalyticsSummary summary : allSummaries) years.add(summary.getYear());

        List<Integer> sortedYears = new ArrayList<>(years);
        sortedYears.sort(Collections.reverseOrder());

        yearOptions = new ArrayList<>();
        yearOptions.add("Todos los años");
        for (int year : sortedYears) yearOptions.add(String.valueOf(year));

        monthOptions = new ArrayList<>();
        monthOptions.add("Todos los meses");
        monthOptions.add("Enero");
        monthOptions.add("Febrero");
        monthOptions.add("Marzo");
        monthOptions.add("Abril");
        monthOptions.add("Mayo");
        monthOptions.add("Junio");
        monthOptions.add("Julio");
        monthOptions.add("Agosto");
        monthOptions.add("Septiembre");
        monthOptions.add("Octubre");
        monthOptions.add("Noviembre");
        monthOptions.add("Diciembre");

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthOptions);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
    }

    private void applyCurrentFilter() {
        if (allSummaries.isEmpty()) {
            renderSummary(new AnalyticsSummary(0, 0, 0, 0, 0, 0, 0d, "Sin datos"));
            tvSelectedPeriod.setText(getString(R.string.dashboard_no_data_available));
            tvDashboardReport.setText(getString(R.string.dashboard_report));
            tvSourcesList.setText(getString(R.string.no_source_data));
            return;
        }

        List<AnalyticsSummary> filtered = filterSummaries();
        AnalyticsSummary current = aggregateSummaries(filtered);
        List<SourceMetric> recentSources = aggregateSources(filtered);
        if (!recentSources.isEmpty()) {
            current = new AnalyticsSummary(
                    current.getYear(),
                    current.getMonth(),
                    current.getVisits(),
                    current.getSessions(),
                    current.getActiveUsers(),
                    current.getNewUsers(),
                    current.getEngagementRate(),
                    recentSources.get(0).getSource()
            );
        }
        renderSummary(current);
        renderTrendBars(filtered);
        renderSources(filtered);

        String periodLabel = buildPeriodLabel(filtered);
        tvSelectedPeriod.setText(periodLabel);
        tvDashboardReport.setText(periodLabel);
    }

    private List<AnalyticsSummary> filterSummaries() {
        List<AnalyticsSummary> filtered = new ArrayList<>();
        int count = Math.min(DASHBOARD_MONTH_LIMIT, allSummaries.size());
        for (int i = 0; i < count; i++) {
            filtered.add(allSummaries.get(i));
        }
        return filtered;
    }

    private AnalyticsSummary aggregateSummaries(List<AnalyticsSummary> data) {
        if (data.isEmpty()) return new AnalyticsSummary(selectedYear, selectedMonth, 0, 0, 0, 0, 0d, "Sin datos");

        int visits = 0;
        int sessions = 0;
        int active = 0;
        int newer = 0;
        double rateWeight = 0d;
        int rateBase = 0;

        Map<String, Integer> sourceActive = new HashMap<>();
        for (AnalyticsSummary item : data) {
            visits += item.getVisits();
            sessions += item.getSessions();
            active += item.getActiveUsers();
            newer += item.getNewUsers();
            rateWeight += item.getEngagementRate() * item.getActiveUsers();
            rateBase += item.getActiveUsers();

            String source = item.getTopSource();
            sourceActive.compute(source, (key, value) -> value == null ? item.getActiveUsers() : value + item.getActiveUsers());
        }

        String topSource = "Sin datos";
        int maxActive = -1;
        for (Map.Entry<String, Integer> entry : sourceActive.entrySet()) {
            if (entry.getValue() > maxActive) {
                maxActive = entry.getValue();
                topSource = entry.getKey();
            }
        }

        double engagement = rateBase > 0 ? rateWeight / rateBase : 0d;
        AnalyticsSummary head = data.get(0);
        return new AnalyticsSummary(head.getYear(), head.getMonth(), visits, sessions, active, newer, engagement, topSource);
    }

    private void renderSummary(AnalyticsSummary s) {
        tvTotalVisitsValue.setText(formatInt(s.getVisits()));
        tvSessionsValue.setText(formatInt(s.getSessions()));
        tvAssetsValue.setText(formatInt(s.getActiveUsers()));
        tvNewValue.setText(formatInt(s.getNewUsers()));
        tvInteractionValue.setText(formatPercent(s.getEngagementRate()));
        tvSourceValue.setText(s.getTopSource());
    }

    private void renderTrendBars(List<AnalyticsSummary> filtered) {
        trafficBarsContainer.removeAllViews();
        if (filtered.isEmpty()) return;

        int limit = Math.min(12, filtered.size());
        int maxVisits = 1;
        int maxSessions = 1;

        for (int i = 0; i < limit; i++) {
            AnalyticsSummary item = filtered.get(i);
            maxVisits = Math.max(maxVisits, item.getVisits());
            maxSessions = Math.max(maxSessions, item.getSessions());
        }

        for (int i = 0; i < limit; i++) {
            AnalyticsSummary item = filtered.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, 12);

            TextView label = new TextView(this);
            label.setText(getString(R.string.dashboard_month_year, monthLabel(item.getMonth()), item.getYear()));
            label.setTextColor(getColor(R.color.white_70));
            label.setTextSize(12f);
            row.addView(label);

            ProgressBar visits = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            visits.setMax(maxVisits);
            visits.setProgress(item.getVisits());
            visits.setProgressTintList(getColorStateList(R.color.titules));
            visits.setIndeterminate(false);
            row.addView(visits, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 12));

            ProgressBar sessions = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            sessions.setMax(maxSessions);
            sessions.setProgress(item.getSessions());
            sessions.setProgressTintList(getColorStateList(R.color.white_70));
            sessions.setIndeterminate(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10);
            params.topMargin = 4;
            row.addView(sessions, params);

            trafficBarsContainer.addView(row);
        }
    }

    private void renderSources(List<AnalyticsSummary> filtered) {
        List<SourceMetric> aggregate = aggregateSources(filtered);
        if (aggregate.isEmpty()) {
            tvSourcesList.setText(getString(R.string.no_source_data));
            return;
        }

        StringBuilder builder = new StringBuilder();
        int limit = Math.min(5, aggregate.size());
        for (int i = 0; i < limit; i++) {
            SourceMetric m = aggregate.get(i);
            builder.append(i + 1)
                    .append(") ")
                    .append(m.getSource())
                    .append("  • A: ")
                    .append(formatInt(m.getActiveUsers()))
                    .append(" | N: ")
                    .append(formatInt(m.getNewUsers()))
                    .append(" | Int: ")
                    .append(formatPercent(m.getEngagementRate()));
            if (i < limit - 1) builder.append("\n\n");
        }

        tvSourcesList.setText(builder.toString());
    }

    private List<SourceMetric> aggregateSources(List<AnalyticsSummary> filteredSummaries) {
        Map<String, MutableSource> sourceMap = new HashMap<>();
        for (AnalyticsSummary summary : filteredSummaries) {
            String key = summary.getYear() + "-" + summary.getMonth();
            List<SourceMetric> periodSources = sourcesByPeriod.get(key);
            if (periodSources == null) continue;

            for (SourceMetric metric : periodSources) {
                MutableSource m = sourceMap.get(metric.getSource());
                if (m == null) m = new MutableSource();
                m.active += metric.getActiveUsers();
                m.newUsers += metric.getNewUsers();
                m.rateWeight += metric.getEngagementRate() * metric.getActiveUsers();
                m.rateBase += metric.getActiveUsers();
                sourceMap.put(metric.getSource(), m);
            }
        }

        List<SourceMetric> out = new ArrayList<>();
        for (Map.Entry<String, MutableSource> entry : sourceMap.entrySet()) {
            MutableSource m = entry.getValue();
            double engagement = m.rateBase > 0 ? m.rateWeight / m.rateBase : 0d;
            out.add(new SourceMetric(entry.getKey(), m.active, m.newUsers, engagement));
        }
        out.sort((o1, o2) -> Integer.compare(o2.getActiveUsers(), o1.getActiveUsers()));
        return out;
    }

    private String buildPeriodLabel(List<AnalyticsSummary> filtered) {
        if (filtered == null || filtered.isEmpty()) return "Sin datos disponibles";

        AnalyticsSummary newest = filtered.get(0);
        AnalyticsSummary oldest = filtered.get(filtered.size() - 1);
        if (filtered.size() == 1) {
            return getString(R.string.dashboard_period_single, monthLabel(newest.getMonth()), newest.getYear());
        }

        return getString(
                R.string.dashboard_period_range,
                monthLabel(oldest.getMonth()),
                oldest.getYear(),
                monthLabel(newest.getMonth()),
                newest.getYear()
        );
    }

    private int parseYearOption(String value) {
        if (value == null || value.startsWith("Todos")) return 0;
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    private int parseMonthOption(String value) {
        if (value == null || value.startsWith("Todos")) return 0;
        String v = value.toLowerCase(Locale.ROOT);
        switch (v) {
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

    private String monthLabel(int month) {
        switch (month) {
            case 1: return "enero";
            case 2: return "febrero";
            case 3: return "marzo";
            case 4: return "abril";
            case 5: return "mayo";
            case 6: return "junio";
            case 7: return "julio";
            case 8: return "agosto";
            case 9: return "septiembre";
            case 10: return "octubre";
            case 11: return "noviembre";
            case 12: return "diciembre";
            default: return "mes";
        }
    }

    private String formatInt(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value * 100.0);
    }

    @Override
    protected void onDestroy() {
        repository.removeObservers();
        super.onDestroy();
    }

    private static class MutableSource {
        int active;
        int newUsers;
        double rateWeight;
        int rateBase;
    }
}
