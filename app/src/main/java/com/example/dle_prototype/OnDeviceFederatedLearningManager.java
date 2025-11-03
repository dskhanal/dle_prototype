// OnDeviceFederatedLearningManager.java
package com.example.dle_prototype;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnDeviceFederatedLearningManager {
    private static final String TAG = "FederatedLearning";
    // This must be a TFLite model prepared for on-device training.
    private static final String MODEL_NAME = "dle_model.tflite";

    private TransferLearningModelWrapper modelWrapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void init(Context context, InitCallback callback) {
        executor.execute(() -> {
            try {
                modelWrapper = new TransferLearningModelWrapper(context, MODEL_NAME);
                Log.d(TAG, "Trainable TFLite model loaded successfully.");
                callback.onSuccess();
            } catch (IOException e) {
                Log.e(TAG, "Failed to load trainable model", e);
                callback.onError(e);
            }
        });
    }

    public void runInferenceAsync(float[] inputFeatures, InferenceCallback callback) {
        executor.execute(() -> {
            if (modelWrapper == null) {
                callback.onError(new IllegalStateException("Model not initialized."));
                return;
            }
            float[] prediction = modelWrapper.predict(inputFeatures);
            callback.onPrediction(prediction);
        });
    }

    public void performLocalTrainingAsync(float[] inputFeatures, float[] label, TrainCallback callback) {
        executor.execute(() -> {
            if (modelWrapper == null) {
                callback.onError(new IllegalStateException("Model not initialized."));
                return;
            }
            float loss = modelWrapper.train(inputFeatures, label);
            callback.onTrainingComplete(loss);
        });
    }


    public void exportWeightsAsync(ExportCallback callback) {
        executor.execute(() -> {
            if (modelWrapper == null) {
                callback.onError(new IllegalStateException("Model not initialized."));
                return;
            }
            ByteBuffer weights = modelWrapper.exportWeights();
            callback.onWeightsExported(weights);
        });
    }

    public void close() {
        executor.execute(() -> {
            if (modelWrapper != null) {
                modelWrapper.close();
            }
        });
        executor.shutdown();
    }

    // Define Callbacks
    public interface InitCallback { void onSuccess(); void onError(Exception e); }
    public interface InferenceCallback { void onPrediction(float[] value); void onError(Exception e); }
    public interface TrainCallback { void onTrainingComplete(float loss); void onError(Exception e); }
    public interface ExportCallback { void onWeightsExported(ByteBuffer weights); void onError(Exception e); }
}