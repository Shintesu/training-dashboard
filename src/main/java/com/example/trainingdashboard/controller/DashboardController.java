package com.example.trainingdashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trainingdashboard.dto.WeeklyComparisonResponse;
import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/weekly")
    public WeeklyMetricsResponse weeklyMetrics() {
        return dashboardService.weeklyMetrics();
    }

    @GetMapping("/comparison")
    public WeeklyComparisonResponse weeklyComparison() {
        return dashboardService.weeklyComparison();
    }
}
