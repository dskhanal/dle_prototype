package com.example.dle_prototype;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.Arrays;

public class TFLitePersonalizationManager {

    private static final String MODEL_NAME = "dle_model.tflite";
    private static TFLitePersonalizationManager instance;
    private Interpreter interpreter;

    private static final float[] MEANS = {4.2f, 8.0f, 45.7f, 2.0f};
    private static final float[] STDS = {2.0f, 4.1f, 28.5f, 0.8f};
    private static final String[] DOMAINS = {
            "Conscientiousness", "Motivation", "Understanding", "Engagement"
    };

    public TFLitePersonalizationManager(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context.getApplicationContext(), MODEL_NAME));
        } catch (IOException e) {
            Log.e("TFLiteManager", "Model load failed: " + e.getMessage());
            interpreter = null;
        }
    }

    public static TFLitePersonalizationManager getInstance(Context context) {
        if (instance == null) {
            instance = new TFLitePersonalizationManager(context);
        }
        return instance;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] predict(float[] rawInput) {
        if (interpreter == null) {
            Log.e("TFLiteManager", "Interpreter is null");
            return null;
        }

        if (rawInput == null || rawInput.length != 4) {
            throw new IllegalArgumentException("Input must be float[4], got: " +
                    (rawInput == null ? "null" : rawInput.length));
        }

        float[] inputScaled = new float[4];
        for (int i = 0; i < 4; i++) {
            inputScaled[i] = (ensureValid(rawInput[i]) - MEANS[i]) / STDS[i];
        }

        float[][] inputBatch = new float[1][4];
        System.arraycopy(inputScaled, 0, inputBatch[0], 0, 4);

        float[][] output = new float[1][4];

        Log.d("TFLiteDebug", "Input to model: " + Arrays.toString(inputScaled));

        interpreter.run(inputBatch, output);

        return output[0];
    }

    public String getPersonalizedExperienceReport(Context context,
                                                  float login_frequency,
                                                  float time_spent,
                                                  float quiz_score,
                                                  float difficulty_reached) {

        float[] rawInput = {
                ensureValid(login_frequency),
                ensureValid(time_spent),
                ensureValid(quiz_score),
                ensureValid(difficulty_reached)
        };

        float[] preds;
        try {
            preds = predict(rawInput);
        } catch (Exception e) {
            Log.e("TFLiteManager", "Inference error: " + e.getMessage(), e);
            return "Unable to generate personalized experience.";
        }

        if (preds == null || preds.length != 4) {
            return "Prediction failed. Output was null or incorrect size.";
        }

        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < preds.length; i++) {
            resultBuilder.append(DOMAINS[i])
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.2f", preds[i]))
                    .append("\n");
        }

        int maxIdx = 0, minIdx = 0;
        for (int i = 1; i < preds.length; i++) {
            if (preds[i] > preds[maxIdx]) maxIdx = i;
            if (preds[i] < preds[minIdx]) minIdx = i;
        }

        resultBuilder.append("\n")
                .append("🌟 Strongest: ").append(DOMAINS[maxIdx]).append("\n")
                .append("🎯 Improve: ").append(DOMAINS[minIdx]).append("\n\n");

        switch (maxIdx) {
            case 0:
                resultBuilder.append("You are highly conscientious! Try mentoring others.\n");
                break;
            case 1:
                resultBuilder.append("Strong motivation! Consider advanced tasks.\n");
                break;
            case 2:
                resultBuilder.append("Great understanding! Take on challenge quizzes.\n");
                break;
            case 3:
                resultBuilder.append("Good engagement! Join learning communities.\n");
                break;
        }

        switch (minIdx) {
            case 0:
                resultBuilder.append("Tip: Build daily habits to improve conscientiousness.\n");
                break;
            case 1:
                resultBuilder.append("Tip: Choose exciting study topics to boost motivation.\n");
                break;
            case 2:
                resultBuilder.append("Tip: Review concepts or ask for feedback.\n");
                break;
            case 3:
                resultBuilder.append("Tip: Participate more actively in discussions.\n");
                break;
        }

        return resultBuilder.toString();
    }

    private static float ensureValid(float value) {
        return (Float.isNaN(value) || Float.isInfinite(value)) ? 0f : value;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
