package com.example.trainingdashboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trainingdashboard.dto.WorkoutTypeResponse;
import com.example.trainingdashboard.service.WorkoutService;

@RestController
@RequestMapping("/api/workout-types")
public class WorkoutTypeController {

    private final WorkoutService workoutService;

    public WorkoutTypeController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping
    public List<WorkoutTypeResponse> listWorkoutTypes() {
        return workoutService.listWorkoutTypes();
    }
}
