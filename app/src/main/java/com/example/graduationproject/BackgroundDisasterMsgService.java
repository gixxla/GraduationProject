package com.example.graduationproject;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackgroundDisasterMsgService extends Service {
    private static final String TAG = "BackgroundDisasterMsgService";
    @Override
    public void onCreate() {
        super.onCreate();
        startDisasterMessageFetching();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
