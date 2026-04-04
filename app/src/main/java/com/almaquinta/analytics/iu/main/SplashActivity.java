package com.almaquinta.analytics.iu.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;
import com.almaquinta.analytics.iu.dashboard.DashboardActivity;
import com.almaquinta.analytics.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        configureSystemBars();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::routeFromSplash, 3000);
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

    private void routeFromSplash() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            openMain();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(firebaseUser.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    FirebaseAuth.getInstance().signOut();
                    openMain();
                    return;
                }

                String name = getSafeString(snapshot.child("nombre").getValue(), "");
                String lastName = getSafeString(snapshot.child("apellido").getValue(), "");
                String email = getSafeString(snapshot.child("correo").getValue(), firebaseUser.getEmail());
                String roleRaw = getSafeString(snapshot.child("role").getValue(), "");
                boolean active = getSafeBoolean(snapshot.child("active").getValue());

                UserRole role = parseRole(roleRaw);
                AppUser appUser = new AppUser(lastName, firebaseUser.getUid(), name, email, role, active);
                SessionManager.getInstance().setCurrentUser(appUser);

                openDashboard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                openMain();
            }
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getSafeString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean getSafeBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return true;
    }

    private UserRole parseRole(String roleRaw) {
        if ("ADMIN".equalsIgnoreCase(roleRaw)) {
            return UserRole.ADMIN;
        }
        if ("COORDINATOR".equalsIgnoreCase(roleRaw)) {
            return UserRole.COORDINATOR;
        }
        if (roleRaw == null || roleRaw.trim().isEmpty()) {
            return UserRole.ADMIN;
        }
        return UserRole.EMPLOYEE;
    }
}