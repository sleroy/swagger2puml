package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import net.sourceforge.plantuml.OptionFlags;

/**
 * MSANTOSH
 */
public class PlantUMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(PlantUMLGenerator.class.getName());

    public PlantUMLGenerator() {
        super();
    }

    /**
     * @param specFile
     * @param output
     */
    public void transformSwagger2Puml(String specFile, String output, boolean generateDefinitionModelOnly, boolean includeCardinality,
                                      boolean generateSvg) {
        LOGGER.entering(LOGGER.getName(), "transformSwagger2Puml");

        File swaggerSpecFile = new File(specFile);
        File targetLocation  = new File(output);

        final boolean isValidSwaggerPath = swaggerSpecFile.exists() && !swaggerSpecFile.isDirectory();
        final boolean isValidOutputPath  = targetLocation.exists() && targetLocation.isDirectory();
        if (isValidSwaggerPath && isValidOutputPath) {

            Swagger swaggerObject = new SwaggerParser().read(swaggerSpecFile.getAbsolutePath());
            PlantUMLCodegen codegen = new PlantUMLCodegen(swaggerObject, targetLocation, generateDefinitionModelOnly,
                                                          includeCardinality);
            String pumlPath = null;

            try {
                LOGGER.info("Processing File --> " + specFile);
                pumlPath = codegen.generatePuml();
                LOGGER.info("Sucessfully Create PUML !!!");

                if (generateSvg) {
                    generateUMLDiagram(pumlPath, targetLocation);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } else {
            if (!isValidSwaggerPath) {
                throw new RuntimeException("Invalid Swagger path");
            }
            if (!isValidOutputPath) {
                throw new RuntimeException("Invalid Output path");
            }
        }

        LOGGER.exiting(LOGGER.getName(), "transformSwagger2Puml");
    }

    /**
     * @param pumlLocation
     * @param targetLocation
     * @throws IOException
     * @throws InterruptedException
     */
    private void generateUMLDiagram(String pumlLocation, File targetLocation) throws IOException, InterruptedException {
        OptionFlags.getInstance().setSystemExit(false);
        System.out.println("Generation PNG");
        net.sourceforge.plantuml.Run.main(new String[]{"-tpng", "-o", targetLocation.getAbsolutePath(), "-I", pumlLocation});
        System.out.println("Generation SVG");
        net.sourceforge.plantuml.Run.main(new String[]{ "-tsvg", "-o", targetLocation.getAbsolutePath(), "-I", pumlLocation});

    }
}