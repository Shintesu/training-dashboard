package com.example.trainingdashboard.exception;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.slf4j.Logger;

public final class SanitizedStackTrace {

    private SanitizedStackTrace() {
    }

    public static void log(Logger log, Throwable throwable) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && seen.add(current)) {
            log.error("{}", current.getClass().getName());
            for (StackTraceElement element : current.getStackTrace()) {
                log.error("    at {}", element);
            }
            current = current.getCause();
        }
    }
}
