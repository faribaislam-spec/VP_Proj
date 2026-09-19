package com.example.rungirlrun.logic;

import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Place this file under: app/src/test/java/com/example/rungirlrun/logic/
 * Run with: right-click file -> Run 'DijkstraPathFinderTest',
 * or `./gradlew testDebugUnitTest`
 * No emulator/device/Firebase needed - pure JVM logic test.
 */
public class DijkstraPathFinderTest {

    private Location loc(String id) {
        return new Location(id, id, 23.8, 90.4); // lat/lng irrelevant for these tests
    }

    // Manually sets both dayWeight and nightWeight without going through
    // WeightCalculator, so each test controls exact weights directly.
    private Road road(String id, Location start, Location end, double dayWeight, double nightWeight) {
        Road r = new Road(id, id, start, end, dayWeight);
        r.setDayWeight(dayWeight);
        r.setNightWeight(nightWeight);
        return r;
    }

    @Test
    public void findsShortestPath_simpleLine() {
        // A --1--> B --1--> C   (straight line, only one possible path)
        Location a = loc("A"), b = loc("B"), c = loc("C");
        Graph graph = new Graph();
        graph.addRoad(road("R1", a, b, 1.0, 1.0));
        graph.addRoad(road("R2", b, c, 1.0, 1.0));

        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "A", "C", false);

        assertTrue(result.isPathFound());
        assertEquals(3, result.getPath().size());
        assertEquals("A", result.getPath().get(0).getId());
        assertEquals("B", result.getPath().get(1).getId());
        assertEquals("C", result.getPath().get(2).getId());
        assertEquals(2.0, result.getTotalWeight(), 0.001);
    }

    @Test
    public void picksLowerWeightPath_evenWithMoreHops() {
        // Two ways from A to D:
        //   Direct:      A --10--> D                 (1 hop, weight 10 = "unsafe shortcut")
        //   Roundabout:   A --1--> B --1--> C --1--> D  (3 hops, weight 3 = "safer detour")
        Location a = loc("A"), b = loc("B"), c = loc("C"), d = loc("D");
        Graph graph = new Graph();
        graph.addRoad(road("DIRECT", a, d, 10.0, 10.0));
        graph.addRoad(road("R1", a, b, 1.0, 1.0));
        graph.addRoad(road("R2", b, c, 1.0, 1.0));
        graph.addRoad(road("R3", c, d, 1.0, 1.0));

        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "A", "D", false);

        assertTrue(result.isPathFound());
        assertEquals("Should take the longer-but-safer route",
                4, result.getPath().size()); // A, B, C, D
        assertEquals(3.0, result.getTotalWeight(), 0.001);
    }

    @Test
    public void dayAndNightWeights_produceDifferentRoutes() {
        // Direct road is fine by day but dangerous at night.
        // Detour is a bit longer but consistent day/night.
        Location a = loc("A"), b = loc("B"), c = loc("C"), d = loc("D");
        Graph graph = new Graph();
        graph.addRoad(road("DIRECT", a, d, 1.0, 20.0));   // safe by day, unsafe by night
        graph.addRoad(road("R1", a, b, 2.0, 2.0));
        graph.addRoad(road("R2", b, c, 2.0, 2.0));
        graph.addRoad(road("R3", c, d, 2.0, 2.0));

        DijkstraPathFinder.PathResult dayResult =
                DijkstraPathFinder.findShortestPath(graph, "A", "D", false);
        DijkstraPathFinder.PathResult nightResult =
                DijkstraPathFinder.findShortestPath(graph, "A", "D", true);

        // Daytime: direct route wins (1.0 < 6.0)
        assertEquals(2, dayResult.getPath().size());

        // Nighttime: detour wins (6.0 < 20.0)
        assertEquals(4, nightResult.getPath().size());
    }

    @Test
    public void noPathExists_whenGraphIsDisconnected() {
        Location a = loc("A"), b = loc("B");
        Location x = loc("X"), y = loc("Y"); // separate, unconnected island

        Graph graph = new Graph();
        graph.addRoad(road("R1", a, b, 1.0, 1.0));
        graph.addRoad(road("R2", x, y, 1.0, 1.0));

        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "A", "Y", false);

        assertFalse(result.isPathFound());
        assertTrue(result.getPath().isEmpty());
    }

    @Test
    public void roadIsBidirectional_pathWorksInReverse() {
        Location a = loc("A"), b = loc("B"), c = loc("C");
        Graph graph = new Graph();
        graph.addRoad(road("R1", a, b, 1.0, 1.0));
        graph.addRoad(road("R2", b, c, 1.0, 1.0));

        // Roads were added A->B->C, now search backwards C->A
        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "C", "A", false);

        assertTrue("Bidirectional roads should allow reverse traversal", result.isPathFound());
        assertEquals(3, result.getPath().size());
        assertEquals("C", result.getPath().get(0).getId());
        assertEquals("A", result.getPath().get(2).getId());
    }

    @Test
    public void invalidStartOrEndId_returnsNoPath() {
        Location a = loc("A"), b = loc("B");
        Graph graph = new Graph();
        graph.addRoad(road("R1", a, b, 1.0, 1.0));

        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "A", "DOES_NOT_EXIST", false);

        assertFalse(result.isPathFound());
    }

    @Test
    public void sameStartAndEnd_returnsSingleNodePathWithZeroWeight() {
        Location a = loc("A"), b = loc("B");
        Graph graph = new Graph();
        graph.addRoad(road("R1", a, b, 1.0, 1.0));

        DijkstraPathFinder.PathResult result =
                DijkstraPathFinder.findShortestPath(graph, "A", "A", false);

        assertTrue(result.isPathFound());
        assertEquals(1, result.getPath().size());
        assertEquals(0.0, result.getTotalWeight(), 0.001);
    }
}