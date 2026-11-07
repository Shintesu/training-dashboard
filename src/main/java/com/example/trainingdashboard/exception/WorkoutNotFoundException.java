package com.example.trainingdashboard.exception;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(Long id) {
        super("No se encontró el entrenamiento con id " + id);
    }
}
