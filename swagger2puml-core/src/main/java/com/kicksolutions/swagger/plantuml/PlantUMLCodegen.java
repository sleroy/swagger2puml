/**
 *
 */
package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.kicksolutions.swagger.plantuml.vo.*;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import org.apache.commons.lang3.StringUtils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

/**
 * @author MSANTOSH
 *
 */
public class PlantUMLCodegen {

    private static final Logger  LOGGER                      = Logger.getLogger(PlantUMLCodegen.class.getName());
    private static final String  CARDINALITY_ONE_TO_MANY     = "1..*";
    private static final String  CARDINALITY_NONE_TO_MANY    = "0..*";
    private static final String  CARDINALITY_ONE_TO_ONE      = "1..1";
    private static final String  CARDINALITY_NONE_TO_ONE     = "0..1";
    private final        Swagger swagger;
    private final        File    targetLocation;
    private              boolean generateDefinitionModelOnly = false;
    private              boolean includeCardinality          = true;

    /**
     *
     */
    public PlantUMLCodegen(Swagger swagger, File targetLocation, boolean generateDefinitionModelOnly,
                           boolean includeCardinality) {
        this.swagger = swagger;
        this.targetLocation = targetLocation;
        this.generateDefinitionModelOnly = generateDefinitionModelOnly;
        this.includeCardinality = includeCardinality;
    }

    /**
     *
     */
    public String generatePuml() throws IOException, IllegalAccessException {
        LOGGER.entering(LOGGER.getName(), "generatePuml");

        Map<String, Object> additionalProperties = preprocessSwagger(swagger);

        MustacheFactory mf       = new DefaultMustacheFactory();
        Mustache        mustache = mf.compile("puml.mustache");
        Writer          writer   = null;
        String pumlPath = targetLocation.getAbsolutePath() + File.separator +
                "swagger.puml";
        try {
            writer = new FileWriter(pumlPath);
            mustache.execute(writer, additionalProperties);

            LOGGER.log(Level.FINEST, "Successfully Written Puml File @ " + pumlPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalAccessException(e.getMessage());
        } finally {
            if (writer != null) {
                writer.flush();
            }
        }

        LOGGER.exiting(LOGGER.getName(), "generatePuml");
        return pumlPath;
    }

    /**
     *
     * @param swagger
     */
    private Map<String, Object> preprocessSwagger(Swagger swagger) {
        LOGGER.entering(LOGGER.getName(), "preprocessSwagger");

        Map<String, Object> additionalProperties = new TreeMap<String, Object>();

        additionalProperties.put("title", swagger.getInfo().getTitle());
        additionalProperties.put("version", swagger.getInfo().getVersion());

        List<ClassDiagram> classDiagrams = processSwaggerModels(swagger);
        additionalProperties.put("classDiagrams", classDiagrams);

        List<InterfaceDiagram> interfaceDiagrams = new ArrayList<>();
        List<OperationDiagram> operationDiagrams = new ArrayList<>();

        if (!generateDefinitionModelOnly) {
            final DiagramModel diagramModel = processSwaggerPaths(swagger);
            interfaceDiagrams.addAll(diagramModel.interfaceDiagrams);
            operationDiagrams.addAll(diagramModel.operationDiagrams);

            additionalProperties.put("interfaceDiagrams", interfaceDiagrams);
            additionalProperties.put("operationDiagrams", operationDiagrams);

        }
        // Compute all relationships
        additionalProperties.put("entityRelations", computeAllRelations(classDiagrams, interfaceDiagrams, operationDiagrams));

        LOGGER.exiting(LOGGER.getName(), "preprocessSwagger");

        return additionalProperties;
    }

    /**
     *
     * @param classDiagrams
     * @param interfaceDiagrams
     * @return
     */
    private List<ClassRelation> computeAllRelations(List<ClassDiagram> classDiagrams,
                                                    List<InterfaceDiagram> interfaceDiagrams,
                                                    List<OperationDiagram> operationDiagrams) {
        List<ClassRelation> relations = new ArrayList<ClassRelation>();
        relations.addAll(getAllModelRelations(classDiagrams));
        relations.addAll(getAllInterfacesRelations(interfaceDiagrams));
        relations.addAll(getAlLOperationsRelations(operationDiagrams));

        return filterUnique(relations, false);
    }

    private Collection<? extends ClassRelation> getAlLOperationsRelations(final List<OperationDiagram> operationDiagrams) {
        List<ClassRelation> modelRelations = new ArrayList<ClassRelation>();

        for (OperationDiagram classDiagram : operationDiagrams) {
            List<ClassRelation> classRelations = classDiagram.dependencies;

            for (ClassRelation classRelation : classRelations) {
                if (classRelation.getSourceClass() == null) {
                    classRelation.setSourceClass(classDiagram.operationName);
                }
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }

    /**
     *
     * @param classDiagrams
     * @return
     */
    private List<ClassRelation> getAllModelRelations(List<ClassDiagram> classDiagrams) {
        List<ClassRelation> modelRelations = new ArrayList<ClassRelation>();

        for (ClassDiagram classDiagram : classDiagrams) {
            List<ClassRelation> classRelations = classDiagram.getChildClass();

            for (ClassRelation classRelation : classRelations) {
                classRelation.setSourceClass(classDiagram.getClassName());
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }


    private List<ClassRelation> getAllInterfacesRelations(List<InterfaceDiagram> interfaceDiagrams) {
        List<ClassRelation> modelRelations = new ArrayList<ClassRelation>();

        for (InterfaceDiagram classDiagram : interfaceDiagrams) {
            List<ClassRelation> classRelations = classDiagram.getChildClass();

            for (ClassRelation classRelation : classRelations) {
                classRelation.setSourceClass(classDiagram.getInterfaceName());
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }

    /**
     *
     * @param swagger
     * @return
     */
    private DiagramModel processSwaggerPaths(Swagger swagger) {
        LOGGER.entering(LOGGER.getName(), "processSwaggerPaths");

        final DiagramModel diagramModel = new DiagramModel();

        List<InterfaceDiagram> interfaceDiagrams = new ArrayList<InterfaceDiagram>();
        Map<String, Path>      paths             = swagger.getPaths();

        for (Map.Entry<String, Path> entry : paths.entrySet()) {
            Path pathObject = entry.getValue();

            LOGGER.info("Processing Path --> " + entry.getKey());

            Map<HttpMethod, Operation> operations = pathObject.getOperationMap();
            String                     uri        = entry.getKey();

            for (Entry<HttpMethod, Operation> operationEntry : operations.entrySet()) {
                diagramModel.interfaceDiagrams.add(getInterfaceDiagram(operationEntry, uri));
                diagramModel.operationDiagrams.add(getOperationInterface(operationEntry, uri));
            }
        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerPaths");
        return diagramModel;
    }

    private OperationDiagram getOperationInterface(final Entry<HttpMethod, Operation> operationEntry, final String uri) {
        LOGGER.entering(LOGGER.getName(), "getOperationInterface");
        HttpMethod method    = operationEntry.getKey();
        Operation  operation = operationEntry.getValue();

        OperationDiagram interfaceDiagram = new OperationDiagram();
        interfaceDiagram.parentAPI = getInterfaceName(operation.getTags(), operation, uri);
        String interfaceName  = getOperationName(method, operation);
        String errorClassName = getErrorClassName(operation);
        interfaceDiagram.operationName = interfaceName;
        interfaceDiagram.dependencies = (getInterfaceRelations(operation, errorClassName));


        // Attach operation to API
        ClassRelation classRelation = new ClassRelation();
        classRelation.setTargetClass(interfaceDiagram.operationName);
        classRelation.setSourceClass(interfaceDiagram.parentAPI);
        classRelation.setComposition(false);
        classRelation.setExtends(true);

        interfaceDiagram.dependencies.add(classRelation);

        for (Parameter parameter : operation.getParameters()) {
            final FieldDefinition fieldDefinition = new FieldDefinition();
            if (parameter instanceof PathParameter) {
                PathParameter pathParameter = (PathParameter) parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired();
                if (pathParameter.getType() != null) {
                    fieldDefinition.setReturnType(obtainSimpleTypeName(pathParameter.getType(), pathParameter.getFormat()));
                }
                interfaceDiagram.pathParams.add(fieldDefinition);
            } else if (parameter instanceof QueryParameter) {
                QueryParameter queryParameter = (QueryParameter) parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired();
                if (queryParameter.getType() != null) {
                    fieldDefinition.setReturnType(obtainSimpleTypeName(queryParameter.getType(), queryParameter.getFormat()));
                }
                interfaceDiagram.queryParams.add(fieldDefinition);
            } else if (parameter instanceof FormParameter) {
                FormParameter formParameter = (FormParameter) parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired();
                if (formParameter.getType() != null) {
                    fieldDefinition.setReturnType(obtainSimpleTypeName(formParameter.getType(), formParameter.getFormat()));
                }
                interfaceDiagram.formParams.add(fieldDefinition);
            }  else if (parameter instanceof HeaderParameter) {
                HeaderParameter formParameter = (HeaderParameter) parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired();
                if (formParameter.getType() != null) {
                    fieldDefinition.setReturnType(obtainSimpleTypeName(formParameter.getType(), formParameter.getFormat()));
                }
                interfaceDiagram.headers.add(fieldDefinition);
            }else {
                BodyParameter bodyParameter = (BodyParameter) parameter;
                fieldDefinition.setFieldName("payload");
                fieldDefinition.required = parameter.getRequired();
                if (bodyParameter.getSchema() != null) {
                    Model bodyParameterMdel = bodyParameter.getSchema();
                    if (bodyParameterMdel instanceof RefModel) {
                        fieldDefinition.setReturnType(((RefModel) bodyParameterMdel).getSimpleRef());
                    } else if (bodyParameterMdel instanceof ArrayModel) {
                        fieldDefinition.setReturnType(((ArrayModel) bodyParameterMdel).getType() + "[]");
                    }
                } else {
                    System.out.println(bodyParameter);
                }
                interfaceDiagram.body = fieldDefinition;
            }

        }

        for (Entry<String, Response> responseEntry : operation.getResponses().entrySet()) {
            final FieldDefinition fieldDefinition = new FieldDefinition();
            fieldDefinition.setFieldName(responseEntry.getKey());
            Property bodyParameterMdel = responseEntry.getValue().getSchema();

            String responseParameter;
            if (bodyParameterMdel instanceof RefProperty) {
                responseParameter = new StringBuilder()
                        .append(toTitleCase(((RefProperty) bodyParameterMdel).getSimpleRef()))
                        .toString();
            } else if (bodyParameterMdel instanceof StringProperty) {
                responseParameter = new StringBuilder()
                        .append(toTitleCase(bodyParameterMdel.getType()))
                        .toString();
            } else {
                responseParameter = new StringBuilder()
                        .toString();
            }
            fieldDefinition.setReturnType(responseParameter);
            interfaceDiagram.responses.add(fieldDefinition);
        }
        LOGGER.exiting(LOGGER.getName(), "getOperationInterface");
        return interfaceDiagram;

    }

    private String obtainSimpleTypeName(String type, String format) {
        return type + (format != null ? (" " + format) : "");
    }

    private String obtainType(final String type, final String s, final String format) {
        return type + s + format;
    }


    private InterfaceDiagram getInterfaceDiagram(Entry<HttpMethod, Operation> operationEntry, String uri) {
        LOGGER.entering(LOGGER.getName(), "getInterfaceDiagram");
        HttpMethod method    = operationEntry.getKey();
        Operation  operation = operationEntry.getValue();

        InterfaceDiagram interfaceDiagram = new InterfaceDiagram();
        String           interfaceName    = getInterfaceName(operation.getTags(), operation, uri);
        String           errorClassName   = getErrorClassName(operation);
        interfaceDiagram.setInterfaceName(interfaceName);
        interfaceDiagram.setErrorClass(errorClassName);
        interfaceDiagram.setMethods(getInterfaceMethods(operationEntry));
        interfaceDiagram.setChildClass(new ArrayList<ClassRelation>());
        //interfaceDiagram.setChildClass(getInterfaceRelations(operation, errorClassName));

        LOGGER.exiting(LOGGER.getName(), "getInterfaceDiagram");
        return interfaceDiagram;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelations(Operation operation, String errorClassName) {
        List<ClassRelation> relations = new ArrayList<ClassRelation>();
        relations.addAll(getInterfaceRelatedResponses(operation));
        relations.addAll(getInterfaceRelatedInputs(operation));
        if (StringUtils.isNotEmpty(errorClassName)) {
            relations.add(getErrorClass(errorClassName));
        }

        return filterUnique(relations, true);
    }

    /**
     *
     * @param relations
     * @return
     */
    private List<ClassRelation> filterUnique(List<ClassRelation> relations, boolean compareTargetOnly) {
        List<ClassRelation> uniqueList = new ArrayList<ClassRelation>();

        for (ClassRelation relation : relations) {
            if (!isTargetClassInMap(relation, uniqueList, compareTargetOnly)) {
                uniqueList.add(relation);
            }
        }

        return uniqueList;
    }

    /**
     *
     * @param relatedResponses
     * @return
     */
    private boolean isTargetClassInMap(ClassRelation sourceRelation, List<ClassRelation> relatedResponses, boolean considerTargetOnly) {
        for (ClassRelation relation : relatedResponses) {

            if (considerTargetOnly) {
                if (StringUtils.isNotEmpty(relation.getTargetClass()) && StringUtils.isNotEmpty(sourceRelation.getTargetClass())
                        && relation.getTargetClass().equalsIgnoreCase(sourceRelation.getTargetClass())) {
                    return true;
                }
            } else {
                if (StringUtils.isNotEmpty(relation.getSourceClass())
                        && StringUtils.isNotEmpty(sourceRelation.getSourceClass())
                        && StringUtils.isNotEmpty(relation.getTargetClass())
                        && StringUtils.isNotEmpty(sourceRelation.getTargetClass())
                        && relation.getSourceClass().equalsIgnoreCase(sourceRelation.getSourceClass())
                        && relation.getTargetClass().equalsIgnoreCase(sourceRelation.getTargetClass())) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     *
     * @param errorClassName
     * @return
     */
    private ClassRelation getErrorClass(String errorClassName) {
        ClassRelation classRelation = new ClassRelation();
        classRelation.setTargetClass(errorClassName);
        classRelation.setComposition(false);
        classRelation.setExtension(true);

        return classRelation;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelatedInputs(Operation operation) {
        List<ClassRelation> relatedResponses = new ArrayList<ClassRelation>();
        List<Parameter>     parameters       = operation.getParameters();

        for (Parameter parameter : parameters) {
            if (parameter instanceof BodyParameter) {
                Model bodyParameter = ((BodyParameter) parameter).getSchema();

                if (bodyParameter instanceof RefModel) {

                    ClassRelation classRelation = new ClassRelation();
                    classRelation.setTargetClass(((RefModel) bodyParameter).getSimpleRef());
                    classRelation.setComposition(false);
                    classRelation.setExtension(true);

                    relatedResponses.add(classRelation);
                } else if (bodyParameter instanceof ArrayModel) {
                    Property propertyObject = ((ArrayModel) bodyParameter).getItems();

                    if (propertyObject instanceof RefProperty) {
                        ClassRelation classRelation = new ClassRelation();
                        classRelation.setTargetClass(((RefProperty) propertyObject).getSimpleRef());
                        classRelation.setComposition(false);
                        classRelation.setExtension(true);

                        relatedResponses.add(classRelation);
                    }
                }
            }
        }

        return relatedResponses;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelatedResponses(Operation operation) {
        List<ClassRelation>   relatedResponses = new ArrayList<ClassRelation>();
        Map<String, Response> responses        = operation.getResponses();

        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    ClassRelation relation = new ClassRelation();
                    relation.setTargetClass(((RefProperty) responseProperty).getSimpleRef());
                    relation.setComposition(false);
                    relation.setExtension(true);

                    relatedResponses.add(relation);
                } else if (responseProperty instanceof ArrayProperty) {
                    ArrayProperty arrayObject           = (ArrayProperty) responseProperty;
                    Property      arrayResponseProperty = arrayObject.getItems();

                    if (arrayResponseProperty instanceof RefProperty) {
                        ClassRelation relation = new ClassRelation();
                        relation.setTargetClass(((RefProperty) arrayResponseProperty).getSimpleRef());
                        relation.setComposition(false);
                        relation.setExtension(true);

                        relatedResponses.add(relation);
                    }
                }
            }

        }

        return relatedResponses;
    }


    private List<MethodDefinitions> getInterfaceMethods(Entry<HttpMethod, Operation> operationEntry) {
        final HttpMethod        method            = operationEntry.getKey();
        final Operation         operation         = operationEntry.getValue();
        List<MethodDefinitions> interfaceMethods  = new ArrayList<MethodDefinitions>();
        MethodDefinitions       methodDefinitions = new MethodDefinitions();
        final String methodDefinition =
                method.name() + " " + operation.getOperationId() + "(" + getMethodParameters(operation) + ")";
        methodDefinitions.setMethodDefinition(methodDefinition);
        methodDefinitions.setReturnType(getInterfaceReturnType(operation));

        interfaceMethods.add(methodDefinitions);

        return interfaceMethods;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getMethodParameters(Operation operation) {
        String          methodParameter = "";
        List<Parameter> parameters      = operation.getParameters();

        for (Parameter parameter : parameters) {
            if (StringUtils.isNotEmpty(methodParameter)) {
                methodParameter = new StringBuilder().append(methodParameter).append(",").toString();
            }

            if (parameter instanceof PathParameter) {
                methodParameter = new StringBuilder().append(methodParameter)
                                                     .append(toTitleCase(((PathParameter) parameter).getType())).append(" ")
                                                     .append(((PathParameter) parameter).getName()).toString();
            } else if (parameter instanceof QueryParameter) {
                Property queryParameterProperty = ((QueryParameter) parameter).getItems();

                if (queryParameterProperty instanceof RefProperty) {
                    methodParameter = new StringBuilder().append(methodParameter)
                                                         .append(toTitleCase(((RefProperty) queryParameterProperty).getSimpleRef()))
                                                         .append("[] ")
                                                         .append(((BodyParameter) parameter).getName())
                                                         .toString();
                } else if (queryParameterProperty instanceof StringProperty) {
                    methodParameter = new StringBuilder().append(methodParameter)
                                                         .append(toTitleCase(queryParameterProperty.getType()))
                                                         .append("[] ")
                                                         .append(((QueryParameter) parameter).getName())
                                                         .toString();
                } else {
                    methodParameter = new StringBuilder().append(methodParameter)
                                                         .append(toTitleCase(((QueryParameter) parameter).getType())).append(" ")
                                                         .append(((QueryParameter) parameter).getName()).toString();
                }
            } else if (parameter instanceof BodyParameter) {
                Model bodyParameter = ((BodyParameter) parameter).getSchema();

                if (bodyParameter instanceof RefModel) {
                    methodParameter = new StringBuilder().append(methodParameter)
                                                         .append(toTitleCase(((RefModel) bodyParameter).getSimpleRef())).append(" ")
                                                         .append(((BodyParameter) parameter).getName()).toString();
                } else if (bodyParameter instanceof ArrayModel) {
                    Property propertyObject = ((ArrayModel) bodyParameter).getItems();

                    if (propertyObject instanceof RefProperty) {
                        methodParameter = new StringBuilder().append(methodParameter)
                                                             .append(toTitleCase(((RefProperty) propertyObject).getSimpleRef()))
                                                             .append("[] ")
                                                             .append(((BodyParameter) parameter).getName())
                                                             .toString();
                    }
                }
            } else if (parameter instanceof FormParameter) {
                methodParameter = new StringBuilder().append(methodParameter)
                                                     .append(toTitleCase(((FormParameter) parameter).getType())).append(" ")
                                                     .append(((FormParameter) parameter).getName()).toString();
            }
        }

        return methodParameter;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getInterfaceReturnType(Operation operation) {
        String returnType = "void";

        Map<String, Response> responses = operation.getResponses();
        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    returnType = ((RefProperty) responseProperty).getSimpleRef();
                } else if (responseProperty instanceof ArrayProperty) {
                    Property arrayResponseProperty = ((ArrayProperty) responseProperty).getItems();
                    if (arrayResponseProperty instanceof RefProperty) {
                        returnType = new StringBuilder().append(((RefProperty) arrayResponseProperty).getSimpleRef())
                                                        .append("[]").toString();
                    }
                } else if (responseProperty instanceof ObjectProperty) {
                    returnType = new StringBuilder().append(toTitleCase(operation.getOperationId())).append("Generated")
                                                    .toString();
                }
            }
        }

        return returnType;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getErrorClassName(Operation operation) {
        StringBuilder         errorClass = new StringBuilder();
        Map<String, Response> responses  = operation.getResponses();
        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    String errorClassName = ((RefProperty) responseProperty).getSimpleRef();
                    if (!errorClass.toString().contains(errorClassName)) {
                        if (StringUtils.isNotEmpty(errorClass)) {
                            errorClass.append(",");
                        }
                        errorClass.append(errorClassName);
                    }
                }
            }
        }

        return errorClass.toString();
    }

    /**
     *
     * @param tags
     * @param operation
     * @param uri
     * @return
     */
    private String getInterfaceName(List<String> tags, Operation operation, String uri) {
        String interfaceName;

        if (!tags.isEmpty()) {
            interfaceName = toTitleCase(tags.get(0).replaceAll(" ", ""));
        } else if (StringUtils.isNotEmpty(operation.getOperationId())) {
            interfaceName = toTitleCase(operation.getOperationId());
        } else {
            interfaceName = toTitleCase(uri.replaceAll("\\{", "").replaceAll("}", "").replaceAll("\\\\", ""));
        }

        return new StringBuilder().append(interfaceName).append("Api").toString();
    }


    private String getOperationName(final HttpMethod method, Operation operation) {
        return toTitleCase(method + "_" + operation.getOperationId());
    }

    /**
     *
     * @param swagger
     * @return
     */
    private List<ClassDiagram> processSwaggerModels(Swagger swagger) {
        LOGGER.entering(LOGGER.getName(), "processSwaggerModels");

        List<ClassDiagram> classDiagrams = new ArrayList<ClassDiagram>();
        Map<String, Model> modelsMap     = swagger.getDefinitions();

        for (Map.Entry<String, Model> models : modelsMap.entrySet()) {
            String className   = models.getKey();
            Model  modelObject = models.getValue();

            LOGGER.info("Processing Model " + className);

            String             superClass   = getSuperClass(modelObject);
            List<ClassMembers> classMembers = getClassMembers(modelObject, modelsMap);

            classDiagrams.add(new ClassDiagram(className, modelObject.getDescription(), classMembers,
                                               getChildClasses(classMembers, superClass), isModelClass(modelObject), superClass));
        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerModels");

        return classDiagrams;
    }

    /**
     *
     * @param model
     * @return
     */
    private boolean isModelClass(Model model) {
        LOGGER.entering(LOGGER.getName(), "isModelClass");

        boolean isModelClass = true;

        if (model instanceof ModelImpl) {
            List<String> enumValues = ((ModelImpl) model).getEnum();

            if (enumValues != null && !enumValues.isEmpty()) {
                isModelClass = false;
            }
        }

        LOGGER.exiting(LOGGER.getName(), "isModelClass");

        return isModelClass;
    }

    /**
     *
     * @param model
     * @return
     */
    private String getSuperClass(Model model) {
        LOGGER.entering(LOGGER.getName(), "getSuperClass");

        String superClass = null;

        if (model instanceof ArrayModel) {
            ArrayModel arrayModel     = (ArrayModel) model;
            Property   propertyObject = arrayModel.getItems();

            if (propertyObject instanceof RefProperty) {
                superClass = new StringBuilder().append("ArrayList[")
                                                .append(((RefProperty) propertyObject).getSimpleRef()).append("]").toString();
            }
        } else if (model instanceof ModelImpl) {
            Property addProperty = ((ModelImpl) model).getAdditionalProperties();

            if (addProperty instanceof RefProperty) {
                superClass = new StringBuilder().append("Map[").append(((RefProperty) addProperty).getSimpleRef())
                                                .append("]").toString();
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getSuperClass");

        return superClass;
    }

    /**
     *
     * @param classMembers
     * @param superClass
     * @return
     */
    private List<ClassRelation> getChildClasses(List<ClassMembers> classMembers, String superClass) {
        LOGGER.entering(LOGGER.getName(), "getChildClasses");

        List<ClassRelation> childClasses = new ArrayList<ClassRelation>();

        for (ClassMembers member : classMembers) {

            boolean alreadyExists = false;

            for (ClassRelation classRelation : childClasses) {

                if (classRelation.getTargetClass().equalsIgnoreCase(member.getClassName())) {
                    alreadyExists = true;
                }
            }

            if (!alreadyExists && member.getClassName() != null && member.getClassName().trim().length() > 0) {
                if (StringUtils.isNotEmpty(superClass)) {
                    childClasses.add(new ClassRelation(member.getClassName(), true, false, member.getCardinality(), null));
                } else {
                    childClasses.add(new ClassRelation(member.getClassName(), false, true, member.getCardinality(), null));
                }
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getChildClasses");

        return childClasses;
    }

    /**
     *
     * @param modelObject
     * @param modelsMap
     * @return
     */
    private List<ClassMembers> getClassMembers(Model modelObject, Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        if (modelObject instanceof ModelImpl) {
            classMembers = getClassMembers((ModelImpl) modelObject, modelsMap);
        } else if (modelObject instanceof ComposedModel) {
            classMembers = getClassMembers((ComposedModel) modelObject, modelsMap);
        } else if (modelObject instanceof ArrayModel) {
            classMembers = getClassMembers((ArrayModel) modelObject, modelsMap);
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers");
        return classMembers;
    }

    /**
     *
     * @param arrayModel
     * @param modelsMap
     * @return
     */
    private List<ClassMembers> getClassMembers(ArrayModel arrayModel, Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ArrayModel");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        Property propertyObject = arrayModel.getItems();

        if (propertyObject instanceof RefProperty) {
            classMembers.add(getRefClassMembers((RefProperty) propertyObject));
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ArrayModel");
        return classMembers;
    }

    /**
     *
     * @param composedModel
     * @param modelsMap
     * @return
     */
    private List<ClassMembers> getClassMembers(ComposedModel composedModel, Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ComposedModel");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        Map<String, Property> childProperties = new HashMap<String, Property>();

        if (null != composedModel.getChild()) {
            childProperties = composedModel.getChild().getProperties();
        }

        List<Model> allOf = composedModel.getAllOf();
        for (Model currentModel : allOf) {

            if (currentModel instanceof RefModel) {
                RefModel refModel = (RefModel) currentModel;
                childProperties.putAll(modelsMap.get(refModel.getSimpleRef()).getProperties());

                classMembers = convertModelPropertiesToClassMembers(childProperties,
                                                                    modelsMap.get(refModel.getSimpleRef()), modelsMap);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ComposedModel");
        return classMembers;
    }

    /**
     *
     * @param model
     * @return
     */
    private List<ClassMembers> getClassMembers(ModelImpl model, Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ModelImpl");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        Map<String, Property> modelMembers = model.getProperties();
        if (modelMembers != null && !modelMembers.isEmpty()) {
            classMembers.addAll(convertModelPropertiesToClassMembers(modelMembers, model, modelsMap));
        } else {
            Property modelAdditionalProps = model.getAdditionalProperties();

            if (modelAdditionalProps instanceof RefProperty) {
                classMembers.add(getRefClassMembers((RefProperty) modelAdditionalProps));
            }

            if (modelAdditionalProps == null) {
                List<String> enumValues = model.getEnum();

                if (enumValues != null && !enumValues.isEmpty()) {
                    classMembers.addAll(getEnum(enumValues));
                }
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ModelImpl");

        return classMembers;
    }

    /**
     *
     * @param refProperty
     * @return
     */
    private ClassMembers getRefClassMembers(RefProperty refProperty) {
        LOGGER.entering(LOGGER.getName(), "getRefClassMembers");
        ClassMembers classMember = new ClassMembers();
        classMember.setClassName(refProperty.getSimpleRef());
        classMember.setName(" ");

        if (includeCardinality) {
            classMember.setCardinality(CARDINALITY_NONE_TO_MANY);
        }

        LOGGER.exiting(LOGGER.getName(), "getRefClassMembers");
        return classMember;
    }

    /**
     *
     * @param enumValues
     * @return
     */
    private List<ClassMembers> getEnum(List<String> enumValues) {
        LOGGER.entering(LOGGER.getName(), "getEnum");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        if (enumValues != null && !enumValues.isEmpty()) {
            for (String enumValue : enumValues) {
                ClassMembers classMember = new ClassMembers();
                classMember.setName(enumValue);
                classMembers.add(classMember);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getEnum");
        return classMembers;
    }

    /**
     *
     * @param modelMembers
     * @return
     */
    private List<ClassMembers> convertModelPropertiesToClassMembers(Map<String, Property> modelMembers,
                                                                    Model modelObject, Map<String, Model> models) {
        LOGGER.entering(LOGGER.getName(), "convertModelPropertiesToClassMembers");

        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();

        for (Map.Entry<String, Property> modelMapObject : modelMembers.entrySet()) {
            String variablName = modelMapObject.getKey();

            ClassMembers classMemberObject = new ClassMembers();
            Property     property          = modelMembers.get(variablName);

            if (property instanceof ArrayProperty) {
                classMemberObject = getClassMember((ArrayProperty) property, modelObject, models, variablName);
            } else if (property instanceof RefProperty) {
                classMemberObject = getClassMember((RefProperty) property, models, modelObject, variablName);
            } else {
                classMemberObject.setDataType(
                        getDataType(property.getFormat() != null ? property.getFormat() : property.getType(), false));
                classMemberObject.setName(variablName);
            }

            classMembers.add(classMemberObject);
        }

        LOGGER.exiting(LOGGER.getName(), "convertModelPropertiesToClassMembers");
        return classMembers;
    }

    /**
     *
     * @param modelObject
     * @param models
     * @param variablName
     */
    private ClassMembers getClassMember(ArrayProperty property, Model modelObject, Map<String, Model> models,
                                        String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-ArrayProperty");

        ClassMembers classMemberObject = new ClassMembers();
        Property     propObject        = property.getItems();

        if (propObject instanceof RefProperty) {
            classMemberObject = getClassMember((RefProperty) propObject, models, modelObject, variablName);
        } else if (propObject instanceof StringProperty) {
            classMemberObject = getClassMember((StringProperty) propObject, variablName);
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMember-ArrayProperty");
        return classMemberObject;
    }

    /**
     *
     * @param stringProperty
     * @param variablName
     * @return
     */
    private ClassMembers getClassMember(StringProperty stringProperty, String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-StringProperty");

        ClassMembers classMemberObject = new ClassMembers();
        classMemberObject.setDataType(getDataType(stringProperty.getType(), true));
        classMemberObject.setName(variablName);

        LOGGER.exiting(LOGGER.getName(), "getClassMember-StringProperty");
        return classMemberObject;
    }

    /**
     *
     * @param refProperty
     * @param models
     * @param modelObject
     * @param variablName
     * @return
     */
    private ClassMembers getClassMember(RefProperty refProperty, Map<String, Model> models, Model modelObject,
                                        String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-RefProperty");

        ClassMembers classMemberObject = new ClassMembers();
        classMemberObject.setDataType(getDataType(refProperty.getSimpleRef(), true));
        classMemberObject.setName(variablName);

        if (models.containsKey(refProperty.getSimpleRef())) {
            classMemberObject.setClassName(refProperty.getSimpleRef());
        }

        if (includeCardinality && StringUtils.isNotEmpty(variablName) && modelObject != null) {
            if (isRequiredProperty(modelObject, variablName)) {
                classMemberObject.setCardinality(CARDINALITY_ONE_TO_MANY);
            } else {
                classMemberObject.setCardinality(CARDINALITY_NONE_TO_MANY);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMember-RefProperty");
        return classMemberObject;
    }

    /**
     *
     * @param modelObject
     * @param propertyName
     * @return
     */
    private boolean isRequiredProperty(Model modelObject, String propertyName) {
        boolean isRequiredProperty = false;
        LOGGER.entering(LOGGER.getName(), "isRequiredProperty");

        if (modelObject != null) {
            if (modelObject instanceof ModelImpl) {
                List<String> requiredProperties = ((ModelImpl) modelObject).getRequired();
                if (requiredProperties != null && !requiredProperties.isEmpty()) {
                    isRequiredProperty = requiredProperties.contains(propertyName);
                }
            } else {
                isRequiredProperty = false;
            }
        }

        LOGGER.exiting(LOGGER.getName(), "isRequiredProperty");
        return isRequiredProperty;
    }

    /**
     *
     * @param className
     * @param isArray
     * @return
     */
    private String getDataType(String className, boolean isArray) {
        if (isArray) {
            return new StringBuilder().append(toTitleCase(className)).append("[]").toString();
        }

        return toTitleCase(className);
    }

    /**
     *
     * @param input
     * @return
     */
    private String toTitleCase(String input) {
        StringBuilder titleCase     = new StringBuilder();
        boolean       nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}