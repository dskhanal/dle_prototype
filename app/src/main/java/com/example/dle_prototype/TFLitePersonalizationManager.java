package com.example.dle_prototype;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLitePersonalizationManager {

    private static final String MODEL_NAME = "dle_model.tflite";
    private static TFLitePersonalizationManager instance;
    private Interpreter interpreter;

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

    public float[] runInference(float[] input) {
        if (interpreter == null) {
            Log.e("TFLiteManager", "Interpreter is null");
            return null;
        }
        // Change the next line to match your model's output size
        float[][] output = new float[1][4]; // <-- 4 if model output is [1, 4]
        interpreter.run(input, output);
        return output[0];
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
