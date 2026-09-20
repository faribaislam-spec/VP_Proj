package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.rungirlrun.R;
import com.example.rungirlrun.adapters.ContactAdapter;
import com.example.rungirlrun.dao.ContactDAO;
import com.example.rungirlrun.models.Contact;
import android.widget.TextView;
import java.util.ArrayList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.rungirlrun.sync.ContactCloudSync;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private Button btnAddContact;
    private ListView lvContacts;
    private Button btnBackDashboard;
    private ContactDAO contactDAO;
    private ContactAdapter adapter;
    private ArrayList<Contact> contactList;
    private TextView tvEmpty;
    // Temporary user ID
    private String userId;
    private ContactCloudSync cloudSync;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        FloatingActionButton btnAddContact = findViewById(R.id.btnAddContact);
        lvContacts = findViewById(R.id.lvContacts);
        btnBackDashboard = findViewById(R.id.btnBackDashboard);
        contactDAO = new ContactDAO(this);
        cloudSync = new ContactCloudSync();
        tvEmpty = findViewById(R.id.tvEmpty);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    //for decoding (kept for later use)
        if (user != null) {
            Toast.makeText(this, "UID: " + user.getUid(), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "No Firebase user", Toast.LENGTH_LONG).show();
        }
        if (user == null) {
            finish();
            return;
        }

        userId = user.getUid();
        if (userId == null) {
            finish();
            return;
        }

        loadContacts();//from local database
        syncFromCloudThenLoad();//from firebase
        //when add button pressed
        btnAddContact.setOnClickListener(v -> {

            Intent intent = new Intent(ContactsActivity.this, AddContactActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);

        });
        btnBackDashboard.setOnClickListener(v -> {
            finish();
        });
    }
    //load saved contacts
    private void loadContacts() {

        contactList = contactDAO.getContactsByUser(userId);


        if (contactList.isEmpty()) {

            tvEmpty.setVisibility(TextView.VISIBLE);
            lvContacts.setVisibility(ListView.GONE);

        } else {

            tvEmpty.setVisibility(TextView.GONE);
            lvContacts.setVisibility(ListView.VISIBLE);


            adapter = new ContactAdapter(this, contactList);
            lvContacts.setAdapter(adapter);
        }
    }
    private void syncFromCloudThenLoad() {
        cloudSync.pullContacts(userId, new ContactCloudSync.OnContactsFetchedListener() {
            @Override
            public void onFetched(List<Contact> cloudContacts) {
                boolean addedAny = false;
                for (Contact c : cloudContacts) {
                    if (!contactDAO.contactExists(userId, c.getPhone())
                            && contactDAO.getContactCount(userId) < 5) {
                        contactDAO.addContact(c);
                        addedAny = true;
                    }
                }
                if (addedAny) {
                    loadContacts();
                }
            }

            @Override
            public void onError(Exception e) {
                // Offline or nothing in the cloud — local list from onCreate already showing, nothing to do.
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        loadContacts();
    }

}