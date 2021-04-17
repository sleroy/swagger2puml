package com.kicksolutions.swagger.plantuml.vo;

import com.kicksolutions.swagger.graph.Relationship;

import java.util.List;

/**
 * @author MSANTOSH
 */
public class ClassDiagram {

    private String              className;
    private boolean             isClass;
    private String              description;
    private List<ClassMembers>  fields;
    private List<Relationship> childClass;
    private String              superClass;
    private String              color           = "#FF7700";
    private String              stereotype      = "C";
    private String              backgroundColor = "#FFFFFF";
    private String              domain;

    public ClassDiagram(String className, String description, List<ClassMembers> fields,
                        List<Relationship> childClass, boolean isClass, String superClass) {
        super();
        this.className = className;
        this.description = description;
        this.fields = fields;
        this.childClass = childClass;
        this.isClass = isClass;
        this.superClass = superClass;
    }

    public ClassDiagram() {
        super();
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(final String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public String getStereotype() {
        return stereotype;
    }

    public void setStereotype(final String stereotype) {
        this.stereotype = stereotype;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ClassMembers> getFields() {
        return fields;
    }

    public void setFields(List<ClassMembers> fields) {
        this.fields = fields;
    }

    public List<Relationship> getChildClass() {
        return childClass;
    }

    public void setChildClass(List<Relationship> childClass) {
        this.childClass = childClass;
    }

    public boolean isClass() {
        return isClass;
    }

    public void setClass(boolean isClass) {
        this.isClass = isClass;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return "ClassDiagram [className=" + className + ", isClass=" + isClass + ", description=" + description
                + ", fields=" + fields + ", childClass=" + childClass + ", superClass=" + superClass + "]";
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }
}