package com.example.rungirlrun.logic;

import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Runs Dijkstra's algorithm over a Graph, using either dayWeight or
 * nightWeight on each Road depending on isNight.
 *
 * No Firebase or Maps dependency here - takes a Graph (already built
 * from Location/Road objects) and returns a plain result object.
 * The map layer (OSM / Google Maps) is responsible for drawing the
 * returned list of Locations as a route.
 */
public class DijkstraPathFinder {

    /**
     * Result of a pathfinding run.
     */
    public static class PathResult {
        private final List<Location> path;
        private final double totalWeight;
        private final boolean pathFound;

        public PathResult(List<Location> path, double totalWeight, boolean pathFound) {
            this.path = path;
            this.totalWeight = totalWeight;
            this.pathFound = pathFound;
        }

        public List<Location> getPath() {
            return path;
        }

        public double getTotalWeight() {
            return totalWeight;
        }

        public boolean isPathFound() {
            return pathFound;
        }
    }

    /**
     * Finds the safest (lowest-weight) path between two locations.
     *
     * @param graph        the graph to search
     * @param startId      id of the starting Location
     * @param endId        id of the destination Location
     * @param isNight      whether to use nightWeight (true) or dayWeight (false)
     */
    public static PathResult findShortestPath(Graph graph, String startId, String endId, boolean isNight) {
        if (!graph.containsLocation(startId) || !graph.containsLocation(endId)) {
            return new PathResult(new ArrayList<>(), 0, false);
        }

        // Shortest known distance to each location
        Map<String, Double> distances = new HashMap<>();
        // Tracks the previous location + road used to reach each location (for path reconstruction)
        Map<String, String> previousLocation = new HashMap<>();

        for (Location location : graph.getAllLocations()) {
            distances.put(location.getId(), Double.MAX_VALUE);
        }
        distances.put(startId, 0.0);

        PriorityQueue<String> queue = new PriorityQueue<>(
                (a, b) -> Double.compare(distances.get(a), distances.get(b))
        );
        queue.add(startId);

        java.util.Set<String> visited = new java.util.HashSet<>();

        while (!queue.isEmpty()) {
            String currentId = queue.poll();

            if (visited.contains(currentId)) {
                continue;
            }
            visited.add(currentId);

            if (currentId.equals(endId)) {
                break; // reached destination, can stop early
            }

            for (Road road : graph.getConnectedRoads(currentId)) {
                Location neighbor = graph.getOtherEndpoint(road, currentId);
                if (neighbor == null || visited.contains(neighbor.getId())) {
                    continue;
                }

                double edgeWeight = isNight ? road.getNightWeight() : road.getDayWeight();
                double newDistance = distances.get(currentId) + edgeWeight;

                if (newDistance < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), newDistance);
                    previousLocation.put(neighbor.getId(), currentId);
                    queue.add(neighbor.getId());
                }
            }
        }

        if (distances.get(endId) == Double.MAX_VALUE) {
            return new PathResult(new ArrayList<>(), 0, false); // no path exists
        }

        // Reconstruct path by walking backwards from endId to startId
        List<Location> path = new ArrayList<>();
        String currentId = endId;
        while (currentId != null) {
            path.add(graph.getLocation(currentId));
            currentId = previousLocation.get(currentId);
        }
        Collections.reverse(path);

        return new PathResult(path, distances.get(endId), true);
    }
}