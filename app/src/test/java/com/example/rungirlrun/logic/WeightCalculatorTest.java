package com.example.rungirlrun.logic;

import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;
import com.example.rungirlrun.models.SafetyReport;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Place this file under: app/src/test/java/com/example/rungirlrun/logic/
 * Run with: right-click file -> Run, or `./gradlew testDebugUnitTest`
 * No emulator/device needed - this is pure JVM logic.
 */
public class WeightCalculatorTest {

    private Road makeRoad(double baseWeight) {
        Location start = new Location("L1", "Start", 23.8, 90.4);
        Location end = new Location("L2", "End", 23.81, 90.41);
        return new Road("R1", "Test Road", start, end, baseWeight);
    }

    @Test
    public void noReports_weightsStayAtBase() {
        Road road = makeRoad(1.0);
        WeightCalculator.calculateWeights(road, new ArrayList<>());

        assertEquals(1.0, road.getDayWeight(), 0.001);
        assertEquals(1.0, road.getNightWeight(), 0.001);
    }

    @Test
    public void nightReport_onlyAffectsNightWeight() {
        Road road = makeRoad(1.0);
        List<SafetyReport> reports = new ArrayList<>();
        reports.add(new SafetyReport("rep1", "R1", "user1",
                ReportType.ROBBERY, true, System.currentTimeMillis()));

        WeightCalculator.calculateWeights(road, reports);

        assertEquals(1.0, road.getDayWeight(), 0.001); // unaffected
        assertTrue("Night weight should increase above base", road.getNightWeight() > 1.0);
    }

    @Test
    public void higherSeverityReport_increasesWeightMore() {
        Road roadRobbery = makeRoad(1.0);
        Road roadConstruction = makeRoad(1.0);

        List<SafetyReport> robberyReports = new ArrayList<>();
        robberyReports.add(new SafetyReport("rep1", "R1", "user1",
                ReportType.ROBBERY, false, System.currentTimeMillis()));

        List<SafetyReport> constructionReports = new ArrayList<>();
        constructionReports.add(new SafetyReport("rep2", "R1", "user1",
                ReportType.CONSTRUCTION, false, System.currentTimeMillis()));

        WeightCalculator.calculateWeights(roadRobbery, robberyReports);
        WeightCalculator.calculateWeights(roadConstruction, constructionReports);

        assertTrue("Robbery should weigh more than construction",
                roadRobbery.getDayWeight() > roadConstruction.getDayWeight());
    }

    @Test
    public void oldReport_hasLessInfluenceThanRecentReport() {
        Road recentRoad = makeRoad(1.0);
        Road oldRoad = makeRoad(1.0);

        long now = System.currentTimeMillis();
        long ninetyDaysAgo = now - (90L * 24 * 60 * 60 * 1000);

        List<SafetyReport> recentReports = new ArrayList<>();
        recentReports.add(new SafetyReport("rep1", "R1", "user1",
                ReportType.HARASSMENT, false, now));

        List<SafetyReport> oldReports = new ArrayList<>();
        oldReports.add(new SafetyReport("rep2", "R1", "user1",
                ReportType.HARASSMENT, false, ninetyDaysAgo));

        WeightCalculator.calculateWeights(recentRoad, recentReports);
        WeightCalculator.calculateWeights(oldRoad, oldReports);

        assertTrue("Recent report should weigh more than a 90-day-old report",
                recentRoad.getDayWeight() > oldRoad.getDayWeight());
    }

    @Test
    public void reportsOnDifferentRoad_areIgnored() {
        Road road = makeRoad(1.0);
        List<SafetyReport> reports = new ArrayList<>();
        reports.add(new SafetyReport("rep1", "SOME_OTHER_ROAD", "user1",
                ReportType.ROBBERY, false, System.currentTimeMillis()));

        WeightCalculator.calculateWeights(road, reports);

        assertEquals(1.0, road.getDayWeight(), 0.001); // unchanged
    }

    @Test
    public void poorLighting_weighsMoreAtNightThanDay() {
        Road nightRoad = makeRoad(1.0);
        Road dayRoad = makeRoad(1.0);
        long now = System.currentTimeMillis();

        List<SafetyReport> nightReports = new ArrayList<>();
        nightReports.add(new SafetyReport("rep1", "R1", "user1",
                ReportType.POOR_LIGHTING, true, now));

        List<SafetyReport> dayReports = new ArrayList<>();
        dayReports.add(new SafetyReport("rep2", "R1", "user1",
                ReportType.POOR_LIGHTING, false, now));

        WeightCalculator.calculateWeights(nightRoad, nightReports);
        WeightCalculator.calculateWeights(dayRoad, dayReports);

        assertTrue("Poor lighting should weigh more at night",
                nightRoad.getNightWeight() > dayRoad.getDayWeight());
    }
}