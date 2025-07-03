package com.example.dle_prototype;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.util.JsonReader;
import android.content.Context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private float lastDifficulty = 0f;
    private float lastCategory = 1f; // default: HTML
    private long startTime;

    private TextView questionView, questionCounter, explanationText, metaText, categoryPrompt;
    private Button buttonTrue, buttonFalse;
    private Button btnCategoryHTML, btnCategoryCSS, btnCategoryJS;
    private LinearLayout quizContentLayout, categoryButtonsLayout;

    private int selectedCategory = 1; // 1=HTML, 2=CSS, 3=JavaScript
    private String selectedCategoryName = "HTML";
    private boolean quizStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Get layouts and views
        quizContentLayout = findViewById(R.id.quizContentLayout);
        quizContentLayout.setVisibility(View.GONE); // Hide quiz UI initially

        categoryButtonsLayout = findViewById(R.id.categoryButtonsLayout);

        questionView = findViewById(R.id.questionText);
        questionCounter = findViewById(R.id.questionCounter);
        explanationText = findViewById(R.id.explanationText);
        metaText = findViewById(R.id.metaText);
        buttonTrue = findViewById(R.id.buttonTrue);
        buttonFalse = findViewById(R.id.buttonFalse);
        categoryPrompt = findViewById(R.id.categoryPrompt);

        btnCategoryHTML = findViewById(R.id.btnCategoryHTML);
        btnCategoryCSS = findViewById(R.id.btnCategoryCSS);
        btnCategoryJS = findViewById(R.id.btnCategoryJS);

        // Category selection buttons
        btnCategoryHTML.setOnClickListener(v -> selectCategory(1, "HTML"));
        btnCategoryCSS.setOnClickListener(v -> selectCategory(2, "CSS"));
        btnCategoryJS.setOnClickListener(v -> selectCategory(3, "JavaScript"));

        // Disable quiz controls until category selected
        setQuizControlsEnabled(false);

        buttonTrue.setOnClickListener(v -> handleAnswer(true));
        buttonFalse.setOnClickListener(v -> handleAnswer(false));
    }

    private void selectCategory(int catNum, String catName) {
        selectedCategory = catNum;
        selectedCategoryName = catName;
        lastCategory = catNum;
        Toast.makeText(this, "Category selected: " + catName, Toast.LENGTH_SHORT).show();
        categoryPrompt.setText("Category: " + catName);
        startQuizForCategory(catName);

        // Hide all category buttons after selection
        categoryButtonsLayout.setVisibility(View.GONE);

        // Show quiz content
        quizContentLayout.setVisibility(View.VISIBLE);
    }

    private void startQuizForCategory(String catName) {
        questions = loadQuestionsFromJson(this, catName);
        Collections.shuffle(questions);
        if (questions.size() > 10) {
            questions = questions.subList(0, 10);
        }
        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions for " + catName, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentIndex = 0;
        score = 0;
        quizStarted = true;
        setQuizControlsEnabled(true);
        startTime = SystemClock.elapsedRealtime();
        displayNextQuestion();
    }

    // Only quiz True/False buttons are enabled/disabled here
    private void setQuizControlsEnabled(boolean enabled) {
        buttonTrue.setEnabled(enabled);
        buttonFalse.setEnabled(enabled);
    }

    private List<Question> loadQuestionsFromJson(Context context, String categoryFilter) {
        List<Question> list = new ArrayList<>();
        try (InputStream is = context.getAssets().open("questions.json");
             JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {
            reader.beginArray();
            while (reader.hasNext()) {
                Question q = parseQuestion(reader);
                if (q.getCategory().equalsIgnoreCase(categoryFilter)) {
                    list.add(q);
                }
            }
            reader.endArray();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to load questions", Toast.LENGTH_LONG).show();
        }
        return list;
    }

    private Question parseQuestion(JsonReader reader) throws Exception {
        String question = "", explanation = "", category = "", difficulty = "";
        boolean answer = false;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "question": question = reader.nextString(); break;
                case "answer": answer = reader.nextBoolean(); break;
                case "explanation": explanation = reader.nextString(); break;
                case "category": category = reader.nextString(); break;
                case "difficulty": difficulty = reader.nextString(); break;
                default: reader.skipValue(); break;
            }
        }
        reader.endObject();
        return new Question(question, answer, explanation, category, difficulty);
    }

    private void handleAnswer(boolean userAnswer) {
        if (!quizStarted || questions.isEmpty() || currentIndex >= questions.size()) return;

        Question q = questions.get(currentIndex);
        if (q.getAnswer() == userAnswer) score++;

        lastDifficulty = toFloat(q.getDifficulty());
        // lastCategory is already set from user selection

        explanationText.setText((q.getAnswer() == userAnswer ? "✅ Correct!" : "❌ Incorrect!") + "\n" + q.getExplanation());
        explanationText.setVisibility(View.VISIBLE);

        currentIndex++;
        new Handler().postDelayed(() -> {
            if (currentIndex < questions.size()) {
                displayNextQuestion();
            } else {
                finishQuiz();
            }
        }, 2000);
    }

    private void displayNextQuestion() {
        if (currentIndex >= questions.size()) return;
        Question q = questions.get(currentIndex);
        questionView.setText(q.getQuestion());
        questionCounter.setText((currentIndex + 1) + " of " + questions.size());
        metaText.setText("Category: " + q.getCategory() + " | Difficulty: " + q.getDifficulty());
        explanationText.setVisibility(View.GONE);
    }

    private void finishQuiz() {
        UserStatsManager.recordQuiz(this, score, lastDifficulty, lastCategory);
        String summary = "Quiz complete!\n"
                + "Score: " + score + "/" + questions.size()
                + "\nCategory: " + selectedCategoryName
                + "\nDifficulty: " + lastDifficulty;
        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
        finish();
    }

    // String to float helper for difficulty
    private float toFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            switch (s.toLowerCase()) {
                case "easy": return 1f;
                case "medium": return 2f;
                case "hard": return 3f;
                default: return 0f;
            }
        }
    }
}
