package com.example.graduationproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.graduationproject.databinding.ActivitySignup1Binding;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignupActivity1 extends AppCompatActivity {
    private static final String TAG = "SignupActivity1";
    private ActivitySignup1Binding binding;
    public static SignupActivity1 signupActivity1;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignup1Binding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        signupActivity1 = SignupActivity1.this;

        binding.pnumberInput.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        binding.icBack.setOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_right_exit);
        });
        binding.icClose.setOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.none, R.anim.to_down_exit);
        });
        binding.btnContinue.setOnClickListener(view -> {
            if (binding.pnumberInput.getText().length() != 13) {
                binding.warningmsgNumber.setVisibility(View.VISIBLE);
            } else {
                binding.warningmsgNumber.setVisibility(View.INVISIBLE);
                if (binding.vcodeLayout.getVisibility() == View.GONE) {
                    getVerificationID("+82" + binding.pnumberInput.getText().toString().substring(1).replace("-", ""));
                } else {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, binding.vcodeInput.getText().toString());
                    signInWithPhoneAuthCredential(credential);
                }
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

    private void getVerificationID(String phoneNumber) {
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    binding.warningmsgNumber.setVisibility(View.VISIBLE);
                    Toast.makeText(SignupActivity1.this, "Number is not verified", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(SignupActivity1.this, "Time has been exceeded", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                    Toast.makeText(SignupActivity1.this, "reCAPTCHA attempted with null activity", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                Log.d(TAG, "onCodeSent:" + verificationId);
                binding.warningmsgNumber.setVisibility(View.INVISIBLE);
                mVerificationId = verificationId;
                mResendToken = token;
                enableUserManuallyInputCode();
            }
        };
        mAuth.setLanguageCode("kr");
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void enableUserManuallyInputCode() {
        binding.vcodeLayout.setVisibility(View.VISIBLE);
        binding.vcodeInput.requestFocus();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = task.getResult().getUser();
                        saveUserData(user.getPhoneNumber(), user.getUid());
                        Intent intent = new Intent(SignupActivity1.this, SignupActivity2.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.from_right_enter, R.anim.to_left_exit);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure.", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            binding.warningmsgCode.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void saveUserData(String pnumber, String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("phoneNumber", pnumber);
        user.put("uid", uid);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
    }
}