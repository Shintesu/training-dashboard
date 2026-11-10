import { getWeeklyComparison } from "./api.js";
import {
    COLUMN_LABELS,
    LOADING_TEXT,
    LOAD_ERROR,
    METRIC_LABELS,
    differenceClass,
    formatDifference,
    formatWeekRange
} from "./format.js";

const metricHeader = document.querySelector("#metric-header");
const currentWeekLabel = document.querySelector("#current-week-label");
const currentWeekRange = document.querySelector("#current-week-range");
const previousWeekLabel = document.querySelector("#previous-week-label");
const previousWeekRange = document.querySelector("#previous-week-range");
const differenceHeader = document.querySelector("#difference-header");
const comparisonBody = document.querySelector("#comparison-body");
const statusZone = document.querySelector("#comparison-status");
const alertZone = document.querySelector("#comparison-alert");

function setColumnLabels() {
    metricHeader.textContent = COLUMN_LABELS.metric;
    currentWeekLabel.textContent = COLUMN_LABELS.currentWeek;
    previousWeekLabel.textContent = COLUMN_LABELS.previousWeek;
    differenceHeader.textContent = COLUMN_LABELS.difference;
}

function buildCell(text, tagName = "td") {
    const cell = document.createElement(tagName);
    cell.textContent = text;
    return cell;
}

function buildDifferenceCell(value) {
    const cell = document.createElement("td");
    const indicator = document.createElement("span");
    indicator.className = `difference ${differenceClass(value)}`;
    indicator.textContent = formatDifference(value);
    cell.append(indicator);
    return cell;
}

function buildComparisonRow(label, currentValue, previousValue, differenceValue) {
    const row = document.createElement("tr");
    row.append(
        buildCell(label, "th"),
        buildCell(String(currentValue)),
        buildCell(String(previousValue)),
        buildDifferenceCell(differenceValue)
    );
    row.firstElementChild.scope = "row";
    return row;
}

function renderComparison(comparison) {
    currentWeekRange.textContent = formatWeekRange(
        comparison.currentWeek.startDate,
        comparison.currentWeek.endDate
    );
    previousWeekRange.textContent = formatWeekRange(
        comparison.previousWeek.startDate,
        comparison.previousWeek.endDate
    );
    const rows = [
        [
            METRIC_LABELS.workouts,
            comparison.currentWeek.totalWorkouts,
            comparison.previousWeek.totalWorkouts,
            comparison.difference.workouts
        ],
        [
            METRIC_LABELS.durationMinutes,
            comparison.currentWeek.totalDurationMinutes,
            comparison.previousWeek.totalDurationMinutes,
            comparison.difference.durationMinutes
        ],
        [
            METRIC_LABELS.calories,
            comparison.currentWeek.totalCalories,
            comparison.previousWeek.totalCalories,
            comparison.difference.calories
        ]
    ];
    comparisonBody.replaceChildren();
    for (const [label, currentValue, previousValue, differenceValue] of rows) {
        comparisonBody.append(
            buildComparisonRow(label, currentValue, previousValue, differenceValue)
        );
    }
}

async function loadComparison() {
    setColumnLabels();
    statusZone.textContent = LOADING_TEXT;
    alertZone.textContent = "";
    try {
        const comparison = await getWeeklyComparison();
        renderComparison(comparison);
        statusZone.textContent = "";
    } catch (error) {
        statusZone.textContent = "";
        alertZone.textContent = LOAD_ERROR;
    }
}

void loadComparison();
