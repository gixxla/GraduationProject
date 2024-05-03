package com.example.graduationproject;

public class Friend {
    String name;
    String phoneNumber;
    int resourceId;

    public Friend (int resourceId, String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
}
