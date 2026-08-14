package com.smartboarding.ui.message;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.smartboarding.R;

public class ImagePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";

    private ImageView ivFull;

    private ScaleGestureDetector scaleGestureDetector;

    private float scaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_preview);

        ivFull = findViewById(R.id.ivFullImage);

        String imageUrl =
                getIntent().getStringExtra(EXTRA_IMAGE_URL);

        // Load ảnh
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(ivFull);

        // Bộ xử lý zoom bằng 2 ngón tay
        scaleGestureDetector =
                new ScaleGestureDetector(
                        this,
                        new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                            @Override
                            public boolean onScale(
                                    ScaleGestureDetector detector
                            ) {

                                scaleFactor *= detector.getScaleFactor();

                                // Giới hạn zoom từ 1x đến 4x
                                scaleFactor = Math.max(
                                        1.0f,
                                        Math.min(
                                                scaleFactor,
                                                4.0f
                                        )
                                );

                                ivFull.setScaleX(scaleFactor);
                                ivFull.setScaleY(scaleFactor);

                                return true;
                            }
                        }
                );

        /*
         * Nếu ảnh đang ở mức 1x:
         * bấm vào ảnh -> đóng màn hình.
         *
         * Nếu ảnh đang zoom:
         * bấm vào ảnh không đóng.
         */
        ivFull.setOnClickListener(v -> {

            if (scaleFactor <= 1.01f) {
                finish();
            }
        });

        // Nút X đóng ảnh
        findViewById(R.id.btnClosePreview)
                .setOnClickListener(v -> finish());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }
}