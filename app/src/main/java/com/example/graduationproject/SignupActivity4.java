package com.example.graduationproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.graduationproject.databinding.ActivitySignup4Binding;

public class SignupActivity4 extends AppCompatActivity {
    private ActivitySignup4Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignup4Binding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SignupActivity1 signupActivity1 = (SignupActivity1) SignupActivity1.signupActivity1;
        SignupActivity2 signupActivity2 = (SignupActivity2) SignupActivity2.signupActivity2;
        SignupActivity3 signupActivity3 = (SignupActivity3) SignupActivity3.signupActivity3;

        binding.icBack.setOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_right_exit);
        });
        binding.icClose.setOnClickListener(view -> {
            finish();
            signupActivity1.finish();
            signupActivity2.finish();
            signupActivity3.finish();
            overridePendingTransition(R.anim.none, R.anim.to_down_exit);
        });
        binding.btnContinue.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity4.this, LoginActivity.class);
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_down_exit);
            startActivity(intent);
        });
    }
}