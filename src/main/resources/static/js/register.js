import { ApiError, createWorkout, deleteWorkout, getWorkoutTypes, getWorkouts } from "./api.js";
import {
    DELETE_BUTTON_LABEL,
    DELETE_CONFIRMATION,
    DELETION_SUCCESS,
    EMPTY_WORKOUTS,
    LOADING_TEXT,
    LOAD_ERROR,
    NETWORK_ERROR,
    buildWorkoutTypeLabels,
    deleteButtonAriaLabel,
    formatDate,
    registrationSuccessMessage,
    todayIso
} from "./format.js";

const form = document.querySelector("#workout-form");
const typeSelect = document.querySelector("#type");
const durationInput = document.querySelector("#duration-minutes");
const dateInput = document.querySelector("#date");
const submitButton = document.querySelector("#register-button");
const statusZone = document.querySelector("#register-status");
const alertZone = document.querySelector("#register-alert");
const typeError = document.querySelector("#type-error");
const durationError = document.querySelector("#duration-minutes-error");
const dateError = document.querySelector("#date-error");
const listStatus = document.querySelector("#workout-list-status");
const workoutTable = document.querySelector("#workout-table");
const workoutTableBody = document.querySelector("#workout-table-body");
const emptyMessage = document.querySelector("#workout-list-empty");

let typeLabels = {};
let typesLoaded = false;

function setVisible(element, visible) {
    element.hidden = !visible;
}

function setFieldInvalid(control, invalid) {
    if (invalid) {
        control.setAttribute("aria-invalid", "true");
    } else {
        control.removeAttribute("aria-invalid");
    }
}

function clearFieldInvalidState() {
    setFieldInvalid(typeSelect, false);
    setFieldInvalid(durationInput, false);
    setFieldInvalid(dateInput, false);
}

function clearMessages() {
    statusZone.textContent = "";
    alertZone.textContent = "";
    typeError.textContent = "";
    durationError.textContent = "";
    dateError.textContent = "";
    clearFieldInvalidState();
}

function showFieldErrors(fieldErrors) {
    typeError.textContent = fieldErrors.type || "";
    durationError.textContent = fieldErrors.durationMinutes || "";
    dateError.textContent = fieldErrors.date || "";
    setFieldInvalid(typeSelect, Boolean(typeError.textContent));
    setFieldInvalid(durationInput, Boolean(durationError.textContent));
    setFieldInvalid(dateInput, Boolean(dateError.textContent));
}

async function loadWorkoutTypes() {
    typeSelect.replaceChildren();
    submitButton.disabled = true;
    try {
        const workoutTypes = await getWorkoutTypes();
        typeLabels = buildWorkoutTypeLabels(workoutTypes);
        for (const workoutType of workoutTypes) {
            const option = document.createElement("option");
            option.value = workoutType.code;
            option.textContent = workoutType.label;
            typeSelect.append(option);
        }
        typesLoaded = true;
        submitButton.disabled = false;
    } catch (error) {
        alertZone.textContent = LOAD_ERROR;
        submitButton.disabled = true;
    }
}

function buildCell(text, tagName = "td") {
    const cell = document.createElement(tagName);
    cell.textContent = text;
    return cell;
}

function buildActionsCell(workout) {
    const cell = document.createElement("td");
    const button = document.createElement("button");
    const typeLabel = typeLabels[workout.type] || workout.type;
    button.type = "button";
    button.className = "delete-button";
    button.textContent = DELETE_BUTTON_LABEL;
    button.setAttribute("aria-label", deleteButtonAriaLabel(typeLabel, workout.date));
    button.addEventListener("click", () => {
        void deleteWorkoutRow(workout.id);
    });
    cell.append(button);
    return cell;
}

function buildWorkoutRow(workout) {
    const row = document.createElement("tr");
    row.append(
        buildCell(formatDate(workout.date)),
        buildCell(typeLabels[workout.type] || workout.type),
        buildCell(String(workout.durationMinutes)),
        buildCell(String(workout.calories)),
        buildActionsCell(workout)
    );
    return row;
}

function renderWorkouts(workouts) {
    workoutTableBody.replaceChildren();
    if (workouts.length === 0) {
        setVisible(workoutTable, false);
        setVisible(emptyMessage, true);
        return;
    }
    for (const workout of workouts) {
        workoutTableBody.append(buildWorkoutRow(workout));
    }
    setVisible(workoutTable, true);
    setVisible(emptyMessage, false);
}

async function loadWorkouts() {
    listStatus.textContent = LOADING_TEXT;
    setVisible(workoutTable, false);
    setVisible(emptyMessage, false);
    try {
        const workouts = await getWorkouts();
        renderWorkouts(workouts);
        listStatus.textContent = "";
    } catch (error) {
        listStatus.textContent = "";
        alertZone.textContent = LOAD_ERROR;
    }
}

async function deleteWorkoutRow(workoutId) {
    if (!window.confirm(DELETE_CONFIRMATION)) {
        return;
    }
    clearMessages();
    try {
        await deleteWorkout(workoutId);
        statusZone.textContent = DELETION_SUCCESS;
        await loadWorkouts();
    } catch (error) {
        if (error instanceof ApiError) {
            alertZone.textContent = error.message;
            if (error.status === 404) {
                await loadWorkouts();
            }
        } else {
            alertZone.textContent = NETWORK_ERROR;
        }
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!typesLoaded) {
        return;
    }
    clearMessages();
    submitButton.disabled = true;
    const type = typeSelect.value === "" ? null : typeSelect.value;
    const rawDuration = durationInput.value.trim();
    let durationMinutes = null;
    if (rawDuration !== "") {
        const parsedDuration = Number(rawDuration);
        durationMinutes = Number.isNaN(parsedDuration) ? null : parsedDuration;
    }
    const date = dateInput.value === "" ? null : dateInput.value;
    try {
        const createdWorkout = await createWorkout({ type, durationMinutes, date });
        clearFieldInvalidState();
        statusZone.textContent = registrationSuccessMessage(createdWorkout.calories);
        durationInput.value = "";
        await loadWorkouts();
    } catch (error) {
        if (error instanceof ApiError) {
            alertZone.textContent = error.message;
            showFieldErrors(error.fieldErrors);
        } else {
            alertZone.textContent = NETWORK_ERROR;
        }
    } finally {
        submitButton.disabled = false;
    }
}

form.addEventListener("submit", (event) => {
    void handleSubmit(event);
});

dateInput.value = todayIso();
emptyMessage.textContent = EMPTY_WORKOUTS;
void loadWorkoutTypes();
void loadWorkouts();
