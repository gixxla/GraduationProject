package com.example.graduationproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BackgroundLocationService extends Service implements LocationListener {
    private static final String TAG = "BackgroundLocationService";
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MainActivity mainActivity;
    private boolean stopService = false;
    private Map<String, Integer> regionCodesMap;
    private Map<String, Integer> subRegionCodesMap;
    protected LocationSettingsRequest mLocationSettingsRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        // 일괄 초기화
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        mainActivity = new MainActivity();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.e(TAG, "Location Received");
                mCurrentLocation = locationResult.getLastLocation();
                onLocationChanged(mCurrentLocation);
            }
        };
        loadRegionCodes();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        startLocationUpdates();
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!stopService) {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!stopService) {
                        handler.postDelayed(this, TimeUnit.SECONDS.toMillis(10));
                    }
                }
            }
        };
        handler.postDelayed(runnable, 2000);

        return START_STICKY;
    }

    private void startForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // 임시 이름 (추후 수정 예정)
        String CHANNEL_ID = "channel_location";
        String CHANNEL_NAME = "channel_location";

        NotificationCompat.Builder builder;
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(mainActivity.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
            builder.setChannelId(CHANNEL_ID);
            builder.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE);
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        }

        Uri notificationSound = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
        builder.setContentTitle("Graduation-Project")
                .setContentText("백그라운드 위치 추적 서비스 실행 중")
                .setSound(notificationSound)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_refresh) // 앱 아이콘으로 변경
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();
        startForeground(101, notification);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "위치 권한이 없습니다.");
            return;
        }

        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).setMinUpdateIntervalMillis(5000).build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();

        mSettingsClient = LocationServices.getSettingsClient(this);
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    Log.d(TAG, "GPS Success.");
                    requestLocationUpdate();
                }).addOnFailureListener(e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                int REQUEST_CHECK_SETTINGS = 214;
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult((AppCompatActivity) mainActivity, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.e(TAG, "Unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Log.e(TAG, "Location settings are inadequate, and cannot be fixed here. Fix in Settings.");
                    }
                }).addOnCanceledListener(() -> Log.e(TAG, "checkLocationSettings -> onCanceled"));
    }
    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String address = getAddressFromLocation(latitude, longitude);
        saveUserLocationToDatabase(latitude, longitude, address);
        saveRegionCodes(location);

        if (latitude == 0.0 && longitude == 0.0) {
            requestLocationUpdate();
        } else {
            Log.d(TAG, "위치 변경: 위도 = " + latitude + ", 경도 = " + longitude);
            Log.d(TAG, "주소: " + address);
            broadcastUserLocation(address);
        }
    }
    @SuppressLint("MissingPermission")
    private void requestLocationUpdate() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
                .addOnCompleteListener(task -> Log.d(TAG, "Result: " + task.getResult()))
                .addOnFailureListener(e -> Log.d(TAG, e.getMessage()));
    }

    // 현재 사용자 위치를 브로드캐스트
    private void broadcastUserLocation(String address) {
        Intent intent = new Intent("userLocation");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("location", address);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
        String result;
        try {
            List<Address> address = geocoder.getFromLocation(latitude, longitude, 1);
            if (address != null && !address.isEmpty()) {
                result = address.get(0).getAddressLine(0).toString();
            } else {
                result = "주소 확인 불가";
            }
        } catch (IOException e) {
            Log.e("LocationService", "서비스에서 주소 변환 실패", e);
            result = "주소 변환 불가";
        }
        return result;
    }

    private void saveUserLocationToDatabase(double latitude, double longitude, String address) {
        user = mAuth.getCurrentUser();
        if (user != null) {
            String userUid = user.getUid();
            Map<String, Object> location = new HashMap<>();
            location.put("latitude", latitude);
            location.put("longitude", longitude);
            location.put("address", address);
            db.collection("users").document(userUid).set(location, SetOptions.merge())
                    .addOnSuccessListener(unused -> Log.d(TAG, "사용자 위치 데이터 저장 성공: " + userUid))
                    .addOnFailureListener(e -> Log.e(TAG, "사용자 위치 데이터 저장 실패", e));
        } else {
            Log.d(TAG, "로그인하지 않은 사용자는 위치 데이터를 저장할 수 없습니다.");
        }
    }

    private void loadRegionCodes() {
        regionCodesMap = new HashMap<>();
        subRegionCodesMap = new HashMap<>();
        try {
            InputStream is = getAssets().open("region_codes.json");
            byte[] b = new byte[is.available()];
            is.read(b);
            is.close();
            String json = new String(b, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String state = obj.getString("발송지역(시도)");
                String county = obj.getString("발송지역(시군구)");
                String subCounty = obj.getString("법정동(읍면동)");
                int regionCode = obj.getInt("지역코드(Location_ID)");

                if (county.equals("해당 시도 전체")) {
                    regionCodesMap.put(state, regionCode); // '시도' 전체에 대한 지역 코드
                } else if (!subCounty.equals("해당 시군구 전체")) {
                    subRegionCodesMap.put(county + " " + subCounty, regionCode); // 세부 지역 코드
                } else {
                    subRegionCodesMap.put(county, regionCode); // '시군구' 전체에 대한 지역 코드
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveRegionCodes(Location location) {
        String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
        String[] addressParts = address.split(" ");

        if (addressParts.length < 2) {
            Log.d("LocationService", "주소가 충분히 세분화되지 않았습니다: " + address);
            return; // 주소가 충분히 세분화되지 않은 경우를 처리
        }

        String stateKey = addressParts[1]; // '시도' 이름 추출
        String countyKey = addressParts.length > 2 ? addressParts[2] : ""; // '시군구' 이름 추출, 가능한 경우에만

        Log.d("LocationService", "검색할 시도 키: " + stateKey + ", 시군구 키: " + countyKey);

        Integer generalRegionCode = regionCodesMap.get(stateKey);
        Integer specificRegionCode = subRegionCodesMap.get(countyKey);

        // 로그를 통해 지역 코드를 확인
        Log.d("LocationService", "일반 지역 코드: " + generalRegionCode + ", 세부 지역 코드: " + specificRegionCode);

        // Firebase에 저장하는 로직
        user = mAuth.getCurrentUser();
        if (user != null) {
            String userUid = user.getUid();
           Map<String, Object> locationData = new HashMap<>();
            if (generalRegionCode != null) locationData.put("generalRegionCode", generalRegionCode);
            if (specificRegionCode != null) locationData.put("specificRegionCode", specificRegionCode);

            // 데이터 쓰기 시도 및 로그 출력
            db.collection("users").document(userUid).set(locationData, SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "사용자 위치 지역 코드 저장 성공");
                            // updateFriendsLocationWithMyRegionCodes(userUid, generalRegionCode, specificRegionCode);
                        } else {
                            Log.e(TAG, "사용자 위치 지역 코드 저장 실패", task.getException());
                        }
                    });
        } else {
            Log.d(TAG, "사용자가 로그인하지 않았습니다.");
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "백그라운드 위치 추적이 중지되었습니다.");
        stopService = true;
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Log.e(TAG, "Location Update Callback Removed.");
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
