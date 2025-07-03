package com.example.dle_prototype;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.os.Handler;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.TensorFlowLite;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Arrays;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class ModelDiagnosticsActivity extends Activity {

    TextView textView;
    Handler handler = new Handler();
    private static final String MODEL_FILE_NAME = "dle_model.tflite"; // Centralized model file name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_diagnostics);
        textView = findViewById(R.id.textView);

        // Run all checks in background to avoid UI freeze
        new Thread(() -> {
            StringBuilder log = new StringBuilder();
            log.append("Starting Model Diagnostics...\n\n");

            // 1. Model Validity Check (from ModelValidator)
            log.append("--- Model Validity Check ---\n");
            boolean isValid = isModelValid(this, MODEL_FILE_NAME, log); // Pass log to append messages
            log.append(isValid ? "✅ AI model loaded successfully!\n" : "❌ AI model failed to load.\n");
            runOnUiThread(() -> Toast.makeText(ModelDiagnosticsActivity.this,
                    isValid ? "✅ AI model loaded successfully!" : "❌ AI model failed to load.",
                    Toast.LENGTH_LONG).show());

            if (isValid) {
                // 2. Component Check (from TFCheck)
                log.append("\n--- Component Details Check ---\n");
                String componentCheckResult = runComponentCheck();
                log.append(componentCheckResult);

                // 3. Inference Check (from InferenceCheck)
                log.append("\n--- Inference Simulation Check ---\n");
                String inferenceCheckResult = runInferenceCheck();
                log.append(inferenceCheckResult);
            } else {
                log.append("\nSkipping further checks due to model loading failure.\n");
            }


            log.append("\nDiagnostics Complete.\n");
            runOnUiThread(() -> textView.setText(log.toString()));

            // Only return to main if all essential checks are successful, or based on your desired logic
            if (isValid) { // You might want to add more conditions here based on inference success
                handler.postDelayed(() -> {
                    Intent intent = new Intent(ModelDiagnosticsActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }, 5000);
            }
        }).start();
    }

    // --- Methods from ModelValidator.java ---
    public boolean isModelValid(Context context, String modelFileName, StringBuilder log) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            Interpreter interpreter = new Interpreter(modelBuffer);
            interpreter.close();
            log.append("Model file found and can be mapped.\n");
            return true;
        } catch (Exception e) {
            Log.e("ModelDiagnostics", "Model load failed: " + e.getMessage());
            log.append("Error: ").append(e.getMessage()).append("\n");
            return false;
        }
    }

    // --- Methods from TFCheck.java (modified to be called from here) ---
    private String runComponentCheck() {
        StringBuilder log = new StringBuilder();
        String tfliteRuntimeVersion = TensorFlowLite.runtimeVersion();
        log.append("TensorFlowLite Runtime Version: ").append(tfliteRuntimeVersion).append("\n");

        try {
            byte[] modelBytes = readModelBytesFromAssets(MODEL_FILE_NAME);
            log.append("Model File Size: ").append(modelBytes.length).append(" bytes\n");

            // float[] floats = extractFloats(modelBytes, 5);
            // log.append("\nFirst 5 floats in model file (for illustration):\n");
            // for (float f : floats) {
            //    log.append(String.format(Locale.US, "%.6f", f)).append("\n");
            //}

            File modelFile = copyAssetToCache(MODEL_FILE_NAME);
            Interpreter interpreter = new Interpreter(modelFile);

            log.append("\n✅ Interpreter successfully loaded the model for component check.\n");
            log.append("Input Tensor Count: ").append(interpreter.getInputTensorCount()).append("\n");
            log.append("Output Tensor Count: ").append(interpreter.getOutputTensorCount()).append("\n");

            int inputCount = interpreter.getInputTensorCount();
            for (int i = 0; i < inputCount; i++) {
                int[] inputShape = interpreter.getInputTensor(i).shape();
                DataType inputType = interpreter.getInputTensor(i).dataType();
                String inputShapeStr = java.util.Arrays.toString(inputShape);
                log.append("Input Tensor #").append(i).append("\n");
                log.append("Tensor shape=").append(inputShapeStr).append("\n");
                log.append("Tensor DataType=").append(inputType).append("\n");
            }

            int outputCount = interpreter.getOutputTensorCount();
            for (int i = 0; i < outputCount; i++) {
                int[] outputShape = interpreter.getOutputTensor(i).shape();
                DataType outputType = interpreter.getOutputTensor(i).dataType();
                String outputShapeStr = java.util.Arrays.toString(outputShape);
                log.append("Output Tensor #").append(i).append(": shape=").append(outputShapeStr).append(", dataType=").append(outputType).append("\n");
            }

            for (int i = 0; i < interpreter.getInputTensorCount(); i++) {
                log.append(String.format(Locale.US, "\nInput Tensor #%d: %s", i,
                        interpreter.getInputTensor(i).shapeSignature() != null ?
                                shapeToString(interpreter.getInputTensor(i).shapeSignature()) :
                                "unknown shape"));
            }
            for (int i = 0; i < interpreter.getOutputTensorCount(); i++) {
                log.append(String.format(Locale.US, "\nOutput Tensor #%d: %s", i,
                        interpreter.getOutputTensor(i).shapeSignature() != null ?
                                shapeToString(interpreter.getOutputTensor(i).shapeSignature()) :
                                "unknown shape"));
            }
            interpreter.close();
        } catch (Exception e) {
            log.append("\n❌ Error during component check:\n").append(e.getMessage());
        }
        return log.toString();
    }

    private byte[] readModelBytesFromAssets(String assetFileName) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream buffer = null;
        try {
            inputStream = getAssets().open(assetFileName);
            buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } finally {
            if (inputStream != null) inputStream.close();
            if (buffer != null) buffer.close();
        }
    }

    private File copyAssetToCache(String assetFileName) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File outFile = new File(getCacheDir(), assetFileName);
        try {
            inputStream = getAssets().open(assetFileName);
            outputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return outFile;
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        }
    }

    private float[] extractFloats(byte[] data, int count) {
        float[] floats = new float[count];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int floatSize = Float.BYTES;
        for (int i = 0; i < count; i++) {
            if (buffer.remaining() >= floatSize) {
                floats[i] = buffer.getFloat();
            } else {
                break;
            }
        }
        return floats;
    }

    private String shapeToString(int[] shape) {
        if (shape == null) return "unknown";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < shape.length; i++) {
            sb.append(shape[i]);
            if (i < shape.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }


    // --- Methods from InferenceCheck.java (modified to be called from here) ---
    private String runInferenceCheck() {
        StringBuilder log = new StringBuilder();

        try {
            File modelFile = new File(getCacheDir(), MODEL_FILE_NAME);
            if (!modelFile.exists()) {
                log.append("\nModel file not found for inference: ").append(modelFile.getAbsolutePath());
                return log.toString();
            }

            Interpreter interpreter = new Interpreter(modelFile);

            int[] inputShape = interpreter.getInputTensor(0).shape();
            DataType inputType = interpreter.getInputTensor(0).dataType();
            log.append("Input Tensor: shape=").append(Arrays.toString(inputShape))
                    .append(", dataType=").append(inputType).append("\n");

            int[] outputShape = interpreter.getOutputTensor(0).shape();
            DataType outputType = interpreter.getOutputTensor(0).dataType();
            log.append("Output Tensor: shape=").append(Arrays.toString(outputShape))
                    .append(", dataType=").append(outputType).append("\n");

            Object input = createArray(inputShape, inputType);
            fillArray(input);

            Object output = createArray(outputShape, outputType);

            interpreter.run(input, output);

            log.append("\nSimulated Inference Output:\n").append(arrayToString(output));
            interpreter.close();

        } catch (Exception e) {
            log.append("\n❌ Error during inference simulation:\n").append(e.toString());
        }
        return log.toString();
    }

    private Object createArray(int[] shape, DataType type) {
        if (shape == null || shape.length == 0) {
            switch (type) {
                case FLOAT32: return 0f;
                case INT32:   return 0;
                case UINT8:   return (byte) 0;
                default:      return 0f;
            }
        }
        return java.lang.reflect.Array.newInstance(typeToClass(type), shape);
    }

    private Class<?> typeToClass(DataType type) {
        switch (type) {
            case FLOAT32: return float.class;
            case INT32:   return int.class;
            case UINT8:   return byte.class;
            default:      return float.class;
        }
    }

    private void fillArray(Object array) {
        if (array instanceof float[]) {
            Arrays.fill((float[]) array, 1f);
        } else if (array instanceof int[]) {
            Arrays.fill((int[]) array, 1);
        } else if (array instanceof byte[]) {
            Arrays.fill((byte[]) array, (byte) 1);
        } else if (array.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(array);
            for (int i = 0; i < len; i++) {
                fillArray(java.lang.reflect.Array.get(array, i));
            }
        }
    }

    private String arrayToString(Object array) {
        if (array == null) return "null";
        if (array.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(array);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arrayToString(java.lang.reflect.Array.get(array, i)));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return array.toString();
        }
    }
}