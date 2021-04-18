package com.kicksolutions.swagger.graph;

import com.kicksolutions.swagger.plantuml.vo.Field;

import java.util.List;

public class DomainEntity extends Entity {
    private String             description;
    private List<Relationship> childClasses;
    private String      superClass;
    private List<Field> fields;

    public DomainEntity(final String dotId, final String name) {
        super(dotId, name);
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(final List<Field> fields) {
        this.fields = fields;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(final String superClass) {
        this.superClass = superClass;
    }

    public List<Relationship> getChildClasses() {
        return childClasses;
    }

    public void setChildClasses(final List<Relationship> childClasses) {
        this.childClasses = childClasses;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
