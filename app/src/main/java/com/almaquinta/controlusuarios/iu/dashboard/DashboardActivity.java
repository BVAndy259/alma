package com.almaquinta.controlusuarios.iu.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.controlusuarios.R;
import com.almaquinta.controlusuarios.data.model.AnalyticsSummary;
import com.almaquinta.controlusuarios.data.repository.AnalyticsRepository;
import com.almaquinta.controlusuarios.data.repository.AnalyticsRepositoryImpl;
import com.almaquinta.controlusuarios.iu.main.MainActivity;
import com.almaquinta.controlusuarios.iu.register.RegisterActivity;
import com.almaquinta.controlusuarios.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvTotalVisitsValue, tvSessionsValue, tvAssetsValue, tvNewValue, tvInteractionValue, tvSourceValue;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
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

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        loadDashboard();

        Button btnRegister = findViewById(R.id.btnRegister);
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        btnRegister.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (isAdmin) {
            btnRegister.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RegisterActivity.class)));
        }

        Button btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });
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

    private void loadDashboard() {
        AnalyticsSummary s = repository.getSummary(2026, 2);

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