package com.almaquinta.controlusuarios.iu.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.controlusuarios.R;
import com.almaquinta.controlusuarios.iu.register.RegisterActivity;
import com.almaquinta.controlusuarios.iu.dashboard.DashboardActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private EditText etUser, etPassword;
    private TextView tvDashboard;
    private Button btnRegister;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private String user = "", password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUser = findViewById(R.id.etUserLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnRegister = findViewById(R.id.btnLogin);
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Espere por favor...");
        tvDashboard = findViewById(R.id.tvRegisterLogin);

        tvDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
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
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialog.dismiss();
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                            Toast.makeText(MainActivity.this, "Bienvenido " + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Verifique si el correo o contraseña con correctos", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Ocurrió un problema", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}