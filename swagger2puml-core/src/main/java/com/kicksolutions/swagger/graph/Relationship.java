package com.kicksolutions.swagger.graph;

import javax.management.relation.RelationNotFoundException;
import java.util.Objects;

public class Relationship {
    public  String  from;
    public  String  to;
    public  String  relationshipName;
    private boolean isExtension;
    private boolean isComposition;
    private boolean isExtends;
    private String  cardinality;

    public Relationship() {
        super();
    }

    public Relationship(final String from, final String to) {
        this.from = from;
        this.to = to;
    }

    public Relationship(String targetClass, boolean isExtension, boolean isComposition, String cardinality, String sourceClass) {
        this.from = sourceClass;
        this.to =targetClass;
        this.isExtension = isExtension;
        this.isComposition = isComposition;
        this.cardinality = cardinality;
    }


    public String getTargetClass() {
        return to;
    }

    public void setTargetClass(String targetClass) {
        this.to = targetClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, relationshipName);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Relationship that = (Relationship) o;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(relationshipName, that.relationshipName);
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", relationshipName='" + relationshipName + '\'' +
                ", isExtension=" + isExtension +
                ", isComposition=" + isComposition +
                ", isExtends=" + isExtends +
                ", cardinality='" + cardinality + '\'' +
                '}';
    }

    public String getSourceClass() {
        return from;
    }

    public void setSourceClass(String sourceClass) {
        this.from = sourceClass;
    }

    public boolean isExtends() {
        return isExtends;
    }

    public void setExtends(final boolean anExtends) {
        isExtends = anExtends;
    }

    public boolean isExtension() {
        return isExtension;
    }

    public void setExtension(boolean isExtension) {
        this.isExtension = isExtension;
    }

    public boolean isComposition() {
        return isComposition;
    }

    public void setComposition(boolean isComposition) {
        this.isComposition = isComposition;
    }

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }
}
