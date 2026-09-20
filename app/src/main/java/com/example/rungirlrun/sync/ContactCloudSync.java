package com.example.rungirlrun.sync;

import com.example.rungirlrun.models.Contact;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactCloudSync {

    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION = "emergencyContacts";

    private final FirebaseFirestore db;

    public ContactCloudSync() {
        db = FirebaseFirestore.getInstance();
    }

    // Fire-and-forget: SQLite already has the data, so a failure here
    // (offline, etc.) never blocks the add/edit flow or SOS.
    public void pushContact(Contact contact) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", contact.getName());
        data.put("phone", contact.getPhone());
        data.put("relationship", contact.getRelationship());

        db.collection(COLLECTION_USERS)
                .document(contact.getUserId())
                .collection(SUBCOLLECTION)
                .document(contact.getPhone())
                .set(data);
    }

    public void deleteContact(String userId, String phone) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION)
                .document(phone)
                .delete();
    }

    // Used on edit, since the phone number is the doc ID: if it changed,
    // the old doc has to go and a new one takes its place.
    public void renameContactDoc(String userId, String oldPhone, Contact updatedContact) {
        if (!oldPhone.equals(updatedContact.getPhone())) {
            deleteContact(userId, oldPhone);
        }
        pushContact(updatedContact);
    }

    public interface OnContactsFetchedListener {
        void onFetched(List<Contact> cloudContacts);
        void onError(Exception e);
    }

    public void pullContacts(String userId, OnContactsFetchedListener listener) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Contact> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String relationship = doc.getString("relationship");
                        if (name != null && phone != null && relationship != null) {
                            result.add(new Contact(userId, name, phone, relationship));
                        }
                    }
                    listener.onFetched(result);
                })
                .addOnFailureListener(listener::onError);
    }
}
