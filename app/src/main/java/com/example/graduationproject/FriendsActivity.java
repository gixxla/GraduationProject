package com.example.graduationproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private static final String TAG = "FriendsActivity";

    private FloatingActionButton addFriendFab;
    private RecyclerView friendsRecyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friendList = new ArrayList<>();
    private Button setNameButton, mapButton, disasterButton, friendsButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        initializeViews();
        setupButtonListeners();
        loadFriends();
    }

    private void initializeViews() {
        addFriendFab = findViewById(R.id.addFriendFab);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        mapButton = findViewById(R.id.mapButton);
        disasterButton = findViewById(R.id.disasterButton);
        friendsButton = findViewById(R.id.friendsButton);
        setNameButton = findViewById(R.id.setNameButton);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendsAdapter(friendList);
        friendsRecyclerView.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        friendsButton.setOnClickListener(v -> Toast.makeText(FriendsActivity.this, "이미 친구 화면에 있습니다.", Toast.LENGTH_SHORT).show());
        setNameButton.setOnClickListener(v -> showSetNameDialog());
        addFriendFab.setOnClickListener(v -> showAddFriendDialog());
    }

    private void showAddFriendDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = user != null ? user.getUid() : "사용자 ID를 찾을 수 없습니다.";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("친구 추가");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 0); // 패딩 설정

        TextView myUidTextView = new TextView(this);
        myUidTextView.setText("내 UID: " + myUid);
        myUidTextView.setTextColor(getResources().getColor(R.color.purple_500)); // UID 텍스트 색상 설정
        myUidTextView.setOnClickListener(v -> {
            // 클립보드에 UID 복사
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("uid", myUid);
            clipboard.setPrimaryClip(clip);
            // 사용자에게 알림
            Toast.makeText(FriendsActivity.this, "UID가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
        });
        layout.addView(myUidTextView);

        final EditText inputField = new EditText(this);
        inputField.setInputType(InputType.TYPE_CLASS_TEXT);
        inputField.setHint("친구의 UID를 입력하세요");
        layout.addView(inputField);

        builder.setView(layout);

        builder.setPositiveButton("추가", (dialog, which) -> addFriend(inputField.getText().toString()));
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }



    private void addFriend(String friendUid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String myUid = user.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("users").child(myUid).child("friends").child(friendUid).setValue(true);
            databaseReference.child("users").child(friendUid).child("friends").child(myUid).setValue(true);
            Toast.makeText(this, "친구가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // Firebase에서 친구 목록 불러오기
    private void loadFriends() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String myUid = user.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(myUid).child("friends");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    friendList.clear(); // 기존 목록 클리어
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // 각 친구의 UID를 가져옴
                        String friendUid = snapshot.getKey();
                        DatabaseReference friendDetailRef = FirebaseDatabase.getInstance().getReference("users").child(friendUid);
                        friendDetailRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot friendSnapshot) {
                                // 친구의 이름을 가져옴
                                String name = friendSnapshot.child("name").getValue(String.class);
                                if (name != null) {
                                    // Friend 객체 생성 및 리스트에 추가
                                    friendList.add(new Friend(name, friendUid));
                                } else {
                                    // 이름이 null일 경우, UID를 이름으로 사용
                                    friendList.add(new Friend(friendUid, friendUid));
                                }
                                adapter.notifyDataSetChanged(); // 어댑터에 데이터 변경 알림
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e(TAG, "친구 상세 정보 불러오기 실패", databaseError.toException());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "친구 목록 불러오기 실패", databaseError.toException());
                }
            });
        }
    }

    private void showSetNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이름 설정");

        // 다이얼로그에 입력 필드 추가
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText nameInputField = new EditText(this);
        nameInputField.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInputField.setHint("새로운 이름을 입력하세요");
        layout.addView(nameInputField);
        builder.setView(layout);

        // 다이얼로그 버튼 설정
        builder.setPositiveButton("설정", (dialog, which) -> {
            String name = nameInputField.getText().toString();
            setUserName(name);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Firebase에 사용자 이름 설정
    private void setUserName(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !name.isEmpty()) {
            String myUid = user.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(myUid);
            databaseReference.child("name").setValue(name)
                    .addOnSuccessListener(aVoid -> Toast.makeText(FriendsActivity.this, "이름이 설정되었습니다.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(FriendsActivity.this, "이름 설정에 실패했습니다.", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
}
