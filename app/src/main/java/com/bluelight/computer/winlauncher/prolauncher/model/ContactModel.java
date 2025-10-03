package com.bluelight.computer.winlauncher.prolauncher.model;

public class ContactModel {
    private final String name;
    private final String number;
    private final String photoUri;

    public ContactModel(String name, String number, String photoUri) {
        this.name = name;
        this.number = number;
        this.photoUri = photoUri;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public String getPhotoUri() {
        return photoUri;
    }
}
