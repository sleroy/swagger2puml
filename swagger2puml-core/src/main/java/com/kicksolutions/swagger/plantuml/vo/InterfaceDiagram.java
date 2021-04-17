package com.kicksolutions.swagger.plantuml.vo;

import com.kicksolutions.swagger.graph.Entity;

import java.util.List;

/**
 * @author MSANTOSH
 */
public class InterfaceDiagram extends Entity {

    private List<MethodDefinitions> methods;
    private String                  errorClass;


    public InterfaceDiagram(String dotID,
                            String interfaceName,
                            List<MethodDefinitions> methods,
                            String errorClass) {
        super(dotID, interfaceName);

        this.methods = methods;
        this.errorClass = errorClass;
    }

    public InterfaceDiagram(final String dotID, String interfaceName) {
        super(dotID, interfaceName);
    }

    public List<MethodDefinitions> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodDefinitions> methods) {
        this.methods = methods;
    }


    public String getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(String errorClass) {
        this.errorClass = errorClass;
    }


}
