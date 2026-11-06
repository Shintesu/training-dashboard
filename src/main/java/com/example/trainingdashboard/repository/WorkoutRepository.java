package com.example.trainingdashboard.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trainingdashboard.entity.Workout;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByDateBetween(LocalDate start, LocalDate end);

    List<Workout> findAllByOrderByDateDescIdDesc();
}
