package com.example.rungirlrun.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 3;

    // Table name
    public static final String TABLE_CONTACTS = "contacts";

    // Column names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_RELATIONSHIP = "relationship";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_CONTACTS_TABLE =
                "CREATE TABLE " + TABLE_CONTACTS + "("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_USER_ID + " TEXT,"
                        + COLUMN_NAME + " TEXT,"
                        + COLUMN_PHONE + " TEXT,"
                        + COLUMN_RELATIONSHIP + " TEXT"
                        + ")";

        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Keep existing data
        // Add future ALTER TABLE changes here if needed

    }
}
