package com.kicksolutions.swagger.plantuml;


import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@Ignore
public class PlantUMLGeneratorTest {

    @Test
    public void test() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.yaml", file.getPath(), false, true, true);
    }

    @Test
    public void test2() {
        final PlantUMLGenerator plantUMLGenerator = new PlantUMLGenerator();
        final File              file              = new File("target/graph2");
        file.mkdirs();
        plantUMLGenerator.transformSwagger2Puml("src/test/resources/swagger.json", file.getPath(), false, true, true);
    }

}