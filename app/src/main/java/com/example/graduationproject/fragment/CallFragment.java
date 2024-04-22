package com.example.graduationproject.fragment;

import android.os.Bundle;

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
}