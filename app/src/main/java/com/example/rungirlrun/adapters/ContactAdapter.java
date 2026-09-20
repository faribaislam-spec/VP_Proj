package com.example.rungirlrun.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.rungirlrun.sync.ContactCloudSync;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rungirlrun.R;
import com.example.rungirlrun.activities.AddContactActivity;
import com.example.rungirlrun.dao.ContactDAO;
import com.example.rungirlrun.models.Contact;

import java.util.ArrayList;

public class ContactAdapter extends ArrayAdapter<Contact> {

    private Activity activity;
    private ArrayList<Contact> contacts;
    private ContactDAO contactDAO;
    private ContactCloudSync cloudSync;
    public ContactAdapter(Activity activity, ArrayList<Contact> contacts) {
        super(activity, R.layout.contact_item, contacts);
        this.activity = activity;
        this.contacts = contacts;
        this.contactDAO = new ContactDAO(activity);
        this.cloudSync = new ContactCloudSync();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.contact_item, parent, false);
        }

        Contact contact = contacts.get(position);

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvPhone = convertView.findViewById(R.id.tvPhone);
        TextView tvRelationship = convertView.findViewById(R.id.tvRelationship);

        Button btnEdit = convertView.findViewById(R.id.btnEdit);
        Button btnDelete = convertView.findViewById(R.id.btnDelete);

        tvName.setText(contact.getName());
        tvPhone.setText(contact.getPhone());
        tvRelationship.setText(contact.getRelationship());


        // Edit button
        btnEdit.setOnClickListener(v -> {

            Intent intent = new Intent(activity, AddContactActivity.class);

            intent.putExtra("EDIT_MODE", true);

            intent.putExtra("CONTACT_ID", contact.getId());
            intent.putExtra("userId", contact.getUserId());
            intent.putExtra("NAME", contact.getName());
            intent.putExtra("PHONE", contact.getPhone());
            intent.putExtra("RELATIONSHIP", contact.getRelationship());

            activity.startActivity(intent);

        });


        // Delete button
        btnDelete.setOnClickListener(v -> {

            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to remove this contact?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        boolean deleted = contactDAO.deleteContact(contact.getId());

                        if (deleted) {
                            cloudSync.deleteContact(contact.getUserId(), contact.getPhone());
                            Toast.makeText(activity,
                                    "Contact deleted",
                                    Toast.LENGTH_SHORT).show();

                            contacts.remove(position);
                            notifyDataSetChanged();

                        } else {

                            Toast.makeText(activity,
                                    "Failed to delete contact",
                                    Toast.LENGTH_SHORT).show();
                        }

                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {

                        dialog.dismiss();

                    })
                    .show();

        });


        return convertView;
    }
}