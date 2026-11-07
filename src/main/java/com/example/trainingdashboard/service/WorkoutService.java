package com.example.trainingdashboard.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingdashboard.dto.WorkoutRequest;
import com.example.trainingdashboard.dto.WorkoutResponse;
import com.example.trainingdashboard.dto.WorkoutTypeResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.exception.WorkoutNotFoundException;
import com.example.trainingdashboard.repository.WorkoutRepository;

@Service
public class WorkoutService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutService.class);

    private final WorkoutRepository workoutRepository;

    public WorkoutService(WorkoutRepository workoutRepository) {
        this.workoutRepository = workoutRepository;
    }

    @Transactional
    public WorkoutResponse registerWorkout(WorkoutRequest request) {
        Objects.requireNonNull(request, "request");
        int calories = request.type().getCaloriesPerMinute() * request.durationMinutes();
        Workout workout = workoutRepository.save(
                new Workout(request.type(), request.durationMinutes(), calories, request.date()));
        log.debug("Registered workout {} of type {}", workout.getId(), workout.getType());
        return toResponse(workout);
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> listWorkouts() {
        return workoutRepository.findAllByOrderByDateDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteWorkout(Long id) {
        if (!workoutRepository.existsById(id)) {
            throw new WorkoutNotFoundException(id);
        }
        workoutRepository.deleteById(id);
        log.debug("Deleted workout {}", id);
    }

    @Transactional(readOnly = true)
    public List<WorkoutTypeResponse> listWorkoutTypes() {
        return Arrays.stream(WorkoutType.values())
                .map(type -> new WorkoutTypeResponse(type.name(), type.getLabel(), type.getCaloriesPerMinute()))
                .toList();
    }

    private WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(workout.getId(), workout.getType(), workout.getDurationMinutes(),
                workout.getCalories(), workout.getDate());
    }
}
