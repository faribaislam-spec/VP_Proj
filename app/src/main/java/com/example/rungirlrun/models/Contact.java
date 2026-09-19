package com.example.rungirlrun.models;

public class Contact {

    private int id;
    private String userId;
    private String name;
    private String phone;
    private String relationship;

    // Empty constructor
    public Contact() {

    }
    // Constructor without id (for adding a new contact)
    public Contact(String userId, String name, String phone, String relationship) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }
    // Constructor with id (for retrieving from database)
    public Contact(int id, String userId, String name, String phone, String relationship) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }
    // Getters
    public int getId() {
        return id;
    }
    public String getUserId() {
        return userId;
    }
    public String getName() {
        return name;
    }
    public String getPhone() {
        return phone;
    }
    public String getRelationship() {
        return relationship;
    }
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}