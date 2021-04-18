package com.kicksolutions.swagger.plantuml.vo;

import java.util.Objects;

/**
 * @author MSANTOSH
 */
public class FieldDefinition {

    public  boolean required;
    private String  returnType;
    private String  fieldName;

    public FieldDefinition(String returnType, String fieldName) {
        super();
        this.returnType = returnType;
        this.fieldName = fieldName;
    }

    public FieldDefinition() {
        super();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FieldDefinition that = (FieldDefinition) o;
        return Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public String toString() {
        return "FieldDefinition{" +
                "returnType='" + returnType + '\'' +
                ", methodDefinition='" + fieldName + '\'' +
                '}';
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}