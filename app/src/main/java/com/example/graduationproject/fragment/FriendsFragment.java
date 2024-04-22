package com.example.graduationproject.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.graduationproject.MainActivity;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentFriendsBinding;
import com.example.graduationproject.databinding.FragmentMapsBinding;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FriendsFragment extends Fragment {
    private static final String TAG = "FriendsFragment";
    private FragmentFriendsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MainActivity mainActivity;
    private String userUid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
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
        loadFriend();
        binding.btnFriendAdd.setOnClickListener(v -> {
            String phoneNumber = "+82" + binding.pnumberInput.getText().toString().substring(1).replace("-", "");
            addFriend(phoneNumber);
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    private void loginAlert() {

    }

    private void addFriendAlert() {

    }

    private void loadFriend() {

    }

    private void addFriend(String phoneNumber) {
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
                        // setFriendName();
                        Map<String, Object> friend = new HashMap<>();
                        friend.put("uid", friendUid);
                        friend.put("phoneNumber", phoneNumber);
                        db.collection("users").document(userUid).collection("friends").document(friendUid).set(friend);
                        Toast.makeText(mainActivity, "친구 추가가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    } catch (IndexOutOfBoundsException e) {
                        Toast.makeText(mainActivity, "해당 유저가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mainActivity, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Login 화면
        }
    }

    private void setFriendName() {
        // alert -> 이름 입력 -> 전달
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}