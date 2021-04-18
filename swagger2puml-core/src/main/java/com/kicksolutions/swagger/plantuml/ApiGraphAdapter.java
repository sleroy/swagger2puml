package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.graph.*;
import com.kicksolutions.swagger.plantuml.vo.ClassDiagram;
import com.kicksolutions.swagger.plantuml.vo.InterfaceDiagram;
import com.kicksolutions.swagger.plantuml.vo.OperationDiagram;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiGraphAdapter implements com.kicksolutions.swagger.graph.GraphAdapter {

    private final Graph   graph;
    private final boolean domainClassGeneration;
    private final boolean apiGeneration;
    private final boolean cardinalityGeneration;

    public ApiGraphAdapter(final Graph graph, final boolean domainClassGeneration, final boolean apiGeneration,
                           final boolean cardinalityGeneration) {
        this.graph = graph;
        this.domainClassGeneration = domainClassGeneration;
        this.apiGeneration = apiGeneration;
        this.cardinalityGeneration = cardinalityGeneration;
    }

    @Override
    public void adapt(final Map<String, Object> additionalProperties) {
        additionalProperties.put("classDiagrams", generateClassDiagrams(graph));
        additionalProperties.put("graphPackages", graph.graphPackages);


        final List<InterfaceDiagram> interfaceDiagrams = graph.filter(ResourceEntity.class).stream().map(r -> {
            final InterfaceDiagram interfaceDiagram = new InterfaceDiagram();
            interfaceDiagram.setClassName(r.dotId);
            interfaceDiagram.setDescription(r.name);
            interfaceDiagram.setStereotype("R");
            interfaceDiagram.setColor("lightblue");
            interfaceDiagram.setBackgroundColor("#white-lightblue");
            interfaceDiagram.setDomain(ApiProcessGraphGeneration.API_DEFINITION);
            interfaceDiagram.setMethods(r.methods);
            return interfaceDiagram;
        }).collect(Collectors.toList());


        additionalProperties.put("interfaceDiagrams", interfaceDiagrams);


        final List<OperationDiagram> operationDiagrams = graph.filter(OperationEntity.class).stream().map(op -> {
            final OperationDiagram operationDiagram = new OperationDiagram();
            operationDiagram.setClassName(op.dotId);
            operationDiagram.setDescription(op.name);
            operationDiagram.setStereotype("O");
            operationDiagram.setColor("lightgreen");
            operationDiagram.setBackgroundColor("#white-lightgreen");
            operationDiagram.setDomain(ApiProcessGraphGeneration.API_DEFINITION);
            operationDiagram.setFormParams(op.formParams);
            operationDiagram.setPathParams(op.pathParams);
            operationDiagram.setQueryParams(op.queryParams);
            operationDiagram.setHeaders(new ArrayList<>(op.headers));
            operationDiagram.setResponses(op.responses);
            operationDiagram.setBody(op.body);
            return operationDiagram;
        }).collect(Collectors.toList());

        additionalProperties.put("operationDiagrams", operationDiagrams);

        // Compute all relationships
        additionalProperties.put("entityRelations", computeAllRelations(graph, apiGeneration, domainClassGeneration));

    }


    /**
     * Compute all relations
     *
     * @return
     */

    private @NotNull Set<Relationship> computeAllRelations(final Graph graph, boolean apiGeneration, boolean domainClassGeneration) {
        Set<String> entitiesId = this.graph.entityList.stream().map(e -> e.dotId).collect(Collectors.toSet());
        return graph.entityList.stream()
                               .flatMap(entity -> entity.dependencies.stream()
                                                                     .map(r -> {
                                                                         if (r.getSourceClass() == null) {
                                                                             r.from = entity.dotId;
                                                                         }
                                                                         if (r.getTargetClass() == null) {
                                                                             r.to = entity.dotId;
                                                                         }
                                                                         if (!cardinalityGeneration) {
                                                                             r.setCardinality("");
                                                                         }
                                                                         return r;
                                                                     }))
                               .filter(r -> entitiesId.contains(r.getSourceClass()) && entitiesId.contains(r.getTargetClass()))
                               .collect(Collectors.toSet());
    }

    public List<ClassDiagram> generateClassDiagrams(final Graph graph) {

        final List<ClassDiagram> domainClasses = graph.filter(DomainEntity.class).stream().map(r -> {
            final ClassDiagram classDiagram = new ClassDiagram();
            classDiagram.setClassName(r.dotId);
            classDiagram.setDescription(r.name);
            classDiagram.setChildClass(r.getChildClasses());
            classDiagram.setSuperClass(r.getSuperClass());
            classDiagram.setFields(r.getFields());
            classDiagram.setStereotype("C");
            classDiagram.setColor("lightyellow");
            classDiagram.setBackgroundColor("#white-lightyellow");
            classDiagram.setDomain(NamingUtils.DOMAIN);
            return classDiagram;
        }).collect(Collectors.toList());

        final List<ClassDiagram> collect = new ArrayList<>();
        collect.addAll(domainClasses);
        return collect;
    }
}
