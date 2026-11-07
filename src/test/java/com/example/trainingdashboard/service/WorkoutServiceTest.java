package com.example.trainingdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trainingdashboard.dto.WorkoutRequest;
import com.example.trainingdashboard.dto.WorkoutResponse;
import com.example.trainingdashboard.dto.WorkoutTypeResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.exception.WorkoutNotFoundException;
import com.example.trainingdashboard.repository.WorkoutRepository;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @Mock
    private WorkoutRepository workoutRepository;

    @InjectMocks
    private WorkoutService workoutService;

    @ParameterizedTest
    @CsvSource({
            "RUNNING, 1, 10",
            "RUNNING, 1440, 14400",
            "CYCLING, 1, 8",
            "CYCLING, 1440, 11520",
            "SWIMMING, 1, 9",
            "SWIMMING, 1440, 12960",
            "WEIGHTS, 1, 6",
            "WEIGHTS, 1440, 8640",
            "WALKING, 1, 5",
            "WALKING, 1440, 7200"
    })
    void calculatesCaloriesForEveryTypeAtMinimumAndMaximumDuration(
            WorkoutType type, int durationMinutes, int expectedCalories) {
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutResponse response = workoutService.registerWorkout(new WorkoutRequest(type, durationMinutes, DATE));

        assertThat(response.calories()).isEqualTo(expectedCalories);
    }

    @Test
    void savesWorkoutWithCalculatedCalories() {
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workoutService.registerWorkout(new WorkoutRequest(WorkoutType.RUNNING, 30, DATE));

        ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);
        verify(workoutRepository).save(captor.capture());
        Workout saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(WorkoutType.RUNNING);
        assertThat(saved.getDurationMinutes()).isEqualTo(30);
        assertThat(saved.getCalories()).isEqualTo(300);
        assertThat(saved.getDate()).isEqualTo(DATE);
    }

    @Test
    void buildsResponseFromPersistedWorkout() {
        Workout persisted = new Workout(WorkoutType.RUNNING, 30, 300, DATE);
        persisted.setId(42L);
        when(workoutRepository.save(any(Workout.class))).thenReturn(persisted);

        WorkoutResponse response = workoutService.registerWorkout(
                new WorkoutRequest(WorkoutType.RUNNING, 30, DATE));

        assertThat(response).isEqualTo(new WorkoutResponse(42L, WorkoutType.RUNNING, 30, 300, DATE));
    }

    @Test
    void keepsRepositoryOrderWhenListingWorkouts() {
        Workout first = new Workout(WorkoutType.RUNNING, 30, 300, DATE);
        first.setId(1L);
        Workout second = new Workout(WorkoutType.CYCLING, 30, 240, DATE);
        second.setId(2L);
        when(workoutRepository.findAllByOrderByDateDescIdDesc()).thenReturn(List.of(first, second));

        List<WorkoutResponse> responses = workoutService.listWorkouts();

        assertThat(responses).extracting(WorkoutResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void listsWorkoutTypesInDeclarationOrder() {
        assertThat(workoutService.listWorkoutTypes()).containsExactly(
                new WorkoutTypeResponse("RUNNING", "Correr", 10),
                new WorkoutTypeResponse("CYCLING", "Ciclismo", 8),
                new WorkoutTypeResponse("SWIMMING", "Natación", 9),
                new WorkoutTypeResponse("WEIGHTS", "Pesas", 6),
                new WorkoutTypeResponse("WALKING", "Caminata", 5));
    }

    @Test
    void deletesExistingWorkout() {
        when(workoutRepository.existsById(7L)).thenReturn(true);

        workoutService.deleteWorkout(7L);

        verify(workoutRepository).deleteById(7L);
    }

    @Test
    void rejectsDeletingUnknownWorkout() {
        when(workoutRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> workoutService.deleteWorkout(7L))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessage("No se encontró el entrenamiento con id 7");
        verify(workoutRepository, never()).deleteById(any());
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> workoutService.registerWorkout(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request");
    }
}
