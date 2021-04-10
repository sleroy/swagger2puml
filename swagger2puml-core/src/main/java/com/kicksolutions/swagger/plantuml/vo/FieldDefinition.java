package com.kicksolutions.swagger.plantuml.vo;

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

    @Override
    public String toString() {
        return "FieldDefinition{" +
                "returnType='" + returnType + '\'' +
                ", methodDefinition='" + fieldName + '\'' +
                '}';
    }
}