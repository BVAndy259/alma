package com.almaquinta.controlusuarios.iu.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageView;
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

import java.text.NumberFormat;
import java.util.Locale;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import com.almaquinta.controlusuarios.data.model.AppUser;
import com.almaquinta.controlusuarios.data.model.UserRole;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvTotalVisitsValue, tvSessionsValue, tvAssetsValue, tvNewValue, tvInteractionValue, tvSourceValue;
    private TextView userDash, userRoleDash;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private AnalyticsRepository repository;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

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
    }

    private void bindValues() {
        tvTotalVisitsValue = findViewById(R.id.tvTotalVisitsValue);
        tvSessionsValue = findViewById(R.id.tvSessionsValue);
        tvAssetsValue = findViewById(R.id.tvAssetsValue);
        tvNewValue = findViewById(R.id.tvNewValue);
        tvInteractionValue = findViewById(R.id.tvInteractionValue);
        tvSourceValue = findViewById(R.id.tvSourceValue);

        userDash = findViewById(R.id.userDash);
        userRoleDash = findViewById(R.id.userRoleDash);

        repository = new AnalyticsRepositoryImpl();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        loadUserData();

        loadDashboard();

        Button btnRegister = findViewById(R.id.btnRegister);
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        btnRegister.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (isAdmin) {
            btnRegister.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RegisterActivity.class)));
        }

        Button btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(view -> logOut());
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

                    UserRole role = UserRole.USER;
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

        String rol = "Usuario";
        if (user.getRole() == UserRole.ADMIN) rol = "Administrador";
        else if (user.getRole() == UserRole.COORDINATOR) rol = "Coordinador";

        userRoleDash.setText(rol);

        Button btnRegister = findViewById(R.id.btnRegister);
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        btnRegister.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private String formatInt(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value * 100.0);
    }
}
