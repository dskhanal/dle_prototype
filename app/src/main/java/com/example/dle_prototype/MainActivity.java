package com.example.dle_prototype;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private TextView greetingText, registerText;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usernameInput = findViewById(R.id.inputUsername);
        passwordInput = findViewById(R.id.inputPassword);
        dbHelper = DBHelper.getInstance(this);

        ModelValidator.validateModel(MainActivity.this);

        greetingText = findViewById(R.id.greetingText);
        greetingText.setText("Login");
        registerText = findViewById(R.id.registerText);
        registerText.setText("New User ?");
        List<String> tables = dbHelper.listTables();

        TextView textView = findViewById(R.id.tables_view);
        textView.setText(TextUtils.join("\n", tables));
    }

    private void createTestUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=?", new String[]{username});
        if (!cursor.moveToFirst()) {
            db.execSQL("INSERT INTO users (username, password) VALUES (?, ?)", new String[]{username, password});
        }
        cursor.close();
    }

    public void login(View view) {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();


        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?", new String[]{username, password});

        if (cursor.moveToFirst()) {
            cursor.close();
            Intent intent = new Intent(this, Dashboard.class);
            intent.putExtra("username", username);
            UserStatsManager.setCurrentUsername(this, username); // Save to SharedPreferences

            startActivity(intent);

        } else {
            cursor.close();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }


    public void register(View view) {
        startActivity(new Intent(this, Registration.class));
    }
}
