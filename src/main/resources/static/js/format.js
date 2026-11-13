export const LOADING_TEXT = "Cargando...";

export const REGISTRATION_SUCCESS_TEMPLATE = "Entrenamiento registrado: {calories} kcal estimadas";
export const DELETION_SUCCESS = "Entrenamiento eliminado";
export const DELETE_CONFIRMATION = "¿Eliminar este entrenamiento?";
export const EMPTY_WORKOUTS = "Todavía no hay entrenamientos registrados";
export const NETWORK_ERROR = "No se pudo conectar con el servidor. Inténtalo de nuevo más tarde";
export const LOAD_ERROR = "No se pudo cargar la información. Inténtalo de nuevo más tarde";

export const COLUMN_LABELS = {
    metric: "Métrica",
    currentWeek: "Semana actual",
    previousWeek: "Semana anterior",
    difference: "Diferencia"
};

export const METRIC_LABELS = {
    workouts: "Entrenamientos",
    durationMinutes: "Minutos totales",
    calories: "Calorías totales"
};

export const DELETE_BUTTON_LABEL = "Eliminar";

export function formatDate(isoDate) {
    if (typeof isoDate !== "string") {
        return "";
    }
    const parts = isoDate.split("-");
    if (parts.length !== 3) {
        return isoDate;
    }
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

export function todayIso() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

export function formatDifference(value) {
    if (value > 0) {
        return `▲ +${value}`;
    }
    if (value < 0) {
        return `▼ ${value}`;
    }
    return "= 0";
}

export function formatWeekRange(startDate, endDate) {
    return `${formatDate(startDate)} al ${formatDate(endDate)}`;
}

export function deleteButtonAriaLabel(typeLabel, isoDate) {
    return `Eliminar el entrenamiento de ${typeLabel} del ${formatDate(isoDate)}`;
}

export function buildWorkoutTypeLabels(workoutTypes) {
    const labels = {};
    for (const workoutType of workoutTypes) {
        labels[workoutType.code] = workoutType.label;
    }
    return labels;
}

export function registrationSuccessMessage(calories) {
    return REGISTRATION_SUCCESS_TEMPLATE.replace("{calories}", String(calories));
}

export function differenceClass(value) {
    if (value > 0) {
        return "difference-positive";
    }
    if (value < 0) {
        return "difference-negative";
    }
    return "difference-zero";
}
