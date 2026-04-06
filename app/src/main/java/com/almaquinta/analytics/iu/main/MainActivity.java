package com.almaquinta.analytics.iu.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.almaquinta.analytics.iu.common.SystemBarsEdgeToEdge;

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

public class MainActivity extends AppCompatActivity {
    private EditText etUser, etPassword;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private String user = "", password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemBarsEdgeToEdge.apply(this, R.id.main);

        etUser = findViewById(R.id.etUserLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        Button btnRegister = findViewById(R.id.btnLogin);
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Espere por favor...");

        btnRegister.setOnClickListener(v -> validateData());
    }

    private void validateData() {
        user = etUser.getText().toString().trim();
        password = etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(user).matches()) {
            Toast.makeText(this, "Ingrese correo válido", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Ingrese contraseña válida", Toast.LENGTH_SHORT).show();
        } else if (password.length() < 8) {
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
        } else {
            logIn();
        }
    }

    private void logIn() {
        progressDialog.setMessage("Iniciando sesión...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener(MainActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        loadCurrentUserAndOpenDashboard(firebaseUser);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Verifique si el correo o contraseña con correctos", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Ocurrió un problema", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCurrentUserAndOpenDashboard(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "No se pudo obtener la sesión actual", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(firebaseUser.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = getSafeString(snapshot.child("nombre").getValue(), "");
                String lastName = getSafeString(snapshot.child("apellido").getValue(), "");
                String email = getSafeString(snapshot.child("correo").getValue(), firebaseUser.getEmail());
                String roleRaw = getSafeString(snapshot.child("role").getValue(), "");
                boolean active = getSafeBoolean(snapshot.child("active").getValue());

                if (!active) {
                    progressDialog.dismiss();
                    firebaseAuth.signOut();
                    SessionManager.getInstance().logout();
                    Toast.makeText(MainActivity.this, "Tu cuenta está desactivada", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserRole role = parseRole(roleRaw);
                if (roleRaw.isEmpty()) {
                    userRef.child("role").setValue(UserRole.ADMIN.name());
                }
                AppUser appUser = new AppUser(lastName, firebaseUser.getUid(), name, email, role, active);
                SessionManager.getInstance().setCurrentUser(appUser);

                progressDialog.dismiss();
                startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                Toast.makeText(MainActivity.this, "Bienvenido " + name + " " + lastName, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "No se pudo cargar el perfil de usuario", Toast.LENGTH_SHORT).show();
            }
        });
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