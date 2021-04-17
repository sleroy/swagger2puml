package com.kicksolutions.swagger.graph;

import com.kicksolutions.swagger.plantuml.vo.ClassMembers;

import java.util.List;

public class DomainEntity extends Entity {
    public String              description;
    public List<ClassMembers>  classMembers;
    public List<Relationship> childClasses;
    public String superClass;

    public DomainEntity(final String dotId, final String name) {
        super(dotId, name);
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

    public List<ClassMembers> getClassMembers() {
        return classMembers;
    }

    public void setClassMembers(final List<ClassMembers> classMembers) {
        this.classMembers = classMembers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
