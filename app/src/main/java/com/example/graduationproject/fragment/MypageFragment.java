package com.example.graduationproject.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.graduationproject.LoginActivity;
import com.example.graduationproject.MainActivity;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentMypageBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MypageFragment extends Fragment {
    private static final String TAG = "MypageFragment";
    private FragmentMypageBinding binding;
    private FragmentManager fragmentManager;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MainActivity mainActivity;
    private FriendsFragment friendsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        friendsFragment = new FriendsFragment();
        fragmentManager = getParentFragmentManager();
        setNickname();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 뷰바인딩 초기화
        binding = FragmentMypageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.friendManagement.setOnClickListener(v ->
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.from_right_enter, R.anim.to_left_exit, R.anim.none, R.anim.to_right_exit)
                    .replace(R.id.pageView, friendsFragment).addToBackStack(TAG).commit()
        );
        binding.logout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            mainActivity.overridePendingTransition(R.anim.from_down_enter, R.anim.none);
            startActivity(intent);
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    private void setNickname() {
        user = mAuth.getCurrentUser();
        if (user != null) {
            String userUid = user.getUid();
            db.collection("users").document(userUid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        String nickname = document.getData().get("nickname").toString();
                        binding.nickname.setText(nickname);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                }
            });
        }
    }
}