package com.example.dle_prototype;

import android.content.Context;
import android.content.Intent;
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
import java.util.Locale; // Added for String.format locale

public class Dashboard extends AppCompatActivity {

    private TextView greetingText, personalizedExpText, textStats;
    private Button btnRefreshData;
    private String username = "testuser"; // Consider making this dynamic based on login
    private long sessionStartTime;

    // Replace with your real scaler values for ONLY 4 inputs now!
    // These should ideally be loaded from a config or passed in, not hardcoded.
    private static final float[] MEANS = {4.2f, 8.0f, 45.7f, 2.0f};   // length 4
    private static final float[] STDS  = {2.0f, 4.1f, 28.5f, 0.8f};    // length 4

    // Tag for logging, good practice
    private static final String TAG = "DashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize UI elements
        greetingText = findViewById(R.id.GreetingText);
        personalizedExpText = findViewById(R.id.personalizedExpText);
        textStats = findViewById(R.id.textStats);
        btnRefreshData = findViewById(R.id.btnRefreshData);

        // Set initial greeting
        // Use string resources for better internationalization
        greetingText.setText(getString(R.string.welcome_message, username)); // Assuming you have a string resource like <string name="welcome_message">Welcome, %1$s</string>

        // Record login
        UserStatsManager.recordLogin(this);

        // Set up click listeners
        btnRefreshData.setOnClickListener(v -> refreshDashboardData());
        findViewById(R.id.btnClearData).setOnClickListener(this::clearUserData); // Method reference for conciseness
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
        intent.putExtra("username", username);
        startActivity(intent);
    }

    public void buttonInput(View view) {
        Intent intent = new Intent(Dashboard.this, UserInputActivity.class);
        startActivity(intent);
    }

    private void refreshDashboardData() {
        UserStatsManager.UserStats stats = UserStatsManager.getStats(this);
        if (stats == null) {
            Toast.makeText(this, "Stats unavailable. Please try again.", Toast.LENGTH_SHORT).show();
            // Log the error for debugging
            android.util.Log.e(TAG, "UserStatsManager.getStats() returned null.");
            return;
        }

        // Calculate and ensure validity in a more concise way
        float login_frequency = ensureValid(stats.loginCount);
        float time_spent = ensureValid((stats.totalTimeSpentMillis + (System.currentTimeMillis() - sessionStartTime)) / 60000.0f);
        float quiz_score = ensureValid(stats.lastQuizScore);
        float difficulty_reached = ensureValid(stats.lastDifficultyReached);

        // Use StringBuilder for efficient string concatenation
        StringBuilder statsStringBuilder = new StringBuilder();
        statsStringBuilder.append("Login frequency: ").append(login_frequency).append("\n");
        statsStringBuilder.append("Time spent: ").append(String.format(Locale.getDefault(), "%.2f", time_spent)).append(" minutes\n"); // Format time and add units
        statsStringBuilder.append("Quiz score: ").append(quiz_score).append("\n");
        statsStringBuilder.append("Difficulty reached: ").append(difficulty_reached);
        textStats.setText(statsStringBuilder.toString());

        // Personalized experience
        showPersonalizedExperiences(
                login_frequency,
                time_spent,
                quiz_score,
                difficulty_reached
        );

        Toast.makeText(this, "Dashboard Updated.", Toast.LENGTH_SHORT).show();
    }

    // Helper method to ensure a float value is valid (not NaN or infinite)
    private static float ensureValid(float value) {
        return (Float.isNaN(value) || Float.isInfinite(value)) ? 0f : value;
    }

    private void showPersonalizedExperiences(
            float login_frequency,
            float time_spent,
            float quiz_score,
            float difficulty_reached
    ) {
        float[] preds = runPersonalizationInference(
                this,
                login_frequency,
                time_spent,
                quiz_score,
                difficulty_reached
        );

        // Check if inference returned valid predictions
        if (preds == null || preds.length != 4) {
            personalizedExpText.setText("Unable to generate personalized experiences. Please try again later.");
            android.util.Log.e(TAG, "runPersonalizationInference returned invalid predictions.");
            return;
        }

        String[] domains = {"Conscientiousness", "Motivation", "Understanding", "Engagement"};
        StringBuilder experiences = new StringBuilder();

        // Append domain scores
        for (int i = 0; i < preds.length; i++) {
            experiences.append(domains[i]).append(": ").append(String.format(Locale.getDefault(), "%.2f", preds[i])).append("\n");
        }
        experiences.append("\n");

        int maxIdx = 0, minIdx = 0;
        for (int i = 1; i < preds.length; i++) {
            if (preds[i] > preds[maxIdx]) maxIdx = i;
            if (preds[i] < preds[minIdx]) minIdx = i;
        }

        experiences.append("🌟 Your strongest area: ").append(domains[maxIdx]).append("\n");
        experiences.append("🎯 Area to focus: ").append(domains[minIdx]).append("\n\n");

        // Use switch expressions (Java 14+) or more concise if-else if structure if not using Java 14+
        // For broader compatibility, keeping switch statements but they can be slightly refactored
        appendStrengthTip(experiences, maxIdx);
        experiences.append("\n");
        appendFocusTip(experiences, minIdx);

        personalizedExpText.setText(experiences.toString());
    }

    // Helper methods for appending tips to reduce code duplication in showPersonalizedExperiences
    private void appendStrengthTip(StringBuilder experiences, int maxIdx) {
        switch (maxIdx) {
            case 0: experiences.append("You are highly conscientious! Consider setting higher goals or mentoring others."); break;
            case 1: experiences.append("Your motivation is impressive! Try tackling advanced topics."); break;
            case 2: experiences.append("Strong understanding! Try a challenge quiz."); break;
            case 3: experiences.append("Great engagement! Consider joining group discussions."); break;
        }
    }

    private void appendFocusTip(StringBuilder experiences, int minIdx) {
        switch (minIdx) {
            case 0: experiences.append("Tip: Build a daily habit to improve conscientiousness."); break;
            case 1: experiences.append("Tip: Find study topics that excite you to boost motivation."); break;
            case 2: experiences.append("Tip: Review core concepts or ask for feedback to improve understanding."); break;
            case 3: experiences.append("Tip: Participate in more quizzes or discussions for engagement."); break;
        }
    }


    private static MappedByteBuffer loadModelFile(Context context, String modelFilename) throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public static float[] runPersonalizationInference(
            Context context,
            float login_frequency,
            float time_spent,
            float quiz_score,
            float difficulty_reached
    ) {
        // Prepare raw input array using the ensureValid helper
        float[] rawInput = {
                ensureValid(login_frequency),
                ensureValid(time_spent),
                ensureValid(quiz_score),
                ensureValid(difficulty_reached)
        };

        // Input validation moved here for early exit
        if (rawInput.length != 4) {
            android.util.Log.e(TAG, "Raw input array length is not 4.");
            return new float[4]; // Return an array of zeros as a fallback
        }
        for (float v : rawInput) {
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                android.util.Log.e(TAG, "Raw input contains NaN or infinite value.");
                return new float[4]; // Return an array of zeros as a fallback
            }
        }

        float[] inputScaled = new float[4];
        for (int i = 0; i < 4; i++) {
            inputScaled[i] = (rawInput[i] - MEANS[i]) / STDS[i];
        }

        // TensorFlow Lite model input and output tensors
        float[][] modelInput = {inputScaled}; // Directly use inputScaled
        float[][] modelOutput = new float[1][4];

        Interpreter interpreter = null;
        try {
            interpreter = new Interpreter(loadModelFile(context, "dle_model.tflite"));

            // More robust input/output tensor shape checks before running inference
            if (interpreter.getInputTensor(0).shape().length != 2 || interpreter.getInputTensor(0).shape()[1] != 4) {
                android.util.Log.e(TAG, "Unexpected input tensor shape for the model.");
                return new float[4];
            }
            if (interpreter.getOutputTensor(0).shape().length != 2 || interpreter.getOutputTensor(0).shape()[1] != 4) {
                android.util.Log.e(TAG, "Unexpected output tensor shape for the model.");
                return new float[4];
            }

            interpreter.run(modelInput, modelOutput);

            // Defensive: check output after inference
            if (modelOutput[0] == null || modelOutput[0].length != 4) {
                android.util.Log.e(TAG, "Model output array is null or has incorrect length.");
                return new float[4];
            }
            for (float v : modelOutput[0]) {
                if (Float.isNaN(v) || Float.isInfinite(v)) {
                    android.util.Log.e(TAG, "Model output contains NaN or infinite value.");
                    return new float[4];
                }
            }
            return modelOutput[0];

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error during TFLite inference: " + e.getMessage(), e);
            return new float[4]; // Return an array of zeros on error
        } finally {
            if (interpreter != null) {
                interpreter.close();
            }
        }
    }

    public void clearUserData(View view) { // Add 'View view' as an argument
        try {
            // Your existing clear data logic
            String[] args = { username };
            DBHelper dbHelper = DBHelper.getInstance(this);

            dbHelper.getWritableDatabase().delete("dle_data", "username = ?", args);
            dbHelper.getWritableDatabase().delete("users", "username = ?", args);

            getSharedPreferences("user_stats", MODE_PRIVATE).edit().clear().apply();

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
    private void clearUserData() {
        try {

            DBHelper dbHelper = DBHelper.getInstance(this);

            // Using arg for delete method (already good, just noting)
            String[] selectionArgs = {username};

            // Use beginTransaction, setTransactionSuccessful, and endTransaction for database operations
            // This improves performance and ensures data integrity.
            dbHelper.getWritableDatabase().beginTransaction();
            try {
                int deletedRowsDleData = dbHelper.getWritableDatabase().delete("dle_data", "username = ?", selectionArgs);
                int deletedRowsUsers = dbHelper.getWritableDatabase().delete("users", "username = ?", selectionArgs);

                getSharedPreferences("user_stats", MODE_PRIVATE).edit().clear().apply();

                dbHelper.getWritableDatabase().setTransactionSuccessful();

                // Log deleted rows for debugging
                android.util.Log.i(TAG, "Cleared dle_data rows: " + deletedRowsDleData);
                android.util.Log.i(TAG, "Cleared users rows: " + deletedRowsUsers);
            } finally {
                dbHelper.getWritableDatabase().endTransaction();
            }


            refreshDashboardData();

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Data Cleared")
                    .setMessage("Your user data has been deleted.")
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            // Log the exception for debugging
            android.util.Log.e(TAG, "Failed to clear user data: " + e.getMessage(), e);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Failed to clear user data:\n" + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}