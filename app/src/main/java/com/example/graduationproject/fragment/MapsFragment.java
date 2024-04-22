package com.example.graduationproject.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.graduationproject.MainActivity;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentMapsBinding;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MapsFragment";
    private FragmentMapsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MainActivity mainActivity;
    private MapView mapView;
    private NaverMap mMap;
    private double latitude, longitude;
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final float MAX_DISTANCE = 10000;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 일괄 초기화
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        // 위치 정보 제공자 초기화
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        LocalBroadcastManager.getInstance(mainActivity).registerReceiver(mMessageReceiver, new IntentFilter("userLocation"));
        Log.d(TAG, "onCreate: MapsFragment started.");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 뷰바인딩 초기화
        binding = FragmentMapsBinding.inflate(inflater, container, false);
        // MapView 초기화
        mapView = binding.map;
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        try {
            MapsInitializer.initialize(mainActivity.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission granted.");
        } else {
            Log.d(TAG, "Location permission not granted. Requesting permission...");
            // 여기서 위치 권한을 요청할 수 있습니다.
        }
    }

    // MapView 생명 주기
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Log.d(TAG, "onResume: MapView resumed.");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: MapsActivity paused.");
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mMap = naverMap;

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setLocationButtonEnabled(true);
        mMap.setLocationSource(locationSource);
        mMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        // 위치 변경 시 위도, 경도 받아와서 카메라 시점 이동
        mMap.addOnLocationChangeListener(location -> {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            mMap.moveCamera(CameraUpdate.scrollTo(new LatLng(lat, lng)));
            mMap.moveCamera(CameraUpdate.zoomTo(16));
        });

        loadAndAddShelterMarkers();
        loadAndAddAEDMarkers();
    }

    // 위치 브로드캐스트 리시버
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            Log.d("receiver", "Got latitude: " + latitude + ", Got longitude: " + longitude);
        }
    };

    private void loadAndAddShelterMarkers() {
        try {
            // 대피소 위치 데이터 파일 로드
            InputStream is = mainActivity.getAssets().open("LOCALDATA_ALL_12_04_12_E.json");
            byte[] b = new byte[is.available()];
            is.read(b);
            is.close();
            String json = new String(b, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);
            Marker[] markers = new Marker[jsonArray.length()];

            // JSON 배열을 순회하며 각 위치에 마커를 추가
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String status = obj.getString("운영상태");
                String name = obj.getString("시설명");

                // "사용중"인 대피소만을 대상으로 하며, 특정 조건(예: 시청, 학교, 지하철역)을 만족하는 경우만 마커 추가
                if ("사용중".equals(status) && (name.contains("시청") || name.contains("초등학교") ||
                        name.contains("중학교") || name.contains("소방서")|| name.contains("대피시설")|| name.contains("고등학교") || name.contains("지하철역"))) {
                    double lat = obj.getDouble("위도(EPSG4326)");
                    double lng = obj.getDouble("경도(EPSG4326)");
                    LatLng location = new LatLng(lat, lng);

                    markers[i] = new Marker(location, MarkerIcons.RED);
                    markers[i].setCaptionText(name);
                    markers[i].setMap(mMap);
                }
            }
        } catch (Exception e){
            Toast.makeText(mainActivity, "마커 데이터 로딩 중 오류 발생", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadAndAddAEDMarkers() {
        new Thread(() -> {
            try (InputStream is = mainActivity.getAssets().open("aed_data.json");
                 JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.beginArray();

                while (reader.hasNext()) {
                    double lat = 0, lng = 0;
                    String institutionName = null;

                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("위도")) {
                            lat = reader.nextDouble();
                        } else if (name.equals("경도")) {
                            lng = reader.nextDouble();
                        } else if (name.equals("설치기관명")) {
                            institutionName = reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();

                    float[] results = new float[1];
                    Location.distanceBetween(latitude, longitude, lat, lng, results);
                    float distanceInMeters = results[0];

                    if (distanceInMeters <= MAX_DISTANCE) {
                        LatLng aedLocation = new LatLng(lat, lng);
                        final String finalInstitutionName = institutionName;
                        mainActivity.runOnUiThread(() -> {
                            if (mMap != null) {
                                Marker marker = new Marker(aedLocation, MarkerIcons.GREEN);
                                marker.setCaptionText(finalInstitutionName);
                                marker.setMap(mMap);
                            }
                        });
                    }
                }
                reader.endArray();
            } catch (Exception e) {
                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity, "AED 마커 데이터 로딩 중 오류 발생", Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    // 수정 중
//    private void loadAndDisplayFriendsLocations() {
//        user = mAuth.getCurrentUser();
//        if (user != null) {
//            String userUid = user.getUid();
//            String[] friendsUid;
//            DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("users").child(myUid).child("friends");
//
//            friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
//                        String friendUid = friendSnapshot.getKey();
//
//                        DatabaseReference friendLocationRef = FirebaseDatabase.getInstance().getReference("users").child(friendUid).child("location");
//                        friendLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot locationSnapshot) {
//                                Double latitude = locationSnapshot.child("latitude").getValue(Double.class);
//                                Double longitude = locationSnapshot.child("longitude").getValue(Double.class);
//                                String name = locationSnapshot.child("name").getValue(String.class);
//
//                                if (latitude != null && longitude != null) {
//                                    com.google.android.gms.maps.model.LatLng location = new com.google.android.gms.maps.model.LatLng(latitude, longitude);
//
//                                    // 사용자 정의 마커 이미지로 마커를 생성합니다.
//                                    MarkerOptions markerOptions = new MarkerOptions()
//                                            .position(location)
//                                            .title(name)
//                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.custom_marker)); // 여기서 사용자 정의 이미지를 설정합니다.
//
//                                    mMap.addMarker(markerOptions);
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) {
//                                Log.e("MapsActivity", "데이터베이스 오류: " + databaseError.getMessage());
//                            }
//                        });
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    Log.e("MapsActivity", "데이터베이스 오류: " + databaseError.getMessage());
//                }
//            });
//        }
//    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        binding = null;
    }
}