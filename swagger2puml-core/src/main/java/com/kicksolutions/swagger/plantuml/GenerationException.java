package com.kicksolutions.swagger.plantuml;

public class GenerationException extends RuntimeException {
    public GenerationException(final String message) {
        super(message);
    }

    public GenerationException(final Exception e) {
        super("Cannot generate the schema", e);
    }
}
