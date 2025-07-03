package com.example.dle_prototype;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Registration extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);

        dbHelper = DBHelper.getInstance(this);
    }

    public void registerUser(View view) {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);

        long result = db.insert("users", null, values);
        if (result > 0) {
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
        }
    }
}
