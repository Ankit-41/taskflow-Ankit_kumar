package com.ankit.taskflow.dto.project;

import java.util.Map;

public record ProjectStatsResponse(
        Map<String, Long> statusCounts,
        Map<String, Long> assigneeCounts) {
}

