const WORKOUTS_PATH = "/api/workouts";
const WORKOUT_TYPES_PATH = "/api/workout-types";
const WEEKLY_METRICS_PATH = "/api/dashboard/weekly";
const WEEKLY_COMPARISON_PATH = "/api/dashboard/comparison";

export class ApiError extends Error {
    constructor(status, message, fieldErrors) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

export class NetworkError extends Error {
    constructor() {
        super("Network request failed");
        this.name = "NetworkError";
    }
}

async function requestJson(path, options) {
    let response;
    try {
        response = await fetch(path, options);
    } catch (error) {
        throw new NetworkError();
    }
    if (!response.ok) {
        throw await readApiError(response);
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

async function readApiError(response) {
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new NetworkError();
    }
    if (!isErrorResponse(payload)) {
        throw new NetworkError();
    }
    return new ApiError(response.status, payload.message, payload.fieldErrors);
}

function isErrorResponse(payload) {
    return payload !== null
        && typeof payload === "object"
        && typeof payload.message === "string"
        && payload.fieldErrors !== null
        && typeof payload.fieldErrors === "object"
        && !Array.isArray(payload.fieldErrors);
}

export async function getWorkoutTypes() {
    return requestJson(WORKOUT_TYPES_PATH);
}

export async function getWorkouts() {
    return requestJson(WORKOUTS_PATH);
}

export async function createWorkout(workout) {
    return requestJson(WORKOUTS_PATH, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(workout)
    });
}

export async function deleteWorkout(id) {
    return requestJson(`${WORKOUTS_PATH}/${id}`, {
        method: "DELETE"
    });
}

export async function getWeeklyMetrics() {
    return requestJson(WEEKLY_METRICS_PATH);
}

export async function getWeeklyComparison() {
    return requestJson(WEEKLY_COMPARISON_PATH);
}
