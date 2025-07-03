package com.example.dle_prototype;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelValidator {

    // Returns true if model loads successfully
    public static boolean isModelValid(Context context, String modelFileName) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            Interpreter interpreter = new Interpreter(modelBuffer);
            interpreter.close();
            return true;
        } catch (Exception e) {
            Log.e("ModelValidator", "Model load failed: " + e.getMessage());
            return false;
        }
    }

    // Shows Toast indicating model validity
    public static void validateModel(Context context) {
        boolean isValid = isModelValid(context, "dle_model.tflite");
        Toast.makeText(context,
                isValid ? "✅ AI model loaded successfully!" : "❌ AI model failed to load.",
                Toast.LENGTH_LONG).show();
    }
}