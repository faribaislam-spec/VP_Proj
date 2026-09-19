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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        FloatingActionButton btnAddContact = findViewById(R.id.btnAddContact);
        lvContacts = findViewById(R.id.lvContacts);
        btnBackDashboard = findViewById(R.id.btnBackDashboard);
        contactDAO = new ContactDAO(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    //for decoding (kept for later use)
        if (user != null) {
            Toast.makeText(this, "UID: " + user.getUid(), Toast.LENGTH_LONG).show();
        } else {
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

        loadContacts();
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


    @Override
    protected void onResume() {
        super.onResume();

        loadContacts();
    }

}