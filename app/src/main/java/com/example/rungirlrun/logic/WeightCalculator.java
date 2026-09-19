package com.example.rungirlrun.logic;

import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.models.Road;
import com.example.rungirlrun.models.SafetyReport;

import java.util.List;

/**
 * Calculates dynamic safety weights for each road.
 *
 * Base road weight = physical road distance.
 * Safety reports add penalties.
 *
 * Recent serious reports add larger penalties.
 * Older reports gradually lose influence.
 * Day and night reports affect day/night routing separately.
 */
public class WeightCalculator {

    // Controls how quickly old reports lose influence
    private static final double DECAY_LAMBDA = 0.05;

    private static final long MILLIS_PER_DAY =
            86_400_000L;

    // Ignore reports older than 180 days
    private static final double MAX_REPORT_AGE_DAYS =
            180.0;


    public static void calculateWeights(
            Road road,
            List<SafetyReport> allReports) {

        double dayPenalty = 0.0;
        double nightPenalty = 0.0;


        for (SafetyReport report : allReports) {

            // Only use reports assigned to this road
            if (report.getRoadId() == null
                    || !report.getRoadId().equals(
                    road.getId())) {

                continue;
            }


            double ageDays =
                    ageInDays(
                            report.getTimestamp()
                    );


            // Ignore very old reports
            if (ageDays >
                    MAX_REPORT_AGE_DAYS) {

                continue;
            }


            double severity =
                    severityOf(
                            report.getReportType(),
                            report.isNight()
                    );


            double decay =
                    decayFactor(
                            ageDays
                    );


            double contribution =
                    severity * decay;


            // --------------------------------------------------
            // DAY / NIGHT SPLIT
            // --------------------------------------------------

            if (report.isNight()) {

                /*
                 * Night incident strongly affects
                 * night routing.
                 */
                nightPenalty +=
                        contribution;


                /*
                 * It may still indicate some general
                 * risk during the day, but less strongly.
                 */
                dayPenalty +=
                        contribution * 0.20;


            } else {

                /*
                 * Day incident strongly affects
                 * daytime routing.
                 */
                dayPenalty +=
                        contribution;


                /*
                 * Some danger remains relevant at night too.
                 */
                nightPenalty +=
                        contribution * 0.40;
            }
        }


        road.setDayWeight(
                road.getBaseWeight()
                        + dayPenalty
        );


        road.setNightWeight(
                road.getBaseWeight()
                        + nightPenalty
        );
    }


    /**
     * Safety penalty for each incident type.
     *
     * These values are intentionally larger because
     * OSM baseWeight is measured in meters.
     */
    private static double severityOf(
            ReportType type,
            boolean isNight) {

        switch (type) {

            case ROBBERY:
                return 5000.0;

            case HARASSMENT:
                return 4000.0;

            case POOR_LIGHTING:

                if (isNight) {
                    return 300.0;
                }

                return 100.0;

            case VANDALISM:
                return 200.0;

            case FLOOD:
                return 350.0;

            case CONSTRUCTION:
                return 150.0;

            case OTHER:
            default:
                return 100.0;
        }
    }


    /**
     * Calculates age of report in days.
     */
    private static double ageInDays(
            long timestamp) {

        long ageMillis =
                System.currentTimeMillis()
                        - timestamp;


        return ageMillis
                / (double) MILLIS_PER_DAY;
    }


    /**
     * Exponential decay.
     *
     * New reports count almost fully.
     * Older reports gradually fade.
     */
    private static double decayFactor(
            double ageDays) {

        return Math.exp(
                -DECAY_LAMBDA
                        * Math.max(
                        ageDays,
                        0
                )
        );
    }
}