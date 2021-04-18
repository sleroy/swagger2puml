package com.kicksolutions.swagger.graph;

import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.plantuml.vo.FieldDefinition;
import com.kicksolutions.swagger.plantuml.vo.ResponseDefinition;

import java.util.*;

/**
 * @author sleroy
 */
public class OperationEntity extends Entity {

    public final List<FieldDefinition>        pathParams  = new ArrayList<FieldDefinition>();
    public final List<FieldDefinition>        formParams  = new ArrayList<FieldDefinition>();
    public final List<FieldDefinition>        queryParams = new ArrayList<FieldDefinition>();
    public final List<ResponseDefinition>     responses   = new ArrayList<>();
    public       Map<String, FieldDefinition> body        = new HashMap<>();

    public String               parentAPI;
    public Set<FieldDefinition> headers = new HashSet<>();

    public OperationEntity(final String dotId, String name) {
        super(dotId, name);
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
        return !this.formParams.isEmpty();
    }

    public boolean hasBody() {
        return !this.body.isEmpty();
    }
}
