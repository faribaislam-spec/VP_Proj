package com.example.rungirlrun.logic;

import java.util.Calendar;

/**
 * Helper for determining whether it's currently "night" for the
 * purposes of route weighting. Kept simple (fixed hour threshold)
 * for now - can later be swapped for real sunrise/sunset lookup
 * without changing anything that calls isNightTime().
 */
public class TimeUtils {

    // Night is considered 6 PM (18:00) to 6 AM
    private static final int NIGHT_START_HOUR = 18;
    private static final int NIGHT_END_HOUR = 6;

    /**
     * Auto-detects night based on the device's current time.
     */
    public static boolean isNightTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }
}