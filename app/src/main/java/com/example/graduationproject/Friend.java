package com.example.graduationproject;

public class Friend {
    private String name; // 친구의 이름
    private String email; // 친구의 이메일
    // 필요에 따라 추가 정보(예: 프로필 사진 URL)를 여기에 정의할 수 있습니다.

    // 기본 생성자 - Firebase Realtime Database 등에서 사용할 수 있도록 포함
    public Friend() {
        // 기본 생성자는 필요에 따라 비워둘 수 있습니다.
    }

    // 모든 필드를 초기화하는 생성자
    public Friend(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // 이름에 대한 getter 메서드
    public String getName() {
        return name;
    }

    // 이름에 대한 setter 메서드
    public void setName(String name) {
        this.name = name;
    }

    // 이메일에 대한 getter 메서드
    public String getEmail() {
        return email;
    }

    // 이메일에 대한 setter 메서드
    public void setEmail(String email) {
        this.email = email;
    }

}
