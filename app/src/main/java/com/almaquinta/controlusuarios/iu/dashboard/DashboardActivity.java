package com.almaquinta.controlusuarios.iu.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.controlusuarios.R;
import com.almaquinta.controlusuarios.data.model.AnalyticsSummary;
import com.almaquinta.controlusuarios.data.repository.AnalyticsRepository;
import com.almaquinta.controlusuarios.data.repository.AnalyticsRepositoryImpl;
import com.almaquinta.controlusuarios.iu.register.RegisterActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvTotalVisitsValue, tvSessionsValue, tvAssetsValue, tvNewValue, tvInteractionValue, tvSourceValue;
    private Button btnRegister;
    private AnalyticsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            bindValues();
        }
    }

    private void bindValues() {
        tvTotalVisitsValue = findViewById(R.id.tvTotalVisitsValue);
        tvSessionsValue = findViewById(R.id.tvSessionsValue);
        tvAssetsValue = findViewById(R.id.tvAssetsValue);
        tvNewValue = findViewById(R.id.tvNewValue);
        tvInteractionValue = findViewById(R.id.tvInteractionValue);
        tvSourceValue = findViewById(R.id.tvSourceValue);

        repository = new AnalyticsRepositoryImpl();

        loadDashboard(2026, 2);

        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, RegisterActivity.class));
            }
        });
    }

    private void loadDashboard(int year, int month) {
        AnalyticsSummary s = repository.getSummary(year, month);

        tvTotalVisitsValue.setText(formatInt(s.getVisits()));
        tvSessionsValue.setText(formatInt(s.getSessions()));
        tvAssetsValue.setText(formatInt(s.getActiveUsers()));
        tvNewValue.setText(formatInt(s.getNewUsers()));
        tvInteractionValue.setText(formatPercent(s.getEngagementRate()));
        tvSourceValue.setText(s.getTopSource());
    }

    private String formatInt(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value * 100.0);
    }
}