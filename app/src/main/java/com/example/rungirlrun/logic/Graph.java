package com.example.rungirlrun.logic;

import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph representation built from Location nodes and Road edges.
 * Roads are treated as bidirectional (walkable both ways).
 *
 * No Firebase or Maps dependency - this is fed plain Location/Road
 * objects (e.g. from a DAO layer) and just exposes traversal.
 */
public class Graph {

    private final Map<String, Location> locations = new HashMap<>();

    // locationId -> list of roads connected to that location (both directions)
    private final Map<String, List<Road>> adjacency = new HashMap<>();

    public void addLocation(Location location) {
        locations.put(location.getId(), location);
        adjacency.putIfAbsent(location.getId(), new ArrayList<>());
    }

    /**
     * Adds a road and registers it on BOTH endpoints,
     * since roads are bidirectional by default.
     */
    public void addRoad(Road road) {
        String startId = road.getStart().getId();
        String endId = road.getEnd().getId();

        // Make sure both locations exist in the graph
        if (!locations.containsKey(startId)) {
            addLocation(road.getStart());
        }
        if (!locations.containsKey(endId)) {
            addLocation(road.getEnd());
        }

        adjacency.get(startId).add(road);
        adjacency.get(endId).add(road);
    }

    public Location getLocation(String locationId) {
        return locations.get(locationId);
    }

    public List<Location> getAllLocations() {
        return new ArrayList<>(locations.values());
    }

    /**
     * Returns all roads connected to a given location (in either direction).
     */
    public List<Road> getConnectedRoads(String locationId) {
        List<Road> roads = adjacency.get(locationId);
        return roads != null ? roads : new ArrayList<>();
    }

    /**
     * Given a road and one of its endpoints, returns the OTHER endpoint.
     * Useful when walking the graph, since a Road doesn't inherently
     * know which direction you're traveling.
     */
    public Location getOtherEndpoint(Road road, String fromLocationId) {
        if (road.getStart().getId().equals(fromLocationId)) {
            return road.getEnd();
        } else if (road.getEnd().getId().equals(fromLocationId)) {
            return road.getStart();
        }
        return null; // fromLocationId isn't actually part of this road
    }

    public boolean containsLocation(String locationId) {
        return locations.containsKey(locationId);
    }
}