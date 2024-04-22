package com.example.graduationproject.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.graduationproject.R;
import com.example.graduationproject.databinding.FragmentManualBinding;

import java.io.IOException;
import java.io.InputStream;

public class ManualFragment extends Fragment {
    private static final String TAG = "ManualFragment";
    private FragmentManualBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManualBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.typhoonButton.setOnClickListener(v -> {
            binding.typhoonScroll.setVisibility(View.VISIBLE);
            binding.earthquakeScroll.setVisibility(View.INVISIBLE);
        });
        binding.earthquakeButton.setOnClickListener(v -> {
            binding.earthquakeScroll.setVisibility(View.VISIBLE);
            binding.typhoonScroll.setVisibility(View.INVISIBLE);
        });

        binding.tButton1.setOnClickListener(v -> {
            try {
                InputStream in = getResources().openRawResource(R.raw.typhoon_usual);
                byte[] b = new byte[in.available()];
                in.read(b);
                binding.typhoonManual1.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.tExpansionLayout1.getVisibility() == View.VISIBLE) {
                binding.tExpansionLayout1.setVisibility(View.GONE);
            }
            else {
                binding.tExpansionLayout1.setVisibility(View.VISIBLE);
            }
        });
        binding.tButton2.setOnClickListener(v -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.typhoon_when_occured);
                byte[] b = new byte[is.available()];
                is.read(b);
                binding.typhoonManual2.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.tExpansionLayout2.getVisibility() == View.VISIBLE) {
                binding.tExpansionLayout2.setVisibility(View.GONE);
            }
            else {
                binding.tExpansionLayout2.setVisibility(View.VISIBLE);
            }
        });
        binding.tButton3.setOnClickListener(v -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.typhoon_after_occured);
                byte[] b = new byte[is.available()];
                is.read(b);
                binding.typhoonManual3.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.tExpansionLayout3.getVisibility() == View.VISIBLE) {
                binding.tExpansionLayout3.setVisibility(View.GONE);
            }
            else {
                binding.tExpansionLayout3.setVisibility(View.VISIBLE);
            }
        });

        binding.eButton1.setOnClickListener(v -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.earthquake_forcasted);
                byte[] b = new byte[is.available()];
                is.read(b);
                binding.earthquakeManual1.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.eExpansionLayout1.getVisibility() == View.VISIBLE) {
                binding.eExpansionLayout1.setVisibility(View.GONE);
            }
            else {
                binding.eExpansionLayout1.setVisibility(View.VISIBLE);
            }
        });
        binding.eButton2.setOnClickListener(v -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.earthquake_when_occured);
                byte[] b = new byte[is.available()];
                is.read(b);
                binding.earthquakeManual2.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.eExpansionLayout2.getVisibility() == View.VISIBLE) {
                binding.eExpansionLayout2.setVisibility(View.GONE);
            }
            else {
                binding.eExpansionLayout2.setVisibility(View.VISIBLE);
            }
        });
        binding.eButton3.setOnClickListener(v -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.earthquake_after_occured);
                byte[] b = new byte[is.available()];
                is.read(b);
                binding.earthquakeManual3.setText(new String(b, "utf-8"));
            } catch (IOException e){
                throw new RuntimeException(e);
            }

            if (binding.eExpansionLayout3.getVisibility() == View.VISIBLE) {
                binding.eExpansionLayout3.setVisibility(View.GONE);
            }
            else {
                binding.eExpansionLayout3.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}