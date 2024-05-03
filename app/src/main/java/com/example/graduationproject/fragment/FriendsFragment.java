package com.example.graduationproject.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.example.graduationproject.Friend;
import com.example.graduationproject.FriendAdapter;
import com.example.graduationproject.MainActivity;
import com.example.graduationproject.OnItemClick;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentFriendsBinding;
import com.example.graduationproject.databinding.FragmentMapsBinding;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FriendsFragment extends Fragment implements OnItemClick {
    private static final String TAG = "FriendsFragment";
    private FragmentFriendsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MainActivity mainActivity;
    private MypageFragment mypageFragment;
    private FragmentManager fragmentManager;
    private FriendAdapter friendAdapter1;
    private FriendAdapter friendAdapter2;
    private String userUid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        mypageFragment = new MypageFragment();
        fragmentManager = getParentFragmentManager();
        friendAdapter1 = new FriendAdapter(mainActivity,this);
        friendAdapter2 = new FriendAdapter(mainActivity,this);

        if (user == null) {
            loginAlert();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        binding.friendList2.setAdapter(friendAdapter2);
        binding.friendList2.setLayoutManager(new LinearLayoutManager(mainActivity));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.friendList1.setVisibility(View.VISIBLE);
                    binding.friendList2.setVisibility(View.GONE);
                } else {
                    binding.friendList1.setVisibility(View.GONE);
                    binding.friendList2.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        binding.icBack.setOnClickListener(v ->
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.from_left_enter, R.anim.to_right_exit)
                    .replace(R.id.pageView, mypageFragment).addToBackStack(null).commit()
        );
        binding.icAdd.setOnClickListener(v -> addFriendAlert());

        loadFriendAddedByUser();
        loadFriendWhoAddedUser();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    private void loginAlert() {
        // 미구현
    }

    private void loadFriendAddedByUser() {
        user = mAuth.getCurrentUser();
        if (user != null) {
            String userUid = user.getUid();
            ArrayList<Friend> friendsArrayList = new ArrayList<>();

            db.collection("users").document(userUid).collection("friends").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map friendData = document.getData();
                        String nickname = friendData.get("nickname").toString();
                        String phoneNumber = "0" + friendData.get("phoneNumber").toString().substring(3);
                        phoneNumber = phoneNumber.substring(0, 3) + "-" + phoneNumber.substring(3, 7) + "-" + phoneNumber.substring(7);
                        friendsArrayList.add(new Friend(R.drawable.ic_refresh, nickname, phoneNumber));
                    }

                    binding.friendList1.setAdapter(friendAdapter1);
                    binding.friendList1.setLayoutManager(new LinearLayoutManager(mainActivity));
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            });

            friendAdapter1.setFriendList(friendsArrayList);
        }
    }

    private void loadFriendWhoAddedUser() {
        // 미구현
        ArrayList<Friend> friendArrayList = new ArrayList<>();

        for (int i = 1; i < 10; i++) {
            friendArrayList.add(new Friend(R.drawable.ic_refresh, i + "번째 유저", "010-" + i + "000-0000"));
        }

        friendAdapter2.setFriendList(friendArrayList);
    }

    private void addFriendAlert() {
        EditText phoneNumberInput = new EditText(mainActivity);
        phoneNumberInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneNumberInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(13) });
        phoneNumberInput.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        AlertDialog.Builder friendAddDialog = new AlertDialog.Builder(mainActivity);
        friendAddDialog.setTitle("친구를 추가할까요?")
                .setMessage("추가하고 싶은 친구의 휴대폰 번호를 입력해주세요.")
                .setView(phoneNumberInput)
                .setPositiveButton("계속하기", (dialog, which) -> {
                    String phoneNumber = "+82" + phoneNumberInput.getText().toString().substring(1).replace("-", "");
                    setFriendName(phoneNumber);
                })
                .setNegativeButton("그만두기", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setFriendName(String phoneNumber) {
        EditText nicknameInput = new EditText(mainActivity);
        nicknameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nicknameInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
        AlertDialog.Builder friendAddDialog = new AlertDialog.Builder(mainActivity);
        friendAddDialog.setTitle("친구의 별명은 무엇인가요?")
                .setMessage("1~6글자 사이로 입력해주세요.")
                .setView(nicknameInput)
                .setPositiveButton("저장하기", (dialog, which) -> addFriend(phoneNumber, nicknameInput.getText().toString()))
                .setNegativeButton("그만두기", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addFriend(String phoneNumber, String nickname) {
        user = mAuth.getCurrentUser();
        if (user != null) {
            Query queryPhoneNumber = db.collection("users").whereEqualTo("phoneNumber", phoneNumber);
            queryPhoneNumber.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    try {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        userUid = user.getUid();
                        String friendUid = document.get("uid").toString();
                        Map<String, Object> friend = new HashMap<>();
                        friend.put("uid", friendUid);
                        friend.put("nickname", nickname);
                        friend.put("phoneNumber", phoneNumber);
                        db.collection("users").document(userUid).collection("friends").document(friendUid).set(friend);
                        Toast.makeText(mainActivity, "친구 추가가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                        loadFriendAddedByUser();
                    } catch (IndexOutOfBoundsException e) {
                        Toast.makeText(mainActivity, "해당 유저가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mainActivity, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            loginAlert();
        }
    }

    public void deleteFriend(String phoneNumber) {
        user = mAuth.getCurrentUser();
        if (user != null) {
            phoneNumber = "+82" + phoneNumber.substring(1).replace("-", "");
            Query queryPhoneNumber = db.collection("users").whereEqualTo("phoneNumber", phoneNumber);
            queryPhoneNumber.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    try {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        userUid = user.getUid();
                        String friendUid = document.get("uid").toString();
                        db.collection("users").document(userUid)
                                .collection("friends").document(friendUid)
                                .delete()
                                .addOnSuccessListener(unused -> Toast.makeText(mainActivity, "친구 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show());
                        loadFriendAddedByUser();
                    } catch (IndexOutOfBoundsException e) {
                        Toast.makeText(mainActivity, "해당 유저가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mainActivity, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            loginAlert();
        }
    }

    @Override
    public void onClick(String value) {
        deleteFriend(value);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}