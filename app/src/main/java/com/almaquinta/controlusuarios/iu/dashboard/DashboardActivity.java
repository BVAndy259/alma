package com.almaquinta.controlusuarios.iu.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.controlusuarios.R;
import com.almaquinta.controlusuarios.iu.register.RegisterActivity;
import com.almaquinta.controlusuarios.RendimientoCampanaActivity;

public class DashboardActivity extends AppCompatActivity {
    private Button btnRegistrar;
    private Button btnRendimiento;

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

            btnRegistrar = findViewById(R.id.btnRegister);
            btnRegistrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(DashboardActivity.this, RegisterActivity.class));
                }
            });

            btnRendimiento = findViewById(R.id.btnRendimiento);
            btnRendimiento.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(DashboardActivity.this, RendimientoCampanaActivity.class));
                }
            });
        }
    }
}