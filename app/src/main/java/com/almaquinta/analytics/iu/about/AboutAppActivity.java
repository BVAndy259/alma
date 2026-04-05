package com.almaquinta.analytics.iu.about;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.iu.common.SystemBarsEdgeToEdge;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);
        SystemBarsEdgeToEdge.apply(this);


        ImageView btnBack = findViewById(R.id.btnBackAbout);
        btnBack.setOnClickListener(v -> finish());
    }
}

