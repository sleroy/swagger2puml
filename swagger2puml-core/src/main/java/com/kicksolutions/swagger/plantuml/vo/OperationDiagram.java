package com.kicksolutions.swagger.plantuml.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationDiagram extends ClassDiagram {

    public  List<FieldDefinition> pathParams  = new ArrayList<FieldDefinition>();
    private List<FieldDefinition> formParams  = new ArrayList<FieldDefinition>();
    public  List<FieldDefinition> queryParams = new ArrayList<FieldDefinition>();
    public List<ResponseDefinition> responses   = new ArrayList<>();
    public Map<String, FieldDefinition> body    = new HashMap<>();
    public String                       parentAPI;
    public List<FieldDefinition>        headers = new ArrayList<>();

    public OperationDiagram() {
        this.setStereotype("O");
        this.setBackgroundColor("#FFFFFF");
        this.setColor("#lightblue");
    }

    public void setPathParams(final List<FieldDefinition> pathParams) {
        this.pathParams = pathParams;
    }

    public void setFormParams(final List<FieldDefinition> formParams) {
        this.formParams = formParams;
    }

    public void setQueryParams(final List<FieldDefinition> queryParams) {
        this.queryParams = queryParams;
    }

    public void setResponses(final List<ResponseDefinition> responses) {
        this.responses = responses;
    }

    public void setBody(final Map<String, FieldDefinition> body) {
        this.body = body;
    }

    public void setParentAPI(final String parentAPI) {
        this.parentAPI = parentAPI;
    }

    public void setHeaders(final List<FieldDefinition> headers) {
        this.headers = headers;
    }

    public boolean hasHeaderParams() {
        return !this.headers.isEmpty();
    }

    public boolean hasPathParams() {
        return !this.pathParams.isEmpty();
    }

    public boolean hasQueryParams() {
        return !this.queryParams.isEmpty();
    }

    public boolean hasFormParams() {
        return !this.getFormParams().isEmpty();
    }

    public boolean hasBody() {
        return !this.body.isEmpty();
    }

    public List<FieldDefinition> getFormParams() {
        return formParams;
    }
}
