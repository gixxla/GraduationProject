package com.example.graduationproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationBasedAlertsManager {
    private static final String TAG = "LocationAlertsManager";
    private final Context context;
    private final DatabaseReference databaseReference;
    private static final String CHANNEL_ID = "DisasterAlertsChannel";
    private int notificationId = 100;

    public LocationBasedAlertsManager(Context context) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        createNotificationChannel();
        setupDisasterMessagesListener();
    }

    public String getChannelId() {
        return CHANNEL_ID;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Disaster Alerts";
            String description = "Notifications for disaster alerts based on location";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupDisasterMessagesListener() {
        DatabaseReference disasterMessagesRef = databaseReference.child("disasterMessages");
        disasterMessagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    DisasterMessage message = messageSnapshot.getValue(DisasterMessage.class);
                    if (message != null) {
                        checkAndNotifyDisasterMessage(message);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to listen for disaster messages", databaseError.toException());
            }
        });
    }

    // 사용자와 친구들의 위치에 기반하여 재난 메시지에 대한 알림을 보내는 메소드
    private void checkAndNotifyDisasterMessage(DisasterMessage message) {
        // 현재 로그인한 사용자의 정보를 가져온다.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && message != null && message.locationId != null) {
            // 현재 사용자의 ID를 가져온다.
            String userId = currentUser.getUid();
            // messageLocationIds 배열을 선언하고 message.locationId를 기반으로 초기화합니다.
            String[] messageLocationIds = message.locationId.split(",");

            // 사용자의 위치 정보를 Firebase에서 가져온다.
            databaseReference.child("users").child(userId).child("location").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userLocationSnapshot) {
                    // 사용자의 위치 정보를 가져온다.
                    UserLocation userLocation = userLocationSnapshot.getValue(UserLocation.class);
                    // 사용자 위치 정보와 재난 메시지의 locationId가 유효할 경우 처리한다.
                    if (userLocation != null && message.locationId != null) {
                        // locationId를 분할하여 배열로 만든다.
                        String[] messageLocationIds = message.locationId.split(",");
                        // 각 locationId를 확인하여 사용자의 지역 코드와 비교한다.
                        for (String messageLocationId : messageLocationIds) {
                            // 사용자의 지역 코드와 일치하는지 확인한다.
                            boolean isUserLocation = String.valueOf(userLocation.generalRegionCode).equals(messageLocationId.trim())
                                    || String.valueOf(userLocation.specificRegionCode).equals(messageLocationId.trim());
                            // 특정 조건을 포함하는 메시지에 대해서만 처리한다.
                            if ((message.msg.contains("실종") || message.msg.contains("강풍") || message.msg.contains("호우")|| message.msg.contains("대설")
                                    || message.msg.contains("한파")|| message.msg.contains("폭염")|| message.msg.contains("황사")|| message.msg.contains("지진")) && isUserLocation) {
                                // 사용자의 위치에서 발생한 재난에 대한 알림을 보낸다.
                                sendNotification("사용자의 위치에서 재난 경보가 발령되었습니다.", message.msg);
                                // 알림을 보냈으므로 루프를 종료한다.
                                return;
                            }
                        }
                    }
                    // 사용자 위치가 아닌 경우 친구들의 위치를 확인한다.
                    databaseReference.child("users").child(userId).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot friendsSnapshot) {
                            // 모든 친구들의 위치 정보를 확인한다.
                            for (DataSnapshot friendSnapshot : friendsSnapshot.getChildren()) {
                                // 친구의 위치 정보를 가져온다.
                                UserLocation friendLocation = friendSnapshot.child("location").getValue(UserLocation.class);
                                // 친구의 각 위치에 대해 재난 메시지의 locationId와 비교한다.
                                for (String messageLocationId : messageLocationIds) {
                                    // 친구의 지역 코드와 일치하는지 확인한다.
                                    boolean isFriendLocation = friendLocation != null &&
                                            (String.valueOf(friendLocation.generalRegionCode).equals(messageLocationId.trim()) ||
                                                    String.valueOf(friendLocation.specificRegionCode).equals(messageLocationId.trim()));
                                    // 친구의 위치에서 발생한 재난 메시지에 대한 알림을 보낼 조건을 충족하는 경우 처리한다.
                                    if (isFriendLocation && (message.msg.contains("실종") || message.msg.contains("강풍") || message.msg.contains("태풍"))) {
                                        // 친구의 이름을 가져와서 알림 제목에 포함한다.
                                        String friendName = friendSnapshot.child("name").getValue(String.class);
                                        // 친구의 위치에서 발생한 재난에 대한 알림을 보낸다.
                                        sendNotification(friendName + "의 위치에서 재난 경보가 발령되었습니다.", message.msg);
                                        // 알림을 보냈으므로 루프를 종료한다.
                                        return;
                                    }
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // 친구들의 위치 정보 가져오기가 취소되었을 때 로그를 출력한다.
                            Log.e(TAG, "Failed to fetch friends' locations", error.toException());
                        }
                    });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // 사용자 위치 정보 가져오기가 취소되었을 때 로그를 출력한다.
                    Log.e(TAG, "Failed to fetch user location", error.toException());
                }
            });
        }
    }


    // 알림을 보내는 메소드
    private void sendNotification(String title, String message) {
        // 메인 액티비티를 열기 위한 인텐트 생성
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 알림을 생성하고 설정한다.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 알림을 발송한다.
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId++, builder.build());
    }
}