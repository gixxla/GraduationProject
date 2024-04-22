package com.example.graduationproject.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.graduationproject.MainActivity;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentFriendsBinding;
import com.example.graduationproject.databinding.FragmentMypageBinding;

public class MypageFragment extends Fragment {
    private static final String TAG = "MypageFragment";
    private FragmentMypageBinding binding;
    private FragmentManager fragmentManager;
    private MainActivity mainActivity;
    private FriendsFragment friendsFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = new MainActivity();
        friendsFragment = new FriendsFragment();
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
        fragmentManager = getParentFragmentManager();
        binding.btnFriend.setOnClickListener(v -> fragmentManager.beginTransaction().replace(R.id.pageView, friendsFragment).addToBackStack(null).commit());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }
}