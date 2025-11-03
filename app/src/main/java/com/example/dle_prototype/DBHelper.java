package com.example.dle_prototype;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "app.db";
    private static final int DB_VERSION = 1;
    private static DBHelper instance;

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT);");
        db.execSQL("CREATE TABLE dle_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT, " +
                "quiz_score REAL, " +
                "time_spent REAL, " +
                "login_frequency REAL, " +
                "difficulty_reached REAL, " +
                "category_selected REAL" +
                ");");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE dle_data ADD COLUMN username TEXT;");
        }
        db.execSQL("DROP TABLE IF EXISTS users;");
        db.execSQL("DROP TABLE IF EXISTS dle_data;");
    }
    public List<String> listTables() {
        List<String> names = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                        "AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
                null);
        try {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return names;
    }
}
