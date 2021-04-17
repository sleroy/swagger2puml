package com.kicksolutions.swagger.graph;

public class GraphPackage {
    private final String name;
    private final String color;

    public GraphPackage(final String name, final String color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String toString() {
        return "GraphPackage{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }
}
