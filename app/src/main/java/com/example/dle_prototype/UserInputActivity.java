package com.example.dle_prototype;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class UserInputActivity extends AppCompatActivity {

    private static final float[] MEANS = {4.2f, 8.0f, 45.7f, 2.0f, 3.8f};
    private static final float[] STDS  = {2.0f, 4.1f, 28.5f, 0.8f, 1.9f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_input);

        TextView textStats = findViewById(R.id.textStats);
        TextView textPersonalization = findViewById(R.id.PersonalizationText);

        // Get user stats
        UserStatsManager.UserStats stats = UserStatsManager.getStats(this);
        float login_frequency = stats.loginCount;
        float time_spent = stats.totalTimeSpentMillis / 60000.0f;
        float quiz_score = stats.lastQuizScore;
        float difficulty_reached = stats.lastDifficultyReached;
        float category_selected = stats.lastCategorySelected;

        // Show user stats
        String statsString = "Login frequency: " + login_frequency
                + "\nTime spent: " + time_spent
                + "\nQuiz score: " + quiz_score
                + "\nDifficulty reached: " + difficulty_reached
                + "\nCategory selected: " + category_selected;
        textStats.setText(statsString);

        // Get personalization scores on load
        float[] preds = runPersonalizationInference(
                this,
                login_frequency,
                time_spent,
                quiz_score,
                difficulty_reached,
                category_selected
        );

        String msg = "Conscientiousness: " + preds[0]
                + "\nMotivation: " + preds[1]
                + "\nUnderstanding: " + preds[2]
                + "\nEngagement: " + preds[3];
        textPersonalization.setText(msg);
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
}
