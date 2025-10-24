package com.service.billupdateblue;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class ThirdActivity extends BaseActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        int form_id = getIntent().getIntExtra("form_id", -1);
        Button submit = findViewById(R.id.btnProceed);
        submit.setOnClickListener(v -> {
            Intent intent = new Intent(this, FourthActivity.class);
            intent.putExtra("form_id", form_id);
            startActivity(intent);
        });
    }
}
