package com.examly.springapp.dto.dashboard;

/** One bar, slice or point of a chart. */
public record ChartPoint(String label, long value) {
}