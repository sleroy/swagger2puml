package com.kicksolutions.swagger.graph;

import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.plantuml.vo.MethodDefinitions;

import java.util.List;

public class ResourceEntity extends Entity {

    private final String tag;
    public        String description;
    public  List<MethodDefinitions> methods;
    private String                  errorClass;

    public ResourceEntity(final String dotId, final String tag, final String resourceName) {
        super(dotId, resourceName);
        this.tag = tag;
    }

    public List<MethodDefinitions> getMethods() {
        return methods;
    }

    public void setMethods(final List<MethodDefinitions> methods) {
        this.methods = methods;
    }

    public String getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(final String errorClass) {
        this.errorClass = errorClass;
    }

    public String getTag() {
        return tag;
    }

}
