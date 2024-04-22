package com.example.graduationproject.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.graduationproject.LocationService;
import com.example.graduationproject.MainActivity;
import com.example.graduationproject.databinding.FragmentMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.graduationproject.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private FragmentMapBinding binding;
    private MainActivity mainActivity;
    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: MapFragment started.");
        // 위치 정보 제공자 초기화
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMapBinding.inflate(inflater, container, false);
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
        requestLocationPermission();
        // 권한이 이미 부여되었으면 위치 업데이트 서비스 시작
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission granted.");
            startLocationService();
        } else {
            Log.d(TAG, "Location permission not granted. Requesting permission...");
            // 여기서 위치 권한을 요청할 수 있습니다.
        }
    }

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
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (fusedLocationProviderClient != null) {
                startLocationUpdates();
            } else {
                Log.e(TAG, "fusedLocationProviderClient is null in onResume.");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: MapsActivity paused.");
        stopLocationUpdates();
        mapView.onPause();
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

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10(Q) 이상에서 백그라운드 위치 권한 요청
            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 위치 권한이 부여되었다면 서비스 시작
                startLocationService();
            } else {
                // 권한 거부 처리
                Toast.makeText(getActivity(), "위치 권한이 필요합니다", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    // 수정 중
    public ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLocationService();
                } else {
                    Toast.makeText(getActivity(), "위치 권한이 필요합니다", Toast.LENGTH_LONG).show();
                }
            });


    // 위치 업데이트 서비스를 시작하는 메서드
    private void startLocationService() {
        Log.d(TAG, "Starting Location Service...");
        Intent serviceIntent = new Intent(getActivity(), LocationService.class);
        mainActivity.startService(serviceIntent);
    }

    // Google Maps가 준비되었을 때 호출되는 콜백 메서드
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map is ready.");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        enableMyLocation();
        loadLocationsAndAddMarkers();
        loadAndAddAEDMarkers();
        // 친구 위치를 불러와 표시하는 함수 호출
        // loadAndDisplayFriendsLocations();
    }
    // JSON 파일에서 읽은 위치 데이터를 사용하여 지도에 마커를 추가하는 메서드
    private void loadLocationsAndAddMarkers() {
        try {
            // assets 폴더에서 대피소 위치 데이터 파일을 로드합니다.
            InputStream is = mainActivity.getAssets().open("LOCALDATA_ALL_12_04_12_E.json");
            byte[] b = new byte[is.available()];
            is.read(b);
            is.close();
            String json = new String(b, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            // JSON 배열을 순회하며 각 위치에 마커를 추가합니다.
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String status = obj.getString("운영상태");
                String name = obj.getString("시설명");

                // "사용중"인 대피소만을 대상으로 하며, 특정 조건(예: 시청, 학교, 지하철역)을 만족하는 경우에 마커를 추가합니다.
                if ("사용중".equals(status) && (name.contains("시청") || name.contains("초등학교") ||
                        name.contains("중학교") || name.contains("소방서")|| name.contains("대피시설")|| name.contains("고등학교") || name.contains("지하철역"))) {
                    double lat = obj.getDouble("위도(EPSG4326)");
                    double lng = obj.getDouble("경도(EPSG4326)");
                    LatLng location = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions().position(location).title(name));
                }
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "마커 데이터 로딩 중 오류 발생", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // AED 위치 데이터를 로드하고 지도에 마커를 추가하는 메서드
    private void loadAndAddAEDMarkers() {
        try {
            // assets 폴더에서 AED 위치 데이터 파일을 로드합니다.
            InputStream is = mainActivity.getAssets().open("aed_data.json");
            byte[] b = new byte[is.available()];
            is.read(b);
            is.close();
            String json = new String(b, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            // JSON 배열을 순회하며 각 위치에 마커를 추가합니다.
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                double lat = obj.getDouble("위도");
                double lng = obj.getDouble("경도");
                String institutionName = obj.getString("설치기관명");

                LatLng location = new LatLng(lat, lng);
                mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(institutionName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // 마커 색 변경
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "AED 마커 데이터 로딩 중 오류 발생", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // 친구들의 위치를 불러와 지도에 표시하는 메서드
    private void loadAndDisplayFriendsLocations() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String myUid = currentUser.getUid();
            DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("users").child(myUid).child("friends");

            friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                        String friendUid = friendSnapshot.getKey();

                        DatabaseReference friendLocationRef = FirebaseDatabase.getInstance().getReference("users").child(friendUid).child("location");
                        friendLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot locationSnapshot) {
                                Double latitude = locationSnapshot.child("latitude").getValue(Double.class);
                                Double longitude = locationSnapshot.child("longitude").getValue(Double.class);
                                String name = locationSnapshot.child("name").getValue(String.class);

                                if (latitude != null && longitude != null) {
                                    LatLng location = new LatLng(latitude, longitude);

                                    // 사용자 정의 마커 이미지로 마커를 생성합니다.
                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(location)
                                            .title(name)
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.custom_marker)); // 여기서 사용자 정의 이미지를 설정합니다.

                                    mMap.addMarker(markerOptions);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("MapsActivity", "데이터베이스 오류: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MapsActivity", "데이터베이스 오류: " + databaseError.getMessage());
                }
            });
        }
    }

    // 위치 업데이트를 시작하는 메서드
    private void startLocationUpdates() {
        LocationRequest locationRequest =
                new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20000).setMinUpdateIntervalMillis(30000).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateMapLocation(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // 현재 사용자의 위치를 지도에 업데이트하는 메서드
    private void updateMapLocation(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }

    // 사용자의 현재 위치를 표시하는 메서드
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Enabling MyLocation layer.");
            mMap.setMyLocationEnabled(true);
        } else {
            Log.d(TAG, "Location permission missing. Cannot enable MyLocation layer.");
            // 권한 요청에 대한 설명을 보여주고 권한을 요청합니다.
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        binding = null;
    }
}