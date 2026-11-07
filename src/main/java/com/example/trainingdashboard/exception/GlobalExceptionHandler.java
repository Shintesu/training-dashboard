package com.example.trainingdashboard.exception;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.core.exc.StreamReadException;

import com.example.trainingdashboard.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String INVALID_DATA = "Los datos del entrenamiento no son válidos";
    private static final String INVALID_BODY = "El cuerpo de la solicitud no es válido";
    private static final String INVALID_ID = "El identificador del entrenamiento no es válido";
    private static final String UNEXPECTED = "Ocurrió un error inesperado. Inténtalo de nuevo más tarde";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return new ErrorResponse(INVALID_DATA, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException exception) {
        Optional<Map.Entry<String, String>> fieldError = findFieldError(exception.getCause());
        if (fieldError.isPresent()) {
            Map.Entry<String, String> entry = fieldError.get();
            return new ErrorResponse(INVALID_DATA, Map.of(entry.getKey(), entry.getValue()));
        }
        return new ErrorResponse(INVALID_BODY, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ErrorResponse(INVALID_ID, Map.of());
    }

    @ExceptionHandler(WorkoutNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(WorkoutNotFoundException exception) {
        return new ErrorResponse(exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(RuntimeException exception, HttpServletRequest request) {
        logUnexpectedError(request, exception);
        return new ErrorResponse(UNEXPECTED, Map.of());
    }

    private Optional<Map.Entry<String, String>> findFieldError(Throwable throwable) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && seen.add(current)) {
            if (current instanceof StreamReadException) {
                return Optional.empty();
            }
            if (current instanceof JacksonException jacksonException) {
                Optional<String> field = lastPropertyName(jacksonException);
                if (field.isPresent()) {
                    return fieldError(field.get());
                }
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private Optional<String> lastPropertyName(JacksonException exception) {
        List<Reference> path = exception.getPath();
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(path.get(path.size() - 1).getPropertyName());
    }

    private Optional<Map.Entry<String, String>> fieldError(String field) {
        return switch (field) {
            case "type" -> Optional.of(Map.entry("type", "El tipo de entrenamiento no es válido"));
            case "durationMinutes" -> Optional.of(
                    Map.entry("durationMinutes", "La duración debe ser un número entero de minutos"));
            case "date" -> Optional.of(Map.entry("date", "La fecha no es válida (formato AAAA-MM-DD)"));
            default -> Optional.empty();
        };
    }

    private void logUnexpectedError(HttpServletRequest request, RuntimeException exception) {
        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI());
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && seen.add(current)) {
            log.error("{}", current.getClass().getName());
            for (StackTraceElement element : current.getStackTrace()) {
                log.error("    at {}", element);
            }
            current = current.getCause();
        }
    }
}
