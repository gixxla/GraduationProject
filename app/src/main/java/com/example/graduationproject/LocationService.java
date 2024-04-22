package com.example.graduationproject;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationService extends Service {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FirebaseAuth mAuth;
    private Map<String, Integer> regionCodesMap;
    private Map<String, Integer> subRegionCodesMap;
    // LocationBasedAlertsManager 객체 추가
    private LocationBasedAlertsManager alertsManager;
    private DatabaseReference mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        loadRegionCodes();

        // LocationBasedAlertsManager 인스턴스 생성
        alertsManager = new LocationBasedAlertsManager(this);

        // 위치 변경 리스너 정의
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // 위치 변경 시 로그 출력 및 주소 얻기
                Log.d("LocationService", "위치 변경: 위도 = " + location.getLatitude() + ", 경도 = " + location.getLongitude());
                String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
                Log.d("LocationService", "주소: " + address);
                saveUserLocationToDatabase(location.getLatitude(), location.getLongitude(), address);
                saveRegionCodes(location);

                // 위치가 변경될 때마다 재난 메시지를 확인하고 필요한 경우 알림을 전송
                //alertsManager.startMonitoring(); // 현재 사용자와 친구들의 위치에 기반한 재난 메시지 확인 및 알림 발송
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };
        // 위치 업데이트 시작
        startLocationUpdates();

        // 재난 문자 데이터를 주기적으로 가져오기 위한 스케줄러 설정
        startDisasterMessageFetching();

        mDatabase = FirebaseDatabase.getInstance().getReference(); // Firebase Database 참조 초기화
    }

    private void startDisasterMessageFetching() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                fetchDisasterMessages();
            }
        }, 0, 1, TimeUnit.MINUTES); //  1분마다 실행
    }

    private void fetchDisasterMessages() {
        DisasterMsgService service = RetrofitClient.getApiService();

        // 현재로부터 7일 이전 날짜를 계산합니다.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String createDate = dateFormat.format(calendar.getTime());

        // 매개변수에 적절한 값을 넣어 호출합니다.
        // serviceKey와 locationName은 당신의 애플리케이션에 맞게 설정해야 합니다.
        String serviceKey = "2w9UQSJqpf9AiSfSMt69r4wydxHiIW8paG3WPngYivNPW0Ci0JnQL5+s340/PIRtTc1Ppcj2A3Hfe1w0NeoA7g==";
        int pageNo = 1;
        int numOfRows = 10;
        String type = "xml";
        String locationName = ""; // 사용자 위치 기반 지역 이름 조회는 제거되었습니다.

        service.getDisasterMsg(serviceKey, pageNo, numOfRows, type, createDate, locationName).enqueue(new Callback<DisasterMsgResponse>() {
            @Override
            public void onResponse(Call<DisasterMsgResponse> call, Response<DisasterMsgResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveDisasterMessagesToFirebase(response.body());
                }
            }

            @Override
            public void onFailure(Call<DisasterMsgResponse> call, Throwable t) {
                // API 호출 실패 처리
                Log.e("LocationService", "API 호출 실패: " + t.getMessage());
            }
        });
    }


    private void saveDisasterMessagesToFirebase(DisasterMsgResponse disasterMsgResponse) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("disasterMessages");
        for (DisasterMsgResponse.DisasterMsg msg : disasterMsgResponse.getRow()) {
            String key = msg.getMd101Sn();
            dbRef.child(key).setValue(msg).addOnSuccessListener(aVoid -> {
                Log.d("LocationService", "Firebase에 재난 메시지가 성공적으로 저장되었습니다: " + key);
                // 자식 개수 확인 및 가장 오래된 데이터 삭제 로직 추가
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getChildrenCount() > 20) {
                            DataSnapshot oldestChild = null;
                            String oldestDate = "";

                            // 가장 오래된 메시지 찾기
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                String createDate = child.child("createDate").getValue(String.class);
                                if (oldestChild == null || (createDate != null && createDate.compareTo(oldestDate) < 0)) {
                                    oldestChild = child;
                                    oldestDate = createDate;
                                }
                            }

                            // 가장 오래된 메시지 삭제
                            if (oldestChild != null && oldestChild.getKey() != null) {
                                dbRef.child(oldestChild.getKey()).removeValue()
                                        .addOnFailureListener(e -> Log.e("LocationService", "Firebase에서 재난 메시지 삭제 실패", e));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("LocationService", "자식 개수 확인 및 데이터 삭제 과정에서 오류 발생", databaseError.toException());
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("LocationService", "Firebase에 재난 메시지 저장 실패: ", e);
            });
        }
    }



    private void loadRegionCodes() {
        regionCodesMap = new HashMap<>();
        subRegionCodesMap = new HashMap<>();
        String json = null;
        try {
            InputStream is = getAssets().open("region_codes.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

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


    // 위치 정보가 변경될 때마다 지역 코드를 찾아 Firebase에 저장하는 메서드
    private void saveRegionCodes(Location location) {
        String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
        Log.d("LocationService", "받아온 주소: " + address); // 주소 로그 출력
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("location");
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("address", address);

            if (generalRegionCode != null) locationData.put("generalRegionCode", generalRegionCode);
            if (specificRegionCode != null) locationData.put("specificRegionCode", specificRegionCode);

            // 데이터 쓰기 시도 및 로그 출력
            userRef.setValue(locationData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("LocationService", "Firebase에 지역 코드가 성공적으로 저장되었습니다.");
                    updateFriendsLocationWithMyRegionCodes(userId, generalRegionCode, specificRegionCode);
                } else {
                    Log.e("LocationService", "Firebase에 지역 코드 저장 실패", task.getException());
                }
            });
        } else {
            Log.d("LocationService", "사용자가 로그인하지 않았습니다.");
        }
    }

    private void updateFriendsLocationWithMyRegionCodes(String myUserId, Integer myGeneralRegionCode, Integer mySpecificRegionCode) {
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("users").child(myUserId).child("friends");
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();
                    DatabaseReference friendLocationRef = FirebaseDatabase.getInstance().getReference("users").child(friendUid).child("friends").child(myUserId).child("location");
                    Map<String, Object> friendLocationData = new HashMap<>();
                    friendLocationData.put("generalRegionCode", myGeneralRegionCode);
                    friendLocationData.put("specificRegionCode", mySpecificRegionCode);

                    friendLocationRef.updateChildren(friendLocationData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("LocationService", "Successfully updated friend's location with my region codes");
                        } else {
                            Log.e("LocationService", "Failed to update friend's location with my region codes", task.getException());
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("LocationService", "Failed to fetch friends list", databaseError.toException());
            }
        });
    }



    // 위도와 경도를 입력받아 주소를 반환하는 메소드
    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.KOREA); // 한국어 주소 정보를 얻기 위해 Locale 설정
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // 주소를 String으로 변환
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i)); // 주소의 각 라인을 추가
                    if (i != address.getMaxAddressLineIndex()) {
                        sb.append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (IOException e) {
            Log.e("LocationService", "서비스에서 주소 변환 실패", e);
        }
        return "주소를 찾을 수 없음";
    }
    // 위치 업데이트 요청 메소드
    private void startLocationUpdates() {
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "위치 권한이 없습니다.");
            return;
        }
        // 권한이 있으면 위치 업데이트 요청
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 50, locationListener);
    }

    // Firebase Realtime Database에 사용자 위치 저장
    private void saveUserLocationToDatabase(double latitude, double longitude, String address) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("location");
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("address", address); // 주소 추가

            // SharedPreferences를 사용하여 주소 저장
            SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("UserAddress", address);
            editor.apply();

            userRef.setValue(locationData)
                    .addOnSuccessListener(aVoid -> Log.d("LocationService", "사용자 위치 데이터 저장 성공: " + userId))
                    .addOnFailureListener(e -> Log.e("LocationService", "사용자 위치 데이터 저장 실패", e));
        } else {
            Log.d("LocationService", "로그인하지 않은 사용자는 위치 데이터를 저장할 수 없습니다.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 포그라운드 서비스로 알림 표시
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = alertsManager.getChannelId();
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("위치 추적 서비스 실행 중")
                .setContentText("백그라운드에서 위치 추적 중")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // 사용자의 위치 모니터링 시작
        //alertsManager.startMonitoring();

        // 친구들의 위치 모니터링 시작
        //alertsManager.startMonitoringForFriends();

        return START_STICKY; // 변경된 부분
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 바인딩된 서비스가 아니므로 null 반환
    }

}
