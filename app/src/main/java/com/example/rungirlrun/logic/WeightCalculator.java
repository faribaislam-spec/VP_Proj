package com.example.rungirlrun.logic;

import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.models.Road;
import com.example.rungirlrun.models.SafetyReport;

import java.util.List;

/**
 * Pure calculation class - takes a Road and its related SafetyReports,
 * and computes updated dayWeight / nightWeight for that Road.
 *
 * No Firebase or Maps dependency here on purpose:
 * - Firebase repository layer fetches List<SafetyReport> and calls this class.
 * - Map layer (OSM / Google Maps) just reads road.getDayWeight()/getNightWeight()
 *   to decide route styling or Dijkstra path cost.
 */
public class WeightCalculator {

    // How fast old reports lose influence. Higher = faster decay.
    private static final double DECAY_LAMBDA = 0.05;
    private static final long MILLIS_PER_DAY = 86_400_000L;

    // Optional: ignore reports older than this many days entirely
    private static final double MAX_REPORT_AGE_DAYS = 180;

    /**
     * Recalculates and sets dayWeight/nightWeight on the given Road,
     * based on all reports belonging to that road.
     */
    public static void calculateWeights(Road road, List<SafetyReport> allReports) {
        double dayPenalty = 0.0;
        double nightPenalty = 0.0;

        for (SafetyReport report : allReports) {
            if (report.getRoadId() == null || !report.getRoadId().equals(road.getId())) {
                continue; // not relevant to this road
            }

            double ageDays = ageInDays(report.getTimestamp());
            if (ageDays > MAX_REPORT_AGE_DAYS) {
                continue; // too old, ignore
            }

            double severity = severityOf(report.getReportType(), report.isNight());
            double decay = decayFactor(ageDays);
            double contribution = severity * decay;

            if (report.isNight()) {
                nightPenalty += contribution;
            } else {
                dayPenalty += contribution;
            }
        }

        road.setDayWeight(road.getBaseWeight() + dayPenalty);
        road.setNightWeight(road.getBaseWeight() + nightPenalty);
    }

    /**
     * Base danger score per report type.
     * isNight lets a type (like poor lighting) matter more after dark.
     */
    private static double severityOf(ReportType type, boolean isNight) {
        switch (type) {
            case ROBBERY:
                return 10.0;
            case HARASSMENT:
                return 8.0;
            case POOR_LIGHTING:
                return isNight ? 7.0 : 3.0;
            case VANDALISM:
                return 5.0;
            case FLOOD:
                return 4.0;
            case CONSTRUCTION:
                return 3.0;
            case OTHER:
            default:
                return 2.0;
        }
    }

    private static double ageInDays(long timestamp) {
        long ageMillis = System.currentTimeMillis() - timestamp;
        return ageMillis / (double) MILLIS_PER_DAY;
    }

    /**
     * Exponential decay: recent reports count almost fully,
     * older ones fade toward zero influence.
     */
    private static double decayFactor(double ageDays) {
        return Math.exp(-DECAY_LAMBDA * Math.max(ageDays, 0));
    }
}