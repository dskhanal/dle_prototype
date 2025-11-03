package com.example.dle_prototype;

import java.util.List;

public class Question {
    private final String type;
    private final String question;
    private final List<String> options;
    private final Object answer; // String for MCQ, Boolean for True/False
    private final String explanation;
    private final String example;
    private final String category;
    private final String difficulty;

    public Question(String type, String question, List<String> options, Object answer, String explanation, String example, String category, String difficulty) {
        this.type = type;
        this.question = question;
        this.options = options;
        this.answer = answer;
        this.explanation = explanation;
        this.example = example;
        this.category = category;
        this.difficulty = difficulty;
    }

    public String getType() { return type; }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public Object getAnswer() { return answer; }
    public String getExplanation() { return explanation; }
    public String getExample() { return example; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
}
