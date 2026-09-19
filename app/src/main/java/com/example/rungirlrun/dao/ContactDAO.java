package com.example.rungirlrun.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.example.rungirlrun.database.DatabaseHelper;
import com.example.rungirlrun.models.Contact;
import java.util.ArrayList;
public class ContactDAO {

    private DatabaseHelper dbHelper;

    public ContactDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }
    public boolean addContact(Contact contact) {

        if (getContactCount(contact.getUserId()) >= 5) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        System.out.println("Saving contact for user: " + contact.getUserId());
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, contact.getUserId());
        values.put(DatabaseHelper.COLUMN_NAME, contact.getName());
        values.put(DatabaseHelper.COLUMN_PHONE, contact.getPhone());
        values.put(DatabaseHelper.COLUMN_RELATIONSHIP, contact.getRelationship());

        long result = db.insert(DatabaseHelper.TABLE_CONTACTS, null, values);

        System.out.println("INSERT RESULT: " + result);

        db.close();

        return result != -1;
    }
    public boolean contactExists(String userId,String phone) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CONTACTS,
                new String[]{DatabaseHelper.COLUMN_ID},
                DatabaseHelper.COLUMN_USER_ID + "=? AND " +
                        DatabaseHelper.COLUMN_PHONE + "=?",
                new String[]{userId, phone},
                null,
                null,
                null
        );

        boolean exists = cursor.moveToFirst();

        cursor.close();
        db.close();

        return exists;
    }
    public ArrayList<Contact> getContactsByUser(String userId) {

        ArrayList<Contact> contacts = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        System.out.println("Searching contacts for user: " + userId);
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CONTACTS,
                null,
                DatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{userId},
                null,
                null,
                DatabaseHelper.COLUMN_NAME + " ASC"
        );

        if (cursor.moveToFirst()) {
            do {

                Contact contact = new Contact(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RELATIONSHIP))
                );

                contacts.add(contact);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        System.out.println("CONTACT COUNT FROM DB: " + contacts.size());
        return contacts;
    }
    public boolean deleteContact(int contactId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int result = db.delete(
                DatabaseHelper.TABLE_CONTACTS,
                DatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(contactId)}
        );

        db.close();

        return result > 0;
    }
    public boolean updateContact(Contact contact) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.COLUMN_NAME, contact.getName());
        values.put(DatabaseHelper.COLUMN_PHONE, contact.getPhone());
        values.put(DatabaseHelper.COLUMN_RELATIONSHIP, contact.getRelationship());

        int result = db.update(
                DatabaseHelper.TABLE_CONTACTS,
                values,
                DatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(contact.getId())}
        );

        db.close();

        return result > 0;
    }
    public int getContactCount(String userId) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " +
                        DatabaseHelper.TABLE_CONTACTS +
                        " WHERE " +
                        DatabaseHelper.COLUMN_USER_ID +
                        "=?",
                new String[]{userId}
        );

        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }

}
