package com.example.graduationproject;

public class UserLocation {
    public double latitude;
    public double longitude;
    public String address;
    public int generalRegionCode;
    public int specificRegionCode;

    public UserLocation() {
        // Firebase 데이터베이스에서 데이터를 역직렬화할 때 필요한 기본 생성자
    }

    // 필요에 따른 추가 생성자 및 메소드...
}