package com.example.dle_prototype;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStatsManager {

    private static final String PREFS_NAME = "user_stats";

    // Save a login event
    public static void recordLogin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int loginCount = prefs.getInt("login_count", 0) + 1;
        prefs.edit().putInt("login_count", loginCount).apply();
    }

    // Track session time: call on app/activity pause/stop
    public static void recordSessionTime(Context context, long sessionMillis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long totalTime = prefs.getLong("total_time_spent", 0) + sessionMillis;
        prefs.edit().putLong("total_time_spent", totalTime).apply();
    }

    // Record quiz event
    public static void recordQuiz(Context context, float quizScore, float difficulty, float category) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("last_quiz_score", quizScore)
                .putFloat("last_difficulty_reached", difficulty)
                .putFloat("last_category_selected", category)
                .apply();
    }

    // Retrieve all stats as a POJO
    public static UserStats getStats(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        UserStats stats = new UserStats();
        stats.loginCount = prefs.getInt("login_count", 0);
        stats.totalTimeSpentMillis = prefs.getLong("total_time_spent", 0);
        stats.lastQuizScore = prefs.getFloat("last_quiz_score", 0f);
        stats.lastDifficultyReached = prefs.getFloat("last_difficulty_reached", 0f);
        stats.lastCategorySelected = prefs.getFloat("last_category_selected", 0f);
        return stats;
    }

    // Clear stats (for testing or logout)
    public static void clearStats(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    // POJO for easy access
    public static class UserStats {
        public int loginCount;
        public long totalTimeSpentMillis;
        public float lastQuizScore;
        public float lastDifficultyReached;
        public float lastCategorySelected;
    }
}
