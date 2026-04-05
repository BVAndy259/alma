package com.almaquinta.analytics.iu.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.iu.common.SystemBarsEdgeToEdge;

public class AboutAppActivity extends AppCompatActivity {

    private static final String GITHUB_URL = "https://github.com/BVAndy259", YOUTUBE_URL = "https://www.youtube.com/@destructor_777";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);
        SystemBarsEdgeToEdge.apply(this);


        ImageView btnBack = findViewById(R.id.btnBackAbout);
        ImageButton githubIcon = findViewById(R.id.githubIcon);
        ImageButton youtubeIcon = findViewById(R.id.youtubeIcon);

        btnBack.setOnClickListener(v -> finish());
        githubIcon.setOnClickListener(v -> openExternalUrl(GITHUB_URL));
        youtubeIcon.setOnClickListener(v -> openExternalUrl(YOUTUBE_URL));
    }

    private void openExternalUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
        }
    }
}

