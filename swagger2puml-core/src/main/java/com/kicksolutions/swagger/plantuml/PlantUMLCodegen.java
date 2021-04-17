/**
 *
 */
package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.kicksolutions.swagger.GenerationMode;
import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.graph.Graph;
import com.kicksolutions.swagger.graph.Relationship;
import com.kicksolutions.swagger.plantuml.vo.*;
import io.swagger.v3.oas.models.OpenAPI;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.jetbrains.annotations.NotNull;


/**
 * @author MSANTOSH
 *
 */
public class PlantUMLCodegen {

    private static final Logger         LOGGER                      = Logger.getLogger(PlantUMLCodegen.class.getName());
    private static final String         CARDINALITY_ONE_TO_MANY     = "1..*";
    private static final String         CARDINALITY_NONE_TO_MANY    = "0..*";
    private static final String         CARDINALITY_ONE_TO_ONE      = "1..1";
    private static final String         CARDINALITY_NONE_TO_ONE     = "0..1";
    private final        OpenAPI        swagger;
    private final        File           targetLocation;
    private final        GenerationMode mode;
    private              boolean        generateDefinitionModelOnly = false;
    private              boolean        includeCardinality          = true;

    /**
     *
     */
    public PlantUMLCodegen(final OpenAPI swagger,
                           final File targetLocation,
                           final boolean includeCardinality,
                           final GenerationMode mode) {
        this.swagger = swagger;
        this.targetLocation = targetLocation;
        this.generateDefinitionModelOnly = generateDefinitionModelOnly || mode == GenerationMode.domain;
        this.includeCardinality = includeCardinality;
        this.mode = mode;
    }

    /**
     *
     */
    public @NotNull String generatePuml() throws IOException, IllegalAccessException {
        LOGGER.entering(LOGGER.getName(), "generatePuml");

        Map<String, Object> additionalProperties = preprocessSwagger(swagger);

        MustacheFactory mf       = new DefaultMustacheFactory();
        Mustache        mustache = mf.compile("puml.mustache");
        Writer          writer   = null;
        String pumlPath = targetLocation.getAbsolutePath() + File.separator +
                "swagger.puml";
        try {
            writer = new FileWriter(pumlPath);
            mustache.execute(writer, additionalProperties);

            LOGGER.log(Level.FINEST, "Successfully Written Puml File @ " + pumlPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalAccessException(e.getMessage());
        } finally {
            if (writer != null) {
                writer.flush();
            }
        }

        LOGGER.exiting(LOGGER.getName(), "generatePuml");
        return pumlPath;
    }

    /**
     *
     * @param swagger
     */
    private @NotNull Map<String, Object> preprocessSwagger(@NotNull OpenAPI swagger) {
        LOGGER.entering(LOGGER.getName(), "preprocessSwagger");
        final boolean apiGeneration         = mode == GenerationMode.api || mode == GenerationMode.full;
        final boolean domainClassGeneration = mode == GenerationMode.full || mode == GenerationMode.domain;

        Map<String, Object> additionalProperties = new TreeMap<>();

        additionalProperties.put("title", swagger.getInfo().getTitle());
        additionalProperties.put("version", swagger.getInfo().getVersion());

        final ApiProcessGraphGeneration apiProcessGraphGeneration = new ApiProcessGraphGeneration(domainClassGeneration, apiGeneration);
        Graph                           graph                     = apiProcessGraphGeneration.process(swagger);

        List<ClassDiagram> classDiagrams = apiProcessGraphGeneration.generateClassDiagrams(graph);

        apiProcessGraphGeneration.getGraphAdapter().adapt(additionalProperties, graph);

        LOGGER.exiting(LOGGER.getName(), "preprocessSwagger");

        return additionalProperties;
    }


    private List<Relationship> getAlLOperationsRelations(final List<Entity> operationDiagrams) {
        List<Relationship> modelRelations = new ArrayList<>();

        for (Entity classDiagram : operationDiagrams) {
            Set<Relationship> classRelations = classDiagram.dependencies;

            for (Relationship classRelation : classRelations) {
                if (classRelation.from == null) {
                    classRelation.from = (classDiagram.dotId);
                }
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }

    /**
     *
     * @param classDiagrams
     * @return
     */
    private @NotNull List<Relationship> getAllModelRelations(List<Entity> classDiagrams) {
        List<Relationship> modelRelations = new ArrayList<>();

        for (Entity classDiagram : classDiagrams) {
            Set<Relationship> classRelations = classDiagram.dependencies;

            for (Relationship classRelation : classRelations) {
                classRelation.from = classDiagram.dotId;
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }


    private List<Relationship> getAllInterfacesRelations(List<Entity> interfaceDiagrams) {
        List<Relationship> modelRelations = new ArrayList<>();

        for (Entity entity : interfaceDiagrams) {
            InterfaceDiagram  interfaceDiagram = (InterfaceDiagram) entity;
            Set<Relationship> classRelations   = interfaceDiagram.dependencies;

            for (Relationship classRelation : classRelations) {
                classRelation.from = (interfaceDiagram.dotId);
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }


}