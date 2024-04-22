package com.example.graduationproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.graduationproject.databinding.ActivitySignup3Binding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

public class SignupActivity3 extends AppCompatActivity {
    private static final String TAG = "SignupActivity3";
    private ActivitySignup3Binding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignup3Binding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        binding.icBack.setOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_right_exit);
        });
        binding.emailhasnotarrived.setOnClickListener(view -> {
            sendEmailVerification();
            Toast.makeText(SignupActivity3.this, "이메일이 다시 발송되었어요.", Toast.LENGTH_SHORT).show();
        });
        binding.btnContinue.setOnClickListener(view -> {
            user.reload();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (user.isEmailVerified()) {
                mAuth.signOut();
                Intent intent = new Intent(SignupActivity3.this, SignupActivity4.class);
                startActivity(intent);
                overridePendingTransition(R.anim.from_right_enter, R.anim.to_left_exit);
            } else {
                Toast.makeText(SignupActivity3.this, "이메일을 인증해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmailVerification() {
        mAuth.setLanguageCode("kr");
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email sent.");
                    }
                });
    }
}