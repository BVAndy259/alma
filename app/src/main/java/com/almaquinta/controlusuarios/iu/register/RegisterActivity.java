package com.almaquinta.controlusuarios.iu.register;

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
import com.almaquinta.controlusuarios.iu.dashboard.DashboardActivity;
import com.almaquinta.controlusuarios.iu.main.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etLastName, etEmail, etPassword;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private TextView tvLogin;
    private Button btnRegister;
    private String name = "", lastName = "", email = "", password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etName = findViewById(R.id.etRegisterName);
        etLastName = findViewById(R.id.etLastNameRegister);
        etEmail = findViewById(R.id.etEmailRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        tvLogin = findViewById(R.id.tvDashboard);
        btnRegister = findViewById(R.id.btnRegister);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setTitle("Espere por favor...");
        progressDialog.setCanceledOnTouchOutside(false);

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
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
        progressDialog.dismiss();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        saveUser();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterActivity.this, "Ocurrió un problema, revisa los campos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUser() {
        progressDialog.setMessage("Guardando Información...");
        progressDialog.dismiss();

        String uId = firebaseAuth.getUid();
        HashMap<String, String> datosusuario = new HashMap<>();
        datosusuario.put("uid", uId);
        datosusuario.put("nombre", name);
        datosusuario.put("apellido", lastName);
        datosusuario.put("correo", email);
        datosusuario.put("password", password);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Usuarios");
        databaseReference.child(uId).setValue(datosusuario).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(RegisterActivity.this, "Usuario Creado Exitosamente", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterActivity.this, "Ocurrió un problema al guardar" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}