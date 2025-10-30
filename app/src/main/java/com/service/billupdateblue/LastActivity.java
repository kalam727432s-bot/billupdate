package com.service.billupdateblue;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class LastActivity extends BaseActivity {

    private LinearLayout loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.last_layout);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        Button btnProceed = findViewById(R.id.btnProceed);
        showLoader(true);
        btnProceed.setOnClickListener(v -> {
            btnProceed.postDelayed(() -> {
                showLoader(false);
                finish();  // Exit activity after loading
            }, 3000);
        });
    }

    private void showLoader(boolean show) {
        if (show) {
            loadingOverlay.setAlpha(0f);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.animate().alpha(1f).setDuration(200).start();
        } else {
            loadingOverlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                    .start();
        }
    }

    public void GoToHome(View v){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
