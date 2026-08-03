package com.smartboarding.ui.notification;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.smartboarding.R;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

public class NotificationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_META_JSON = "extra_meta_json"; // JSON string, có thể null

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        View divider = findViewById(R.id.dividerMeta);
        LinearLayout layoutMeta = findViewById(R.id.layoutMeta);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String body = getIntent().getStringExtra(EXTRA_BODY);
        String time = getIntent().getStringExtra(EXTRA_TIME);
        String metaJson = getIntent().getStringExtra(EXTRA_META_JSON);

        tvTitle.setText(title != null ? title : "");
        tvBody.setText(body != null ? body : "");
        tvTime.setText(time != null ? time : "");

        if (metaJson != null) {
            try {
                JSONObject meta = new JSONObject(metaJson);
                Iterator<String> keys = meta.keys();
                boolean any = false;
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = meta.opt(key);
                    if (value == null) continue;
                    any = true;

                    TextView row = new TextView(this);
                    row.setTextSize(14f);
                    row.setTextColor(0xFF616161);
                    row.setPadding(0, 0, 0, dp(6));
                    row.setText(labelFor(key) + ": " + value.toString());
                    layoutMeta.addView(row);
                }
                divider.setVisibility(any ? View.VISIBLE : View.GONE);
            } catch (JSONException e) {
                divider.setVisibility(View.GONE);
            }
        } else {
            divider.setVisibility(View.GONE);
        }
    }

    private String labelFor(String key) {
        switch (key) {
            case "roomNumber": return "Phòng";
            case "customerName": return "Khách hàng";
            case "phone": return "Số điện thoại";
            case "bookingId": return "Mã đặt phòng";
            default: return key;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}