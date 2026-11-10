import { getWeeklyMetrics } from "./api.js";
import { LOADING_TEXT, LOAD_ERROR, formatWeekRange } from "./format.js";

const weekRange = document.querySelector("#week-range");
const totalWorkouts = document.querySelector("#total-workouts");
const totalDurationMinutes = document.querySelector("#total-duration-minutes");
const totalCalories = document.querySelector("#total-calories");
const statusZone = document.querySelector("#dashboard-status");
const alertZone = document.querySelector("#dashboard-alert");

async function loadDashboard() {
    statusZone.textContent = LOADING_TEXT;
    alertZone.textContent = "";
    try {
        const metrics = await getWeeklyMetrics();
        weekRange.textContent = `Semana del ${formatWeekRange(metrics.startDate, metrics.endDate)}`;
        totalWorkouts.textContent = String(metrics.totalWorkouts);
        totalDurationMinutes.textContent = String(metrics.totalDurationMinutes);
        totalCalories.textContent = String(metrics.totalCalories);
        statusZone.textContent = "";
    } catch (error) {
        statusZone.textContent = "";
        alertZone.textContent = LOAD_ERROR;
    }
}

void loadDashboard();
