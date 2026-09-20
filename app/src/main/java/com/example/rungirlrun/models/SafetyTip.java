package com.example.rungirlrun.models;
import com.google.firebase.Timestamp;

public class SafetyTip {
    private String id;
    private String tipText;
    private String authorId;
    private String authorName;
    private boolean isOfficial;
    private Timestamp timestamp;
    private String category;
    private String time;
    public SafetyTip() {} // required empty constructor for Firestore

    public SafetyTip(String tipText, String authorId, String authorName, boolean isOfficial) {
        this.tipText = tipText;
        this.authorId = authorId;
        this.authorName = authorName;
        this.isOfficial = isOfficial;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTipText() { return tipText; }
    public void setTipText(String tipText) { this.tipText = tipText; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getCategory() {
        return category;
    }
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    public void setCategory(String category) {
        this.category = category;
    }
}
