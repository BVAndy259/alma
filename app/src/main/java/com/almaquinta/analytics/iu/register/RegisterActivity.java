package com.almaquinta.analytics.iu.register;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;
import com.almaquinta.analytics.security.AuthorizationService;
import com.almaquinta.analytics.iu.dashboard.DashboardActivity;
import com.almaquinta.analytics.iu.main.MainActivity;
import com.almaquinta.analytics.session.SessionManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etLastName, etEmail, etPassword;
    private Spinner spRole;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private String name = "", lastName = "", email = "", password = "";
    private UserRole selectedRole = UserRole.EMPLOYEE;
    private final AuthorizationService authorizationService = new AuthorizationService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        firebaseAuth = FirebaseAuth.getInstance();
        if (!isAdminAllowed()) {
            return;
        }

        View contentView = findViewById(R.id.scrollContentRegister);
        if (contentView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindValues();
    }

    private void bindValues() {
        etName = findViewById(R.id.etRegisterName);
        etLastName = findViewById(R.id.etLastNameRegister);
        etEmail = findViewById(R.id.etEmailRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        spRole = findViewById(R.id.spRoleRegister);
        TextView tvLogin = findViewById(R.id.tvDashboard);
        Button btnRegister = findViewById(R.id.btnRegister);
        setupRoleSpinner();

        progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setTitle("Espere por favor...");
        progressDialog.setCanceledOnTouchOutside(false);

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private void validateData() {
        name = etName.getText().toString().trim();
        lastName = etLastName.getText().toString().trim();
        email = etEmail.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        selectedRole = getSelectedRole();
        if (!isSelectedRoleAllowed(selectedRole)) {
            Toast.makeText(this, "El coordinador solo puede crear empleados", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "El campo nombre está vacío", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, "El campo apellido está vacío", Toast.LENGTH_LONG).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password) || password.length() < 8) {
            Toast.makeText(this, "Ingrese una contraseña de 8 caracteres como mínimo", Toast.LENGTH_SHORT).show();
        } else {
            register();
        }
    }

    private void register() {
        progressDialog.setMessage("Registrando Usuario");
        progressDialog.show();

        FirebaseApp secondaryApp = getOrCreateSecondaryApp();

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> handleCreateUserResult(task, secondaryAuth, secondaryApp));
    }

    private FirebaseApp getOrCreateSecondaryApp() {
        FirebaseOptions defaultOptions = FirebaseApp.getInstance().getOptions();
        try {
            return FirebaseApp.initializeApp(this, defaultOptions, "RegisterSecondaryApp");
        } catch (IllegalStateException ex) {
            return FirebaseApp.getInstance("RegisterSecondaryApp");
        }
    }

    private void handleCreateUserResult(Task<AuthResult> task, FirebaseAuth secondaryAuth, FirebaseApp secondaryApp) {
        if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
            progressDialog.dismiss();
            String message = task.getException() != null ? task.getException().getMessage() : "Ocurrió un problema, revisa los campos";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            cleanupSecondaryAuth(secondaryAuth, secondaryApp);
            return;
        }

        String uId = task.getResult().getUser().getUid();
        saveUser(uId, secondaryAuth, secondaryApp);
    }

    private void saveUser(String uId, FirebaseAuth secondaryAuth, FirebaseApp secondaryApp) {
        progressDialog.setMessage("Guardando Información...");

        Map<String, Object> datosusuario = new HashMap<>();
        datosusuario.put("uid", uId);
        datosusuario.put("nombre", name);
        datosusuario.put("apellido", lastName);
        datosusuario.put("correo", email);
        datosusuario.put("role", selectedRole.name());
        datosusuario.put("active", true);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Usuarios");
        databaseReference.child(uId).setValue(datosusuario).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                cleanupSecondaryAuth(secondaryAuth, secondaryApp);
                Toast.makeText(RegisterActivity.this, "Usuario Creado Exitosamente", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                cleanupSecondaryAuth(secondaryAuth, secondaryApp);
                Toast.makeText(RegisterActivity.this, "Ocurrió un problema al guardar" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cleanupSecondaryAuth(FirebaseAuth secondaryAuth, FirebaseApp secondaryApp) {
        secondaryAuth.signOut();
        secondaryApp.delete();
    }

    private boolean isAdminAllowed() {
        try {
            authorizationService.requireRegistrationAccess();
            return true;
        } catch (SecurityException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            Intent intent = firebaseAuth != null && firebaseAuth.getCurrentUser() != null
                    ? new Intent(this, DashboardActivity.class)
                    : new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return false;
        }
    }

    private void setupRoleSpinner() {
        AppUser currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isCoordinator = currentUser != null && currentUser.getRole() == UserRole.COORDINATOR;

        String[] roles = isCoordinator
                ? new String[]{"Empleado"}
                : new String[]{"Administrador", "Coordinador", "Empleado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, roles);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spRole.setAdapter(adapter);
        spRole.setSelection(roles.length - 1);
        spRole.setEnabled(!isCoordinator);
        spRole.setClickable(!isCoordinator);
    }

    private UserRole getSelectedRole() {
        Object selected = spRole.getSelectedItem();
        if (selected == null) {
            return UserRole.EMPLOYEE;
        }

        String selectedText = selected.toString();
        if ("Administrador".equalsIgnoreCase(selectedText)) {
            return UserRole.ADMIN;
        }
        if ("Coordinador".equalsIgnoreCase(selectedText)) {
            return UserRole.COORDINATOR;
        }
        return UserRole.EMPLOYEE;
    }

    private boolean isSelectedRoleAllowed(UserRole role) {
        AppUser currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        return role == UserRole.EMPLOYEE;
    }
}
