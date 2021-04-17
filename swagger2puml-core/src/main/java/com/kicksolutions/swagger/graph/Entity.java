package com.kicksolutions.swagger.graph;

import java.util.HashSet;
import java.util.Set;

public abstract class Entity {
    public String            dotId;
    public Set<Relationship> dependencies = new HashSet<>();

    public String name;

    public Entity(final String dotId, final String name) {
        this.dotId = dotId;
        this.name = name;
    }

    public Relationship newDependency(final String componentId) {
        final Relationship relationship = new Relationship(this.dotId, componentId);
        dependencies.add(relationship);
        return relationship;
    }
}
