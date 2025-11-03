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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private float lastDifficulty = 0f;
    private float lastCategory = 1f;
    private long startTime;

    private TextView questionView, questionCounter, explanationText, exampleText, metaText, categoryPrompt;
    private Button buttonTrue, buttonFalse;
    private Button btnCategoryHTML, btnCategoryCSS, btnCategoryJS, btnCategoryPHP, btnCategoryMYSQL, btnCategoryPYTHON;
    private LinearLayout quizContentLayout, categoryButtonsLayout, mcqOptionsLayout;

    private int selectedCategory = 1;
    private String selectedCategoryName = "HTML";
    private boolean quizStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Layout and views
        quizContentLayout = findViewById(R.id.quizContentLayout);
        quizContentLayout.setVisibility(View.GONE);
        categoryButtonsLayout = findViewById(R.id.categoryButtonsLayout);
        mcqOptionsLayout = findViewById(R.id.mcqOptionsLayout);

        questionView = findViewById(R.id.questionText);
        questionCounter = findViewById(R.id.questionCounter);
        explanationText = findViewById(R.id.explanationText);
        exampleText = findViewById(R.id.exampleText);
        metaText = findViewById(R.id.metaText);
        buttonTrue = findViewById(R.id.buttonTrue);
        buttonFalse = findViewById(R.id.buttonFalse);
        categoryPrompt = findViewById(R.id.categoryPrompt);

        btnCategoryHTML = findViewById(R.id.btnCategoryHTML);
        btnCategoryCSS = findViewById(R.id.btnCategoryCSS);
        btnCategoryJS = findViewById(R.id.btnCategoryJS);
        btnCategoryPHP = findViewById(R.id.btnCategoryPHP);
        btnCategoryMYSQL = findViewById(R.id.btnCategoryMYSQL);
        btnCategoryPYTHON = findViewById(R.id.btnCategoryPYTHON);

        btnCategoryHTML.setOnClickListener(v -> selectCategory(1, "HTML"));
        btnCategoryCSS.setOnClickListener(v -> selectCategory(2, "CSS"));
        btnCategoryJS.setOnClickListener(v -> selectCategory(3, "JavaScript"));
        btnCategoryPHP.setOnClickListener(v -> selectCategory(4, "PHP"));
        btnCategoryMYSQL.setOnClickListener(v -> selectCategory(5, "MySQL"));
        btnCategoryPYTHON.setOnClickListener(v -> selectCategory(6, "Python"));

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

        // Hide category buttons after category is selected
        categoryButtonsLayout.setVisibility(View.GONE);
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

    private void setQuizControlsEnabled(boolean enabled) {
        buttonTrue.setEnabled(enabled);
        buttonFalse.setEnabled(enabled);
        buttonTrue.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        buttonFalse.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        // MCQ buttons handled dynamically
    }

    private List<Question> loadQuestionsFromJson(Context context, String categoryFilter) {
        List<Question> list = new ArrayList<>();
        try (InputStream is = context.getAssets().open("questions.json");
             JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginArray();
            int total = 0;
            while (reader.hasNext()) {
                Question q = parseQuestion(reader);
                total++;
                if (q.getCategory().trim().equalsIgnoreCase(categoryFilter.trim())) {
                    list.add(q);
                }
            }
            reader.endArray();
            Toast.makeText(context, "Loaded " + list.size() + " questions for " + categoryFilter + " (from " + total + " total)", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to load questions: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return list;
    }

    private Question parseQuestion(JsonReader reader) throws Exception {
        String type = "true_false";
        String question = "", explanation = "", example = "", category = "", difficulty = "";
        List<String> options = null;
        Object answer = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "type": type = reader.nextString(); break;
                case "question": question = reader.nextString(); break;
                case "options":
                    options = new ArrayList<>();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        options.add(reader.nextString());
                    }
                    reader.endArray();
                    break;
                case "answer":
                    if ("mcq".equals(type)) {
                        answer = reader.nextString();
                    } else {
                        answer = reader.nextBoolean();
                    }
                    break;
                case "explanation": explanation = reader.nextString(); break;
                case "example": example = reader.nextString(); break;
                case "category": category = reader.nextString(); break;
                case "difficulty": difficulty = reader.nextString(); break;
                default: reader.skipValue(); break;
            }
        }
        reader.endObject();
        return new Question(type, question, options, answer, explanation, example, category, difficulty);
    }

    private void handleMCQAnswer(String userAnswer) {
        if (!quizStarted || questions.isEmpty() || currentIndex >= questions.size()) return;

        Question q = questions.get(currentIndex);
        boolean correct = false;
        if (q.getAnswer() instanceof String) {
            correct = userAnswer.equals(q.getAnswer());
        }
        if (correct) score++;

        lastDifficulty = toFloat(q.getDifficulty());

        explanationText.setText((correct ? "✅ Correct!" : "❌ Incorrect!") + "\n" + q.getExplanation());
        explanationText.setVisibility(View.VISIBLE);
        exampleText.setText(q.getExample());
        exampleText.setVisibility(View.VISIBLE);

        // Hide MCQ answer buttons after selection
        for (int i = 0; i < mcqOptionsLayout.getChildCount(); i++) {
            View v = mcqOptionsLayout.getChildAt(i);
            v.setEnabled(false);
            v.setVisibility(View.INVISIBLE);
        }

        currentIndex++;
        new Handler().postDelayed(() -> {
            if (currentIndex < questions.size()) {
                displayNextQuestion();
            } else {
                finishQuiz();
            }
        }, 2000);
    }

    private void handleAnswer(boolean userAnswer) {
        if (!quizStarted || questions.isEmpty() || currentIndex >= questions.size()) return;

        Question q = questions.get(currentIndex);
        boolean correct = false;
        if (q.getAnswer() instanceof Boolean) {
            correct = ((Boolean) q.getAnswer()) == userAnswer;
        }
        if (correct) score++;

        lastDifficulty = toFloat(q.getDifficulty());

        explanationText.setText((correct ? "✅ Correct!" : "❌ Incorrect!") + "\n" + q.getExplanation());
        explanationText.setVisibility(View.VISIBLE);
        exampleText.setText(q.getExample());
        exampleText.setVisibility(View.VISIBLE);

        // Hide answer buttons until next question
        buttonTrue.setEnabled(false);
        buttonFalse.setEnabled(false);
        buttonTrue.setVisibility(View.INVISIBLE);
        buttonFalse.setVisibility(View.INVISIBLE);

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
        exampleText.setVisibility(View.GONE);

        if ("mcq".equals(q.getType())) {
            buttonTrue.setVisibility(View.GONE);
            buttonFalse.setVisibility(View.GONE);
            mcqOptionsLayout.setVisibility(View.VISIBLE);
            mcqOptionsLayout.removeAllViews();
            List<String> opts = q.getOptions();

            for (String opt : opts) {
                Button btn = new Button(this, null, 0, R.style.MCQOptionButton);
                btn.setText(toSentenceCase(opt)); // Or original case as you prefer
                btn.setAllCaps(false);
                btn.setOnClickListener(v -> handleMCQAnswer(opt));
                mcqOptionsLayout.addView(btn);
            }
        } else { // true_false
            mcqOptionsLayout.setVisibility(View.GONE);
            buttonTrue.setEnabled(true);
            buttonFalse.setEnabled(true);
            buttonTrue.setVisibility(View.VISIBLE);
            buttonFalse.setVisibility(View.VISIBLE);
        }
    }

    private void finishQuiz() {
        String summary = "Quiz complete!\n"
                + "Score: " + score + "/" + questions.size()
                + "\nCategory: " + selectedCategoryName
                + "\nDifficulty: " + lastDifficulty;
        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();

        UserStatsManager.recordQuiz(this, score, lastDifficulty, lastDifficulty);

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
    private String toSentenceCase(String input) {
        if (input == null || input.isEmpty()) return input;
        input = input.trim();
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

}
