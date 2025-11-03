package com.example.dle_prototype;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class Dashboard extends AppCompatActivity {

    private TextView greetingText, textStats, textScores;
    private Button btnRefreshData;
    private static final String PREFS_NAME = "user_stats";
    private static final String KEY_CURRENT_USERNAME = "current_username";

    // Save username (call after login)
    public static void setCurrentUsername(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_USERNAME, username).apply();
    }

    // Get current username (returns null if not set)
    public static String getCurrentUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENT_USERNAME, null);
    }

    private long sessionStartTime;
    private static final String TAG = "DashboardActivity";

    // Use 5 means/stds for 5 input features if your model expects 5.
    private static final float[] MEANS = {4.2f, 8.0f, 45.7f, 2.0f, 3.8f};
    private static final float[] STDS  = {2.0f, 4.1f, 28.5f, 0.8f, 1.9f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        greetingText = findViewById(R.id.GreetingText);
        textStats = findViewById(R.id.textStats);
        textScores = findViewById(R.id.textScores); // Ensure this exists in your XML!
        btnRefreshData = findViewById(R.id.btnRefreshData);
        sessionStartTime = System.currentTimeMillis();
        findViewById(R.id.btnOpenQuiz).setOnClickListener(this::openQuiz);
        // Use string resources for better internationalization
        greetingText.setText(getString(R.string.welcome_message, getCurrentUsername(this))); // Assuming you have a string resource like <string name="welcome_message">Welcome, %1$s</string>

        // Record login
        UserStatsManager.recordLogin(this);

        btnRefreshData.setOnClickListener(v -> refreshDashboardData());
        findViewById(R.id.btnClearData).setOnClickListener(this::clearUserData);

        Button btnPersonalization = findViewById(R.id.btnPersonalization);

        btnPersonalization.setOnClickListener(v -> {
            startActivity(new Intent(Dashboard.this, PersonalizationActivity.class));
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionStartTime = System.currentTimeMillis();
        refreshDashboardData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        long sessionMillis = System.currentTimeMillis() - sessionStartTime;
        UserStatsManager.recordSessionTime(this, sessionMillis);
    }

    public void openQuiz(View view) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("username", getCurrentUsername(this));
        startActivity(intent);
    }

    private void refreshDashboardData() {
        UserStatsManager.UserStats stats = UserStatsManager.getStats(this);
        if (stats == null) {
            Toast.makeText(this, "Stats unavailable. Please try again.", Toast.LENGTH_SHORT).show();
            android.util.Log.e(TAG, "UserStatsManager.getStats() returned null.");
            return;
        }

        float login_frequency = ensureValid(stats.loginCount);
        float time_spent = ensureValid((stats.totalTimeSpentMillis + (System.currentTimeMillis() - sessionStartTime)) / 60000.0f);
        float quiz_score = ensureValid(stats.lastQuizScore);
        float difficulty_reached = ensureValid(stats.lastDifficultyReached);
        float category_selected = ensureValid(stats.lastCategorySelected); // Add this line

        String statsStringBuilder = "Login frequency: " + login_frequency + "\n" +
                "Time spent: " + String.format(Locale.getDefault(), "%.2f", time_spent) + " minutes\n" +
                "Quiz score: " + quiz_score + "\n" +
                "Difficulty reached: " + difficulty_reached + "\n" +
                "Category selected: " + category_selected;
        textStats.setText(statsStringBuilder);

        // Pass all 5 features to the model!
        float[] preds = runPersonalizationInference(
                this,
                login_frequency,
                time_spent,
                quiz_score,
                difficulty_reached,
                category_selected
        );

        String scoreMsg = "Conscientiousness: " + preds[0]
                + "\nMotivation: " + preds[1]
                + "\nUnderstanding: " + preds[2]
                + "\nEngagement: " + preds[3];
        textScores.setText(scoreMsg);

        Toast.makeText(this, "Dashboard Updated.", Toast.LENGTH_SHORT).show();

        // Example: Add this to your Dashboard
        TextView tipsText = findViewById(R.id.textTips); // Add a TextView in your XML

        StringBuilder tipsBuilder = new StringBuilder();

        if (preds[0] < 0.4) {
            tipsBuilder.append("Tip: Try to stay organised and check your progress often.\n\n");
        } else if (preds[0] < 0.7) {
            tipsBuilder.append("Tip: Keep up the good work! Stay focused and set small goals.\n\n");
        } else {
            tipsBuilder.append("Great job! Your conscientiousness is excellent.\n\n");
        }

        if (preds[1] < 0.4) {
            tipsBuilder.append("Tip: Find what motivates you. Take breaks and reward yourself.\n\n");
        } else if (preds[1] < 0.7) {
            tipsBuilder.append("Tip: You're doing well! Stay curious and keep exploring.\n\n");
        } else {
            tipsBuilder.append("Excellent motivation! Keep challenging yourself.\n\n");
        }

        if (preds[2] < 0.4) {
            tipsBuilder.append("Tip: If you find topics hard, review resources or ask for help.\n\n");
        } else if (preds[2] < 0.7) {
            tipsBuilder.append("Tip: You're building a good understanding. Keep practising.\n\n");
        } else {
            tipsBuilder.append("Your understanding is strong. Try teaching someone else!\n\n");
        }

        if (preds[3] < 0.4) {
            tipsBuilder.append("Tip: Engage with quizzes and activities to boost learning.\n\n");
        } else if (preds[3] < 0.7) {
            tipsBuilder.append("Tip: Great engagement! Keep exploring new features.\n\n");
        } else {
            tipsBuilder.append("Outstanding engagement! Keep up your enthusiasm.\n\n");
        }

        tipsText.setText(tipsBuilder.toString());

    }
    private static float ensureValid(float value) {
        return (Float.isNaN(value) || Float.isInfinite(value)) ? 0f : value;
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static float[] runPersonalizationInference(
            Context context,
            float login_frequency,
            float time_spent,
            float quiz_score,
            float difficulty_reached,
            float category_selected
    ) {
        float[] rawInput = new float[]{
                login_frequency,
                time_spent,
                quiz_score,
                difficulty_reached,
                category_selected
        };
        float[] inputScaled = new float[rawInput.length];
        for (int i = 0; i < rawInput.length; i++) {
            inputScaled[i] = (rawInput[i] - MEANS[i]) / STDS[i];
        }
        float[][] modelInput = new float[1][5];
        modelInput[0] = inputScaled;
        float[][] modelOutput = new float[1][4];

        Interpreter interpreter = null;
        try {
            interpreter = new Interpreter(loadModelFile(context, "dle_model.tflite"));
            interpreter.run(modelInput, modelOutput);
        } catch (IOException e) {
            e.printStackTrace();
            return new float[]{0f, 0f, 0f, 0f};
        } finally {
            if (interpreter != null) interpreter.close();
        }
        return modelOutput[0];
    }

    public void clearUserData(View view) {
        try {
            String currentUsername = getCurrentUsername(this);
            if (currentUsername == null || currentUsername.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("No User")
                        .setMessage("No username found. Cannot clear data.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            String[] args = { currentUsername };
            DBHelper dbHelper = DBHelper.getInstance(this);

            dbHelper.getWritableDatabase().delete("dle_data", "username = ?", args);
            dbHelper.getWritableDatabase().delete("users", "username = ?", args);

            SharedPreferences prefs = getSharedPreferences("user_stats", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("username");
            editor.remove("login_count");
            editor.remove("total_time_spent");
            editor.remove("last_quiz_score");
            editor.remove("last_difficulty_reached");
            editor.remove("last_category_selected");
            editor.apply();

            refreshDashboardData();

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Data Cleared")
                    .setMessage("Your user data has been deleted.")
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Failed to clear user data:\n" + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
            e.printStackTrace();
        }
    }
}
