package com.example.trainingdashboard.enums;

public enum WorkoutType {

    RUNNING("Correr", 10),
    CYCLING("Ciclismo", 8),
    SWIMMING("Natación", 9),
    WEIGHTS("Pesas", 6),
    WALKING("Caminata", 5);

    private final String label;
    private final int caloriesPerMinute;

    WorkoutType(String label, int caloriesPerMinute) {
        this.label = label;
        this.caloriesPerMinute = caloriesPerMinute;
    }

    public String getLabel() {
        return label;
    }

    public int getCaloriesPerMinute() {
        return caloriesPerMinute;
    }
}
