package com.kicksolutions.swagger.plantuml;


import com.kicksolutions.swagger.GenerationMode;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@Ignore
public class PlantUMLGeneratorTest {

    @Test
    public void testDiagramFull() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph/full");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.yaml", file.getPath(), false, true, true, GenerationMode.full);
    }

    @Test
    public void testDiagramAPI() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph/api");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.yaml", file.getPath(), false, true, true, GenerationMode.api);
    }

    @Test
    public void testDiagramDomain() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph/domain");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.yaml", file.getPath(), false, true, true, GenerationMode.domain);
    }

    @Test
    public void test2() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph2");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.json", file.getPath(), false, true, true, GenerationMode.full);
    }

}