package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.GenerationMode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import net.sourceforge.plantuml.OptionFlags;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * @param mode
     */
    public void transformSwagger2Puml(@NotNull String specFile, @NotNull String output,
                                      boolean includeCardinality,
                                      boolean generateSvg, final GenerationMode mode) {
        LOGGER.entering(LOGGER.getName(), "transformSwagger2Puml");

        try {
            File swaggerSpecFile = new File(specFile);
            File targetLocation  = new File(output);

            final boolean isValidSwaggerPath = swaggerSpecFile.exists() && !swaggerSpecFile.isDirectory();
            final boolean isValidOutputPath  = targetLocation.exists() && targetLocation.isDirectory();
            if (isValidSwaggerPath && isValidOutputPath) {

                final String absolutePath = swaggerSpecFile.getAbsolutePath();
                final String readString;
                readString = Files.readString(Paths.get(absolutePath));
                SwaggerParseResult parsingResult = new OpenAPIParser().readContents(readString, null, null);

                // the parsed POJO
                OpenAPI openAPI = parsingResult.getOpenAPI();
                if (parsingResult.getMessages() != null) {
                    parsingResult.getMessages().forEach(System.err::println); // validation errors and warnings
                }
                if (openAPI == null) throw new GenerationException("Cannot read the Swagger");
                PlantUMLCodegen codegen = new PlantUMLCodegen(openAPI,
                                                              targetLocation,
                                                              includeCardinality,
                                                              mode);
                String pumlPath = null;

                try {
                    LOGGER.info("Processing File --> " + specFile);
                    pumlPath = codegen.generatePuml();
                    LOGGER.info("Successfully Create PUML !!!");

                    if (generateSvg) {
                        generateUMLDiagram(pumlPath, targetLocation);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenerationException(e);
                }
            } else {
                if (!isValidSwaggerPath) {
                    throw new GenerationException("Invalid Swagger path");
                }
                if (!isValidOutputPath) {
                    throw new GenerationException("Invalid Output path");
                }
            }
        } catch (IOException e) {
            throw new GenerationException(e);
        }


        LOGGER.exiting(LOGGER.getName(), "transformSwagger2Puml");
    }

    /**
     * @param pumlLocation
     * @param targetLocation
     * @throws IOException
     * @throws InterruptedException
     */
    private void generateUMLDiagram(String pumlLocation, @NotNull File targetLocation) throws IOException, InterruptedException {
        OptionFlags.getInstance().setSystemExit(false);
        System.out.println("Generation PNG");
        net.sourceforge.plantuml.Run.main(new String[]{"-tpng", "-o", targetLocation.getAbsolutePath(), pumlLocation });
        System.out.println("Generation SVG");
        net.sourceforge.plantuml.Run.main(new String[]{"-tsvg", "-o", targetLocation.getAbsolutePath(), pumlLocation});

    }
}