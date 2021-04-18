package com.kicksolutions.swagger.plantuml.vo;

import java.util.*;
import java.util.Map.Entry;

public class OperationDiagram extends ClassDiagram {

    public  List<FieldDefinition>        pathParams  = new ArrayList<FieldDefinition>();
    public  List<FieldDefinition>        queryParams = new ArrayList<FieldDefinition>();
    public  List<ResponseDefinition>     responses   = new ArrayList<>();
    public  Map<String, FieldDefinition> body        = new HashMap<>();
    public  String                       parentAPI;
    public  List<FieldDefinition>        headers     = new ArrayList<>();
    private List<FieldDefinition>        formParams  = new ArrayList<FieldDefinition>();

    public OperationDiagram() {
        this.setStereotype("O");
        this.setBackgroundColor("#FFFFFF");
        this.setColor("#lightblue");
    }

    public List<FieldDefinition> getPathParams() {
        return pathParams;
    }

    public void setPathParams(final List<FieldDefinition> pathParams) {
        this.pathParams = pathParams;
    }

    public List<FieldDefinition> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(final List<FieldDefinition> queryParams) {
        this.queryParams = queryParams;
    }

    public List<ResponseDefinition> getResponses() {
        return responses;
    }

    public void setResponses(final List<ResponseDefinition> responses) {
        this.responses = responses;
    }

    public Map<String, FieldDefinition> getBody() {
        return body;
    }

    public void setBody(final Map<String, FieldDefinition> body) {
        this.body = body;
    }

    public Set<Entry<String, FieldDefinition>> bodyDefinitions() {
        return body.entrySet();
    }

    public String getParentAPI() {
        return parentAPI;
    }

    public void setParentAPI(final String parentAPI) {
        this.parentAPI = parentAPI;
    }

    public List<FieldDefinition> getHeaders() {
        return headers;
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

    public void setFormParams(final List<FieldDefinition> formParams) {
        this.formParams = formParams;
    }
}
