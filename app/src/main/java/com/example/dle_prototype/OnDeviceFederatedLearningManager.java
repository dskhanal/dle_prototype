package com.example.dle_prototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class OnDeviceFederatedLearningManager {
    private static final String PREF_NAME = "personalization_weights";
    private static final String WEIGHTS_KEY = "model_weights";
    private static final int NUM_WEIGHTS = 5; // set this to match your model
    private static final String TAG = "FederatedLearning";

    // Load model weights from SharedPreferences
    public static float[] loadWeights(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String weightsJson = prefs.getString(WEIGHTS_KEY, null);
        float[] weights = new float[NUM_WEIGHTS];
        if (weightsJson != null) {
            try {
                JSONArray arr = new JSONArray(weightsJson);
                for (int i = 0; i < arr.length(); i++) {
                    weights[i] = (float) arr.getDouble(i);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to load weights, resetting", e);
                Arrays.fill(weights, 0.5f);
            }
        } else {
            Arrays.fill(weights, 0.5f);
        }
        return weights;
    }

    // Save model weights to SharedPreferences
    public static void saveWeights(Context context, float[] weights) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        for (float w : weights) arr.put(w);
        prefs.edit().putString(WEIGHTS_KEY, arr.toString()).apply();
    }

    // Simulate local training/incremental learning using new user data
    // This is a simple SGD-like update for demonstration
    public static float[] trainOnUserData(float[] oldWeights, float[] userFeatures, float userLabel, float lr) {
        float[] newWeights = Arrays.copyOf(oldWeights, oldWeights.length);
        float prediction = predict(oldWeights, userFeatures);
        float error = userLabel - prediction;
        // Update rule: w = w + lr * error * x
        for (int i = 0; i < newWeights.length; i++) {
            newWeights[i] += lr * error * userFeatures[i];
        }
        return newWeights;
    }

    // Simple prediction (linear)
    public static float predict(float[] weights, float[] features) {
        float pred = 0f;
        for (int i = 0; i < weights.length; i++) {
            pred += weights[i] * features[i];
        }
        // Optional: add activation (sigmoid, clamp, etc.)
        pred = Math.max(0f, Math.min(pred, 1f)); // clamp between 0 and 1
        return pred;
    }

    // Export current weights for server aggregation (if needed)
    public static JSONObject exportWeightUpdate(float[] oldWeights, float[] newWeights) {
        JSONObject obj = new JSONObject();
        JSONArray deltas = new JSONArray();
        try {
            for (int i = 0; i < oldWeights.length; i++) {
                deltas.put(newWeights[i] - oldWeights[i]);
            }
            obj.put("weight_deltas", deltas);
        } catch (JSONException e) {
            // ignore
        }
        return obj;
    }

    // (Optional) Aggregate weight updates (for simulated federated learning, on device or on server)
    public static float[] aggregateWeightUpdates(float[][] allDeltas) {
        float[] agg = new float[NUM_WEIGHTS];
        Arrays.fill(agg, 0f);
        for (float[] deltas : allDeltas) {
            for (int i = 0; i < agg.length; i++) {
                agg[i] += deltas[i];
            }
        }
        for (int i = 0; i < agg.length; i++) {
            agg[i] /= allDeltas.length;
        }
        return agg;
    }

    // Apply aggregated updates to local weights
    public static float[] applyAggregatedUpdate(float[] weights, float[] aggDeltas) {
        float[] updated = Arrays.copyOf(weights, weights.length);
        for (int i = 0; i < updated.length; i++) {
            updated[i] += aggDeltas[i];
        }
        return updated;
    }
}
