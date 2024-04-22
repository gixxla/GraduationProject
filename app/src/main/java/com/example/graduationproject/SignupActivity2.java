package com.example.graduationproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.graduationproject.databinding.ActivitySignup2Binding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity2 extends AppCompatActivity {
    private static final String TAG = "SignupActivity2";
    private ActivitySignup2Binding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String email, pw, pwverification;
    private final String emailValidation = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private final String pwValidation = "^(?=.*[a-zA-Z0-9])(?=.*[a-zA-Z!@#$%^&*])(?=.*[0-9!@#$%^&*]).{8,16}$";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignup2Binding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.icBack.setOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_right_exit);
        });
        binding.btnContinue.setOnClickListener(view -> {
            email = binding.emailInput.getText().toString();
            pw = binding.pwInput.getText().toString();
            pwverification = binding.pwverifyInput.getText().toString();
            if (!email.matches(emailValidation)) {
                binding.warningmsgEmail.setVisibility(View.VISIBLE);
                binding.warningmsgPW.setVisibility(View.INVISIBLE);
                binding.warningmsgPWverify.setVisibility(View.INVISIBLE);
                if (!pw.matches(pwValidation)) {
                    binding.warningmsgPW.setVisibility(View.VISIBLE);
                    if (!pwverification.equals(pw)) {
                        binding.warningmsgPWverify.setVisibility(View.VISIBLE);
                    }
                }
                if (!pwverification.equals(pw)) {
                    binding.warningmsgPWverify.setVisibility(View.VISIBLE);
                }
            } else if (!pw.matches(pwValidation)) {
                binding.warningmsgEmail.setVisibility(View.INVISIBLE);
                binding.warningmsgPW.setVisibility(View.VISIBLE);
                binding.warningmsgPWverify.setVisibility(View.INVISIBLE);
                if (!pwverification.equals(pw)) {
                    binding.warningmsgPWverify.setVisibility(View.VISIBLE);
                }
            } else if (!pwverification.equals(pw)) {
                binding.warningmsgEmail.setVisibility(View.INVISIBLE);
                binding.warningmsgPW.setVisibility(View.INVISIBLE);
                binding.warningmsgPWverify.setVisibility(View.VISIBLE);
            } else {
                binding.warningmsgEmail.setVisibility(View.INVISIBLE);
                binding.warningmsgPW.setVisibility(View.INVISIBLE);
                binding.warningmsgPWverify.setVisibility(View.INVISIBLE);
                linkWithPhoneNumber(email, pw);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) && view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom())
                ((InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow((this.getWindow().getDecorView().getApplicationWindowToken()), 0);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void linkWithPhoneNumber(String email, String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "linkWithCredential:success");
                        user = task.getResult().getUser();
                        saveUserData(user.getEmail(), user.getUid());
                        sendEmailVerification();
                        Intent intent = new Intent(SignupActivity2.this, SignupActivity3.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.from_right_enter, R.anim.to_left_exit);
                    } else {
                        Log.w(TAG, "linkWithCredential:failure", task.getException());
                        Toast.makeText(SignupActivity2.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        // updateUI(null);
                    }
                });
    }

    private void saveUserData(String email, String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);

        db.collection("users").document(uid).set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
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