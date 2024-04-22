package com.example.graduationproject.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.graduationproject.MainActivity;
import com.example.graduationproject.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private MainActivity mainActivity;
    private String location;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 뷰바인딩 초기화
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.refreshLocation.setOnClickListener(v -> {
            binding.location.setText(location);
            Log.d(TAG, "Location refreshed.");
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 브로드캐스트된 위치 받기
        LocalBroadcastManager.getInstance(mainActivity).registerReceiver(mMessageReceiver, new IntentFilter("userLocation"));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(mainActivity).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
    }

    // 위치 브로드캐스트 리시버
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            location = intent.getStringExtra("location");
            binding.location.setText(location);
            Log.d("receiver", "Got location: " + location);
        }
    };
}