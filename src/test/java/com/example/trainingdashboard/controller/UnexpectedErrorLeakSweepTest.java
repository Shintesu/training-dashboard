package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.trainingdashboard.repository.WorkoutRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class UnexpectedErrorLeakSweepTest {

    private static final String UNEXPECTED = "Ocurrió un error inesperado. Inténtalo de nuevo más tarde";
    private static final String CONNECTION_STRING =
            "jdbc:postgresql://leak.invalid:5432/secret?password=super-secret-sweep";
    private static final String PASSWORD = "super-secret-sweep";
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "Exception",
            "org.springframework",
            "jakarta",
            "tools.jackson",
            "com.fasterxml",
            "at com.example",
            "jdbc:",
            "SQL",
            "Hibernate",
            "postgres",
            "stackTrace",
            "trace");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @MockitoBean
    private WorkoutRepository workoutRepository;

    @Test
    void returnsGenericInternalErrorAndSanitizedLogOnRepositoryFailure(CapturedOutput output) throws Exception {
        when(workoutRepository.findAllByOrderByDateDescIdDesc())
                .thenThrow(new DataAccessResourceFailureException("Could not open " + CONNECTION_STRING));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/workouts"))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String responseBody = response.body();

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(responseBody).contains(UNEXPECTED);
        for (String fragment : FORBIDDEN_FRAGMENTS) {
            assertThat(responseBody)
                    .as("response body must not contain %s", fragment)
                    .doesNotContain(fragment);
        }
        assertThat(responseBody).doesNotContain(CONNECTION_STRING, PASSWORD);
        assertThat(output.getAll()).doesNotContain(CONNECTION_STRING, PASSWORD);
        assertThat(output.getAll()).contains("DataAccessResourceFailureException");
    }
}
