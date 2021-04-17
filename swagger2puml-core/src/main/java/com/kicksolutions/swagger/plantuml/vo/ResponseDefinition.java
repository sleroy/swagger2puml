package com.kicksolutions.swagger.plantuml.vo;

/**
 * @author MSANTOSH
 */
public class ResponseDefinition {

    public  boolean required;
    private String  returnType;
    private String  fieldName;
    private String  mediaType;

    public ResponseDefinition(String returnType, String fieldName) {
        super();
        this.returnType = returnType;
        this.fieldName = fieldName;
    }

    public ResponseDefinition() {
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

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return "ResponseDefinition{" +
                "required=" + required +
                ", returnType='" + returnType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", mediaType='" + mediaType + '\'' +
                '}';
    }
}