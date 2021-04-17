package com.kicksolutions.swagger.graph;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {

    public final List<GraphPackage> graphPackages = new ArrayList();

    public final List<Entity> entityList = new ArrayList<>();

    private final Map<String, ResourceEntity> resourceEntityMap = new HashMap<>();

    public ResourceEntity newResource(final String dotId, String tag, String resourceName) {
        return resourceEntityMap.computeIfAbsent(dotId, dotId1 -> {
            ResourceEntity r = new ResourceEntity(dotId1, tag, resourceName);
            entityList.add(r);
            return r;
        });
    }

    public <T> List<T> filter(final Class<T> resourceEntityClass) {

        return (List<T>) entityList.stream()
                                   .filter(r -> resourceEntityClass.isAssignableFrom(r.getClass()))
                                   .collect(Collectors.toList());

    }

    public void addGraphPackage(final String name, final String color) {
        graphPackages.add(new GraphPackage(name, color));
    }

    public void addEntity(final Entity entity) {
        this.entityList.add(entity);
    }
}
