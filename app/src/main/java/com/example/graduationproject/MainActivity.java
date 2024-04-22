package com.example.graduationproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.example.graduationproject.databinding.ActivityMainBinding;
import com.example.graduationproject.fragment.CallFragment;
import com.example.graduationproject.fragment.HomeFragment;
import com.example.graduationproject.fragment.ManualFragment;
import com.example.graduationproject.fragment.MapFragment;
import com.example.graduationproject.fragment.MapsFragment;
import com.example.graduationproject.fragment.MypageFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FragmentManager fragmentManager;
    private HomeFragment homeFragment;
    private MapsFragment mapsFragment;
    private ManualFragment manualFragment;
    private CallFragment callFragment;
    private MypageFragment mypageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 뷰바인딩 초기화
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.bottomNavigation.setOnApplyWindowInsetsListener(null);

        // Fragment 초기화
        homeFragment = new HomeFragment();
        mapsFragment = new MapsFragment();
        manualFragment = new ManualFragment();
        callFragment = new CallFragment();
        mypageFragment = new MypageFragment();
        
        fragmentManager = getSupportFragmentManager();
        // 첫 시작 page HomeFragment로 설정
        fragmentManager.beginTransaction().replace(R.id.pageView, homeFragment).commit();

        // BottomNavigation <-> Fragment 연결
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_home) {
                fragmentManager.beginTransaction().replace(R.id.pageView, homeFragment).commit();
            } else if (item.getItemId() == R.id.action_map) {
                fragmentManager.beginTransaction().replace(R.id.pageView, mapsFragment).commit();
            } else if (item.getItemId() == R.id.action_manual) {
                fragmentManager.beginTransaction().replace(R.id.pageView, manualFragment).commit();
            } else if (item.getItemId() == R.id.action_call) {
                fragmentManager.beginTransaction().replace(R.id.pageView, callFragment).commit();
            } else if (item.getItemId() == R.id.action_mypage) {
                fragmentManager.beginTransaction().replace(R.id.pageView, mypageFragment).commit();
            }
            return true;
        });

        mAuth = FirebaseAuth.getInstance();
        checkLocationPermissionAndStartService();
        // LocationBasedAlertsManager manager = new LocationBasedAlertsManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("알림 권한 필요");
                builder.setMessage("앱의 중요한 기능을 사용하기 위해서는 알림 권한이 필요합니다.");
                builder.setPositiveButton("설정으로 이동", (dialogInterface, i) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                });
                builder.setNegativeButton("나중에", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void checkLocationPermissionAndStartService() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 위치 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // 권한이 이미 있으면 서비스 시작
            startBackgroundLocationService();
            startBackgroundDisasterMsgService();
        }
    }

    // 백그라운드 위치 추적 서비스 활성화
    private void startBackgroundLocationService() {
        Log.d(TAG, "Starting Background Location Service...");
        Intent serviceIntent = new Intent(this, BackgroundLocationService.class);
        startService(serviceIntent);
    }

    // 백그라운드 재난문자 서비스 활성화
    private void startBackgroundDisasterMsgService() {
        Intent serviceIntent = new Intent(this, BackgroundDisasterMsgService.class);
        startService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여되면 서비스 시작
                startBackgroundLocationService();
                startBackgroundDisasterMsgService();
            } else {
                // 권한 거부 처리
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}