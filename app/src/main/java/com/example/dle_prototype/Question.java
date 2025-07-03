package com.example.dle_prototype;

/**
 * Represents a quiz question with metadata.
 */
public class Question {
    private final String question;
    private final boolean answer;
    private final String explanation;
    private final String category;
    private final String difficulty;

    public Question(String question, boolean answer, String explanation, String category, String difficulty) {
        this.question = question;
        this.answer = answer;
        this.explanation = explanation;
        this.category = category;
        this.difficulty = difficulty;
    }

    public String getQuestion() {
        return question;
    }

    public boolean getAnswer() {
        return answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }
}