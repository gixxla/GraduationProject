package com.example.graduationproject.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentCallBinding;
import com.example.graduationproject.databinding.FragmentHomeBinding;

public class CallFragment extends Fragment {
    private static final String TAG = "CallFragment";
    private FragmentCallBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 경찰 호출 버튼
        binding.buttonPolice.setOnClickListener(v -> dialNumber("112"));
        // 소방서 호출 버튼
        binding.buttonFire.setOnClickListener(v -> dialNumber("119"));
        // 긴급 의료 상담 호출 버튼
        binding.buttonMedical.setOnClickListener(v -> dialNumber("1339"));
    }

    private void dialNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}