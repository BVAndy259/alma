package com.almaquinta.controlusuarios;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout lblUsuarioLogin;
    private TextInputLayout lblPasswordLogin;
    private Button button;
    private TextView txtRegistro;
    private int intentosFallidos = 0;
    private final int MAX_INTENTOS = 3;

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

        lblUsuarioLogin = findViewById(R.id.lblUsuarioLogin);
        lblPasswordLogin = findViewById(R.id.lblPasswordLogin);

        lblUsuarioLogin.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                lblUsuarioLogin.setError(null);
            }
        });

        lblPasswordLogin.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                lblPasswordLogin.setError(null);
            }
        });

        txtRegistro = findViewById(R.id.txtRegistroLogin);
        txtRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegistroActivity.class));
            }
        });

        button = findViewById(R.id.btnLogin);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usuario = lblUsuarioLogin.getEditText().getText().toString().trim();
                String contrasena = lblPasswordLogin.getEditText().getText().toString().trim();
                boolean error = false;

                lblUsuarioLogin.setError(null);
                lblPasswordLogin.setError(null);

                if (usuario.isEmpty()) {
                    lblUsuarioLogin.setError("Campo requerido");
                    error = true;
                } else if (!usuario.contains("@")) {
                    lblUsuarioLogin.setError("Correo inválido. Debe contener @.");
                    error = true;
                }

                if (contrasena.isEmpty()) {
                    lblPasswordLogin.setError("Campo requerido");
                    error = true;
                } else if (contrasena.length() < 8) {
                    lblPasswordLogin.setError("La contraseña debe tener al menos 8 caracteres.");
                    error = true;
                }

                if (error) {
                    return;
                }

                /*if (usuario.isEmpty() || contrasena.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!usuario.contains("@")) {
                    Toast.makeText(MainActivity.this, "Correo inválido. El correo debe contener @", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (contrasena.length() < 8) {
                    Toast.makeText(MainActivity.this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }*/

                if (usuario.equals("admin@email.com") && contrasena.equals("admin123")) {
                    intentosFallidos = 0;
                    Toast.makeText(MainActivity.this, "¡Bienvenido!", Toast.LENGTH_LONG).show();

                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                    finish();
                    return;
                }

                intentosFallidos++;
                int restantes = MAX_INTENTOS - intentosFallidos;

                if (intentosFallidos >= MAX_INTENTOS) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Cuenta bloqueada")
                            .setMessage("Has superado el límite de " + MAX_INTENTOS + " intentos.\nLa aplicación se cerrará.")
                            .setCancelable(false)
                            .setPositiveButton("Aceptar", (dialog, which) -> finishAffinity())
                            .show();

                    button.setEnabled(false);
                } else {
                    String mensaje = "Credenciales incorrectas.\n" +
                            "Intento " + intentosFallidos + " de " + MAX_INTENTOS + ".\n" +
                            "Te quedan " + restantes + " intento" + (restantes == 1 ? "" : "s") + ".";

                    Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}