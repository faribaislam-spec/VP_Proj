package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rungirlrun.sync.ContactCloudSync;
import com.example.rungirlrun.R;
import com.example.rungirlrun.dao.ContactDAO;
import com.example.rungirlrun.models.Contact;

public class AddContactActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etPhone;
    private EditText etRelationship;

    private Button btnSave;
    private Button btnBack;

    private ContactDAO contactDAO;

    private boolean editMode = false;

    private int contactId;
    private String userId;
    private ContactCloudSync cloudSync;
    private String originalPhone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact2);


        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etRelationship = findViewById(R.id.etRelationship);

        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);


        contactDAO = new ContactDAO(this);
        cloudSync = new ContactCloudSync();

        // Check if this screen is opened for editing
        editMode = getIntent().getBooleanExtra("EDIT_MODE", false);


        if (editMode) {

            contactId = getIntent().getIntExtra("CONTACT_ID", -1);
            originalPhone = getIntent().getStringExtra("PHONE");
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                finish();
                return;
            }

            userId = user.getUid();

            if (userId == null) {
                finish();
                return;
            }

            etName.setText(getIntent().getStringExtra("NAME"));
            etPhone.setText(getIntent().getStringExtra("PHONE"));
            etRelationship.setText(getIntent().getStringExtra("RELATIONSHIP"));


            btnSave.setText("Update Contact");

        } else {

            // Normal add mode

            userId = getIntent().getStringExtra("userId");
            if (userId == null) {
                finish();
                return;
            }

        }


        btnSave.setOnClickListener(v -> {


            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String relationship = etRelationship.getText().toString().trim();


            if (name.isEmpty() || phone.isEmpty() || relationship.isEmpty()) {

                Toast.makeText(this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();

                return;
            }
            // Name validation
            if (!name.matches("[a-zA-Z ]{2,50}")) {

                Toast.makeText(this,
                        "Enter a valid name",
                        Toast.LENGTH_SHORT).show();

                return;
            }
            // Phone validation
            if (!phone.matches("01[3-9]\\d{8}")) {
                Toast.makeText(this,
                        "Enter a valid phone number",
                        Toast.LENGTH_SHORT).show();

                return;
            }
            // Relationship validation
            if (!relationship.matches("[a-zA-Z ]{2,30}")) {
                Toast.makeText(this,
                        "Enter a valid relationship",
                        Toast.LENGTH_SHORT).show();

                return;
            }
            Contact contact;
            if (editMode) {

                contact = new Contact(
                        contactId,
                        userId,
                        name,
                        phone,
                        relationship
                );


                boolean updated = contactDAO.updateContact(contact);


                if (updated) {


                        cloudSync.renameContactDoc(userId, originalPhone, contact);
                        Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show();
                        finish();


                }
                else {
                    Toast.makeText(this,
                            "Update failed",
                            Toast.LENGTH_SHORT).show();
                }


            } else {


                contact = new Contact(
                        userId,
                        name,
                        phone,
                        relationship
                );


                if (contactDAO.contactExists(userId, phone)) {

                    Toast.makeText(this,
                            "Contact already exists!",
                            Toast.LENGTH_SHORT).show();

                } else {


                    if (contactDAO.getContactCount(userId) >= 5) {

                        Toast.makeText(this,
                                "Maximum 5 emergency contacts allowed!",
                                Toast.LENGTH_SHORT).show();

                    } else {

                        boolean success = contactDAO.addContact(contact);

                        if (success) {

                            cloudSync.pushContact(contact);
                            Toast.makeText(this, "Contact saved successfully!", Toast.LENGTH_SHORT).show();
                            etName.setText("");
                            etPhone.setText("");
                            etRelationship.setText("");

                        } else {

                            Toast.makeText(this,
                                    "Failed to save contact. Please try again.",
                                    Toast.LENGTH_SHORT).show();

                        }

                    }
                }
            }

        });



        btnBack.setOnClickListener(v -> {
            finish();
        });

    }
}