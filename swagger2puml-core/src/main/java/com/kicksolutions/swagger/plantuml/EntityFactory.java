package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.graph.Graph;
import com.kicksolutions.swagger.graph.ResourceEntity;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;

public class EntityFactory {

    private final Graph graph;

    public EntityFactory(Graph graph) {

        this.graph = graph;
    }

    public ResourceEntity newResourceEntity(final Operation operation, final String tag, HttpMethod httpMethod) {
        final String         resourceDotId  = NamingUtils.generateResourceId(tag);
        final String         resourceName   = NamingUtils.generateResourceName(tag);
        final ResourceEntity resourceEntity = graph.newResource(resourceDotId, tag, resourceName);
        return resourceEntity;

    }

}
