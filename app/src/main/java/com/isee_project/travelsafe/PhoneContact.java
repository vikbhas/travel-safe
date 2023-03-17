package com.isee_project.travelsafe;

class PhoneContact {
    private String name;
    private String phoneNumber;
    private String photo;

    public PhoneContact() {}

    public PhoneContact(String name, String phoneNumber, String photo) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
