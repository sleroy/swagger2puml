package com.kicksolutions.swagger.plantuml.vo;

import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.graph.Relationship;

import java.util.List;

/**
 * @author MSANTOSH
 */
public class InterfaceDiagram {

    private List<MethodDefinitions> methods;
    private String errorClass;
    private String             className;
    private boolean            isClass;
    private String             description;
    private List<Field>        fields;
    private List<Relationship> childClass;
    private String             superClass;
    private String             color           = "#FF7700";
    private String             stereotype      = "C";
    private String             backgroundColor = "#FFFFFF";
    private String             domain;
    public InterfaceDiagram(String className, String description, List<Field> fields,
                            List<Relationship> childClass, boolean isClass, String superClass) {
        super();
        this.className = className;
        this.description = description;
        this.fields = fields;
        this.childClass = childClass;
        this.isClass = isClass;
        this.superClass = superClass;
    }
    public InterfaceDiagram() {
        super();
    }

    public List<MethodDefinitions> getMethods() {
        return methods;
    }

    public void setMethods(final List<MethodDefinitions> methods) {
        this.methods = methods;
    }

    @Override
    public String toString() {
        return "InterfaceDiagram{" +
                "methods=" + methods +
                ", errorClass='" + errorClass + '\'' +
                ", className='" + className + '\'' +
                ", isClass=" + isClass +
                ", description='" + description + '\'' +
                ", fields=" + fields +
                ", childClass=" + childClass +
                ", superClass='" + superClass + '\'' +
                ", color='" + color + '\'' +
                ", stereotype='" + stereotype + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }

    public String getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(final String errorClass) {
        this.errorClass = errorClass;
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

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }
}