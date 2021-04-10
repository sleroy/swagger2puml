package com.kicksolutions.swagger.plantuml.vo;

/**
 * 
 * @author MSANTOSH
 *
 */
public class FieldDefinition {

	private String returnType;
	private String methodDefinition;

	public FieldDefinition(String returnType, String methodDefinition) {
		super();
		this.returnType = returnType;
		this.methodDefinition = methodDefinition;
	}

	public FieldDefinition() {
		super();
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public String getMethodDefinition() {
		return methodDefinition;
	}

	public void setMethodDefinition(String methodDefinition) {
		this.methodDefinition = methodDefinition;
	}

	@Override
	public String toString() {
		return "MethodDefinitions [returnType=" + returnType + ", methodDefinition=" + methodDefinition + "]";
	}
}