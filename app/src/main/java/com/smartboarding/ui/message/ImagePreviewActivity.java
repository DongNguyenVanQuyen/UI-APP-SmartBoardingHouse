package com.smartboarding.ui.message;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.smartboarding.R;

public class ImagePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView ivFull = findViewById(R.id.ivFullImage);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        Glide.with(this).load(imageUrl).into(ivFull);

        ivFull.setOnClickListener(v -> finish());
        findViewById(R.id.btnClosePreview).setOnClickListener(v -> finish());
    }
}