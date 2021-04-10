package com.kicksolutions.swagger.plantuml.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sleroy
 */
public class OperationDiagram {

    public final List<FieldDefinition> pathParams     = new ArrayList<FieldDefinition>();
    public final List<FieldDefinition> formParams     = new ArrayList<FieldDefinition>();
    public final List<FieldDefinition> queryParams    = new ArrayList<FieldDefinition>();
    public final List<FieldDefinition> responses      = new ArrayList<>();
    public final List<ClassRelation>   classRelations = new ArrayList<>();
    public       FieldDefinition       body           = null;
    public       String                operationName;

    public List<ClassRelation>   dependencies;
    public String                parentAPI;
    public List<FieldDefinition> headers = new ArrayList<>();

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
        return !this.formParams.isEmpty();
    }
}
