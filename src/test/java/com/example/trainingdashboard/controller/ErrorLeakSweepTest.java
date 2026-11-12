package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrorLeakSweepTest {

    private static final String INVALID_DATA = "Los datos del entrenamiento no son válidos";
    private static final String INVALID_BODY = "El cuerpo de la solicitud no es válido";
    private static final String INVALID_ID = "El identificador del entrenamiento no es válido";
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorCases")
    void returnsExpectedStatusAndDoesNotLeakInternals(ErrorCase errorCase) throws Exception {
        HttpResponse<String> response = send(errorCase);
        String responseBody = response.body();

        assertThat(response.statusCode())
                .as("%s body: %s", errorCase.label(), responseBody)
                .isEqualTo(errorCase.expectedStatus());
        if (errorCase.expectedMessage() != null) {
            assertThat(responseBody)
                    .as("%s body", errorCase.label())
                    .contains(errorCase.expectedMessage());
        }
        if (errorCase.defaultSpringResponse()) {
            assertThat(responseBody)
                    .as("%s default Spring body", errorCase.label())
                    .doesNotContain("\"message\"", "\"trace\"", "\"exception\"");
        }
        assertNoLeak(errorCase.label(), responseBody);
    }

    @Test
    void servesRegistrationPageWithHtml() throws Exception {
        HttpResponse<String> response = sendPage("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(response.body()).contains("Registro de entrenamientos");
    }

    static Stream<Arguments> errorCases() {
        return Stream.of(
                Arguments.of(new ErrorCase("invalid fields: missing date", "POST", "/api/workouts",
                        "{\"type\":\"RUNNING\",\"durationMinutes\":30}", "application/json",
                        400, INVALID_DATA, false)),
                Arguments.of(new ErrorCase("unknown type", "POST", "/api/workouts",
                        "{\"type\":\"UNKNOWN\",\"durationMinutes\":30,\"date\":\"2026-08-12\"}",
                        "application/json", 400, INVALID_DATA, false)),
                Arguments.of(new ErrorCase("decimal duration", "POST", "/api/workouts",
                        "{\"type\":\"RUNNING\",\"durationMinutes\":30.5,\"date\":\"2026-08-12\"}",
                        "application/json", 400, INVALID_DATA, false)),
                Arguments.of(new ErrorCase("text duration", "POST", "/api/workouts",
                        "{\"type\":\"RUNNING\",\"durationMinutes\":\"30\",\"date\":\"2026-08-12\"}",
                        "application/json", 400, INVALID_DATA, false)),
                Arguments.of(new ErrorCase("malformed date", "POST", "/api/workouts",
                        "{\"type\":\"RUNNING\",\"durationMinutes\":30,\"date\":\"2026-02-30\"}",
                        "application/json", 400, INVALID_DATA, false)),
                Arguments.of(new ErrorCase("empty body", "POST", "/api/workouts",
                        "", "application/json", 400, INVALID_BODY, false)),
                Arguments.of(new ErrorCase("malformed json", "POST", "/api/workouts",
                        "{\"type\":\"RUNNING\",", "application/json", 400, INVALID_BODY, false)),
                Arguments.of(new ErrorCase("non-numeric id", "DELETE", "/api/workouts/abc",
                        null, null, 400, INVALID_ID, false)),
                Arguments.of(new ErrorCase("nonexistent id", "DELETE", "/api/workouts/999999999",
                        null, null, 404, "No se encontró el entrenamiento con id 999999999", false)),
                Arguments.of(new ErrorCase("unknown route", "GET", "/api/no-existe",
                        null, null, 404, null, true)),
                Arguments.of(new ErrorCase("method not allowed", "PUT", "/api/workouts/1",
                        null, null, 405, null, true)),
                Arguments.of(new ErrorCase("unsupported media type", "POST", "/api/workouts",
                        "text", "text/plain", 415, null, true)));
    }

    private HttpResponse<String> send(ErrorCase errorCase) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + errorCase.path()))
                .header("Accept", "application/json");
        if (errorCase.body() == null) {
            builder.method(errorCase.method(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", errorCase.contentType());
            builder.method(errorCase.method(), HttpRequest.BodyPublishers.ofString(
                    errorCase.body(), StandardCharsets.UTF_8));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPage(String path) throws Exception {
        // The static welcome page is negotiated as HTML; Accept: application/json alone returns 406.
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertNoLeak(String label, String responseBody) {
        for (String fragment : FORBIDDEN_FRAGMENTS) {
            assertThat(responseBody)
                    .as("%s body must not contain %s", label, fragment)
                    .doesNotContain(fragment);
        }
    }

    private record ErrorCase(
            String label,
            String method,
            String path,
            String body,
            String contentType,
            int expectedStatus,
            String expectedMessage,
            boolean defaultSpringResponse) {

        @Override
        public String toString() {
            return label;
        }
    }
}
