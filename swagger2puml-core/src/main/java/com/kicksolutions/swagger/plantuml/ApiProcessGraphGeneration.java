package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.graph.*;
import com.kicksolutions.swagger.plantuml.vo.*;
import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ApiProcessGraphGeneration implements GraphAdapter {
    public static final String API_DEFINITION = "API Definition";

    private static final Logger        LOGGER = Logger.getLogger(PlantUMLCodegen.class.getName());
    private final        Graph         graph;
    private final        boolean       domainClassGeneration;
    private final        boolean       apiGeneration;
    private final        EntityFactory entityFactory;
    private final OpenAPI swagger;
    private final boolean cardinalityGeneration;

    public ApiProcessGraphGeneration(final boolean domainClassGeneration, final boolean apiGeneration,
                                     final boolean cardinalityGeneration, final OpenAPI swagger) {
        this.domainClassGeneration = domainClassGeneration;
        this.apiGeneration = apiGeneration;
        this.cardinalityGeneration = cardinalityGeneration;
        this.swagger = swagger;
        graph = new Graph();
        graph.addGraphPackage(API_DEFINITION, "#lightgray-white");
        graph.addGraphPackage(NamingUtils.DOMAIN, "#lightgray-white");
        this.entityFactory = new EntityFactory(graph);

    }

    public Graph process() {

        // Browse tags to create resources
        createResources(swagger);
        processSwaggerPaths(swagger);
        processSwaggerModels(swagger);
        return graph;

    }

    private void createResources(final OpenAPI swagger) {
        Map<String, ResourceEntity> resourceEntityMap = new HashMap<>();

        for (Entry<String, PathItem> paths : swagger.getPaths().entrySet()) {
            final PathItem pathItem = paths.getValue();
            if (pathItem == null) continue;
            final Map<HttpMethod, Operation> methodOperationMap = pathItem.readOperationsMap();
            if (methodOperationMap == null) continue;


            methodOperationMap.forEach((httpMethod, operation) -> {
                final List<String> tags = operation.getTags();
                if (tags == null) return;
                // We attach the operation to the tags
                tags.forEach(tag -> {
                    if (!resourceEntityMap.containsKey(tag)) {
                        resourceEntityMap.put(tag, createResourceEntity(httpMethod, operation, tag));
                    } else {
                        final ResourceEntity resourceEntity = resourceEntityMap.get(tag);
                        resourceEntity.dependencies.addAll(getInterfaceRelations(operation, resourceEntity.getErrorClass(),
                                                                                 resourceEntity));
                        resourceEntity.methods.addAll(getInterfaceMethods(resourceEntity, httpMethod, operation));
                    }

                });
            });

        }
    }

    private ResourceEntity createResourceEntity(final HttpMethod httpMethod, final Operation operation, final String tag) {
        ResourceEntity resourceEntity = entityFactory.newResourceEntity(tag);
        String         errorClassName = getErrorClassName(operation);
        resourceEntity.dependencies = (getInterfaceRelations(operation, errorClassName, resourceEntity));
        resourceEntity.setErrorClass(errorClassName);
        resourceEntity.setMethods(getInterfaceMethods(resourceEntity, httpMethod, operation));
        return resourceEntity;
    }

    private void processSwaggerPaths(@NotNull OpenAPI swagger) {
        LOGGER.entering(LOGGER.getName(), "processSwaggerPaths");


        Paths paths = swagger.getPaths();

        for (Entry<String, PathItem> entry : paths.entrySet()) {
            PathItem pathObject = entry.getValue();

            LOGGER.info("Processing Path --> " + entry.getKey());

            Map<HttpMethod, Operation> operations = pathObject.readOperationsMap();
            String                     uri        = entry.getKey();

            for (Entry<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operationEntry : operations.entrySet()) {
                //diagramModel.interfaceDiagrams.add(getInterfaceDiagram(operationEntry, uri));
                //diagramModel.operationDiagrams.add();
                createOperationEntity(operationEntry, uri);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerPaths");
    }

    private @NotNull OperationEntity createOperationEntity(final @NotNull Entry<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operationEntry,
                                                           final @NotNull String uri) {
        LOGGER.entering(LOGGER.getName(), "getOperationInterface");
        PathItem.HttpMethod                method    = operationEntry.getKey();
        io.swagger.v3.oas.models.Operation operation = operationEntry.getValue();

        //FIX ME operation Id
        final String dotId         = NamingUtils.generateOperationId(method, uri, operation.getOperationId());
        String       interfaceName = NamingUtils.getOperationName(method, operation);

        OperationEntity operationEntity = new OperationEntity(dotId, interfaceName);
        graph.addEntity(operationEntity);

        String errorClassName = getErrorClassName(operation);

        // Operation dependencies
        //operationEntity.dependencies = (getInterfaceRelations(operation, errorClassName));

        // Build links with parent APIs
        List<String> tags = attachOperationToResource(operation, operationEntity);
        operationEntity.parentAPI = NamingUtils.getInterfaceName(tags, operation, uri);


        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                convertParameter(operationEntity, parameter);
            }
        }
        if (operation.getRequestBody() != null) {
            RequestBody                         bodyParameter = operation.getRequestBody();
            final Set<Entry<String, MediaType>> entries       = bodyParameter.getContent().entrySet();
            entries.forEach(stringMediaTypeEntry -> convertRequestBody(operationEntity, bodyParameter, stringMediaTypeEntry));
        }

        for (Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
            final ApiResponse         responseEntryValue = responseEntry.getValue();
            final Content             content            = responseEntryValue.getContent();
            final Map<String, Header> headers            = responseEntry.getValue().getHeaders();
            if (headers != null) {
                headers.entrySet().forEach(entry -> {
                    final FieldDefinition fieldDefinition = new FieldDefinition();
                    fieldDefinition.setFieldName(entry.getKey());
                    fieldDefinition.required = true;
                    fieldDefinition.setReturnType("string");
                    operationEntity.headers.add(fieldDefinition);
                });
            }
            for (Entry<String, MediaType> responseSwgDef : content.entrySet()) {
                convertResponse(operationEntity, responseEntry, responseSwgDef);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getOperationInterface");
        return operationEntity;

    }

    private void convertResponse(final OperationEntity operationEntity, final Entry<String, ApiResponse> responseEntry, final Entry<String,
            MediaType> responseSwgDef) {

        final String             mediaType          = responseSwgDef.getKey();
        final ResponseDefinition responseDefinition = new ResponseDefinition();
        responseDefinition.setFieldName(responseEntry.getKey());
        responseDefinition.setMediaType(mediaType);

        final MediaType value = responseSwgDef.getValue();
        responseDefinition.setReturnType(TypingUtils.resolveType(operationEntity, value.getSchema(), true));
        operationEntity.responses.add(responseDefinition);
    }

    @NotNull
    private List<String> attachOperationToResource(final Operation operation, final OperationEntity operationEntity) {
        List<String> tags = operation.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
        }
        tags.forEach(tag -> attachOperationToResource(operationEntity, tag));
        return tags;
    }

    private void attachOperationToResource(final OperationEntity operationEntity, final String tag) {
        // Attach operation to API
        Relationship classRelation = new Relationship();
        classRelation.setTargetClass(operationEntity.dotId);
        classRelation.setSourceClass(NamingUtils.generateResourceId(tag));
        classRelation.setComposition(false);
        classRelation.setExtends(true);
        operationEntity.dependencies.add(classRelation);
    }

    private void convertRequestBody(final OperationEntity operationEntity,
                                    @NotNull final RequestBody bodyParameter,
                                    final Entry<String, MediaType> stringMediaTypeEntry) {
        final FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.setFieldName("payload");
        fieldDefinition.required = bodyParameter.getRequired() != null && bodyParameter.getRequired();
        fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, stringMediaTypeEntry.getValue()
                                                                                                   .getSchema(), fieldDefinition.required));
        operationEntity.body.put(stringMediaTypeEntry.getKey(), fieldDefinition);
    }

    private void convertParameter(final OperationEntity operationEntity, final Parameter parameter) {
        final FieldDefinition fieldDefinition = new FieldDefinition();
        switch (parameter.getIn()) {
            case "path":
                Parameter pathParameter = parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired() != null && parameter.getRequired();
                if (pathParameter.getSchema() != null) {
                    fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema(),
                                                                          fieldDefinition.required));
                } else {
                    fieldDefinition.setReturnType("unknown");
                }
                operationEntity.pathParams.add(fieldDefinition);
                break;
            case "query":
                Parameter queryParameter = parameter;
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired() != null && parameter.getRequired();
                if (queryParameter.getSchema() != null) {
                    fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema(),
                                                                          fieldDefinition.required));
                } else {
                    fieldDefinition.setReturnType("unknown");
                }
                operationEntity.queryParams.add(fieldDefinition);
                break;
            case "header":
                convertHeader(operationEntity, parameter, fieldDefinition);
                break;
            default:
                LOGGER.log(Level.SEVERE, "Unsupported parameter type " + parameter.getIn());
                break;
        }
    }

    private void convertHeader(final OperationEntity operationEntity, final Parameter parameter, final FieldDefinition fieldDefinition) {
        fieldDefinition.setFieldName(parameter.getName());
        fieldDefinition.required = parameter.getRequired() != null && parameter.getRequired();
        fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema(), fieldDefinition.required));
        operationEntity.headers.add(fieldDefinition);
    }

    private @NotNull String obtainType(final String type, final String s, final String format) {
        return type + s + format;
    }


    private @NotNull Relationship getErrorClass(String errorClassName) {
        Relationship classRelation = new Relationship();
        classRelation.setTargetClass(errorClassName);
        classRelation.setComposition(false);
        classRelation.setExtension(true);

        return classRelation;
    }

    private @NotNull List<Relationship> getInterfaceRelatedInputs(@NotNull Operation operation) {
        List<Relationship> relatedResponses = new ArrayList<>();
        List<Parameter>    parameters       = operation.getParameters();
        if (parameters == null) return relatedResponses;
        for (Parameter parameter : parameters) {
            //TODO Not implemented yet
            /**
             if (parameter instanceof BodyParameter) {
             Model bodyParameter = ((BodyParameter) parameter).getSchema();

             if (bodyParameter instanceof RefModel) {

             Relationship classRelation = new Relationship();
             classRelation.setTargetClass(((RefModel) bodyParameter).getSimpleRef());
             classRelation.setComposition(false);
             classRelation.setExtension(true);

             relatedResponses.add(classRelation);
             } else if (bodyParameter instanceof ArrayModel) {
             Property propertyObject = ((ArrayModel) bodyParameter).getItems();

             if (propertyObject instanceof RefProperty) {
             Relationship classRelation = new Relationship();
             classRelation.setTargetClass(((RefProperty) propertyObject).getSimpleRef());
             classRelation.setComposition(false);
             classRelation.setExtension(true);

             relatedResponses.add(classRelation);
             }
             }
             }
             */
        }

        return relatedResponses;
    }

    private @NotNull List<Relationship> getInterfaceRelatedResponses(io.swagger.v3.oas.models.@NotNull Operation operation, Entity entity) {
        List<Relationship> relatedResponses = new ArrayList<>();
        ApiResponses       responses        = operation.getResponses();

        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {

                Relationship relation = new Relationship();
                relation.setTargetClass(TypingUtils.resolveRefType(entity, responsesEntry.getValue().get$ref(), false, true));
                relation.setComposition(false);
                relation.setExtension(true);

                relatedResponses.add(relation);
            }
        }

        return relatedResponses;
    }


    private @NotNull List<MethodDefinitions> getInterfaceMethods(final @NotNull Entity entity, @NotNull PathItem.HttpMethod method,
                                                                 Operation operation) {
        List<MethodDefinitions> interfaceMethods  = new ArrayList<>();
        MethodDefinitions       methodDefinitions = new MethodDefinitions();
        final String methodDefinition =
                method.name() + " " + operation.getOperationId() + "(" + getMethodParameters(operation) + ")";
        methodDefinitions.setMethodDefinition(methodDefinition);
        methodDefinitions.setReturnType(getInterfaceReturnType(entity, operation));

        interfaceMethods.add(methodDefinitions);

        return interfaceMethods;
    }

    private @NotNull Set<Relationship> filterUnique(List<Relationship> relations) {
        return new HashSet<>(relations);
    }


    private boolean isTargetClassInMap(@NotNull Relationship sourceRelation,
                                       @NotNull Set<Relationship> relatedResponses,
                                       boolean considerTargetOnly) {
        for (Relationship relation : relatedResponses) {

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


    private @NotNull Set<Relationship> getInterfaceRelations(@NotNull Operation operation,
                                                             String errorClassName, final Entity entity) {
        Set<Relationship> relations = new HashSet<>();
        relations.addAll(getInterfaceRelatedResponses(operation, entity));
        relations.addAll(getInterfaceRelatedInputs(operation));
        if (StringUtils.isNotEmpty(errorClassName)) {
            relations.add(getErrorClass(errorClassName));
        }

        return relations;
    }


    private @NotNull String getMethodParameters(io.swagger.v3.oas.models.@NotNull Operation operation) {
        String                                              methodParameter = "";
        List<io.swagger.v3.oas.models.parameters.Parameter> parameters      = operation.getParameters();
        if (parameters != null) {
            for (io.swagger.v3.oas.models.parameters.Parameter parameter : parameters) {
                if (StringUtils.isNotEmpty(methodParameter)) {
                    methodParameter = new StringBuilder().append(methodParameter).append(",").toString();
                }

                if (parameter instanceof io.swagger.v3.oas.models.parameters.PathParameter) {
                    methodParameter = new StringBuilder().append(methodParameter)
//FIXME                                                     .append(toTitleCase(((io.swagger.v3.oas.models.parameters.PathParameter)
// parameter).getType()))
                                                         .append(" ")
                                                         .append(parameter.getName())
                                                         .toString();
                } else if (parameter instanceof io.swagger.v3.oas.models.parameters.QueryParameter) {
                    /**
                     Property queryParameterProperty = ((io.swagger.v3.oas.models.parameters.QueryParameter) parameter).getItems();

                     if (queryParameterProperty instanceof RefProperty) {
                     methodParameter = new StringBuilder().append(methodParameter)
                     .append(toTitleCase(((RefProperty) queryParameterProperty).getSimpleRef()))
                     .append("[] ")
                     .append(parameter.getName())
                     .toString();
                     } else if (queryParameterProperty instanceof StringProperty) {
                     methodParameter = new StringBuilder().append(methodParameter)
                     .append(toTitleCase(queryParameterProperty.getType()))
                     .append("[] ")
                     .append(parameter.getName())
                     .toString();
                     } else {
                     methodParameter = new StringBuilder().append(methodParameter)
                     .append(toTitleCase(((QueryParameter) parameter).getType())).append(" ")
                     .append(parameter.getName()).toString();
                     }
                     **/
                }
                /**
                 else if (parameter instanceof BodyParameter) {
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
                 }**/
            }
        }

        return methodParameter;
    }


    private String getInterfaceReturnType(final @NotNull Entity entity, @NotNull Operation operation) {

        ApiResponses responses   = operation.getResponses();
        Set<String>  returnTypes = new HashSet<>();
        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            final ApiResponse apiResponse = responsesEntry.getValue();
            if (apiResponse.get$ref() != null) {
                final String componentId = NamingUtils.getComponentId(apiResponse.get$ref());
                final Schema schema      = findComponent(componentId);
                returnTypes.add(TypingUtils.resolveType(entity, schema, true));
            } else {
                apiResponse.getContent().entrySet().forEach(entry -> {

                    returnTypes.add(TypingUtils.resolveType(entity, entry.getValue().getSchema(), true));
                });
            }

        }
        return returnTypes.isEmpty() ? "any" : returnTypes.stream().collect(Collectors.joining("|"));
    }


    private @NotNull String getErrorClassName(@NotNull io.swagger.v3.oas.models.@NotNull Operation operation) {
        StringBuilder errorClass = new StringBuilder();
        ApiResponses  responses  = operation.getResponses();
        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300) {
                /*
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
                */
            }
        }

        return errorClass.toString();
    }


    private void processSwaggerModels(@NotNull OpenAPI swagger) {
        LOGGER.entering(LOGGER.getName(), "processSwaggerModels");

        Map<String, Schema> modelsMap = swagger.getComponents().getSchemas();

        for (Map.Entry<String, Schema> models : modelsMap.entrySet()) {
            newDomainEntity(modelsMap, models);

        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerModels");

    }

    private void newDomainEntity(final Map<String, Schema> modelsMap, final Entry<String, Schema> models) {
        String className   = models.getKey();
        Schema modelObject = models.getValue();

        LOGGER.info("Processing Model " + className);

        String superClass = getSuperClass(modelObject);

        final String dotId = NamingUtils.newDomainId(className);

        final DomainEntity domainEntity = new DomainEntity(dotId, className);
        domainEntity.setDescription(modelObject.getDescription());
        domainEntity.setSuperClass(superClass);
        domainEntity.setFields(getClassMembers(domainEntity, modelObject));

        graph.addEntity(domainEntity);
    }


    private boolean isModelClass(Schema model) {
        LOGGER.entering(LOGGER.getName(), "isModelClass");

        boolean isModelClass = true;
/**
 if (model instanceof ModelImpl) {
 List<String> enumValues = ((ModelImpl) model).getEnum();

 if (enumValues != null && !enumValues.isEmpty()) {
 isModelClass = false;
 }
 }
 **/

        LOGGER.exiting(LOGGER.getName(), "isModelClass");

        return isModelClass;
    }

    private @Nullable String getSuperClass(Schema model) {
        LOGGER.entering(LOGGER.getName(), "getSuperClass");

        String superClass = null;
        /**
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
         **/
        LOGGER.exiting(LOGGER.getName(), "getSuperClass");

        return superClass;
    }

    private @NotNull List<Relationship> getChildClasses(@NotNull List<Field> classMembers, String superClass) {
        LOGGER.entering(LOGGER.getName(), "getChildClasses");

        List<Relationship> childClasses = new ArrayList<>();

        for (Field member : classMembers) {

            boolean alreadyExists = false;

            for (Relationship classRelation : childClasses) {

                if (classRelation.getTargetClass().equalsIgnoreCase(member.getClassName())) {
                    alreadyExists = true;
                }
            }

            if (!alreadyExists && member.getClassName() != null && member.getClassName().trim().length() > 0) {
                if (StringUtils.isNotEmpty(superClass)) {
                    childClasses.add(new Relationship(member.getClassName(), true, false, member.getCardinality(), null));
                } else {
                    childClasses.add(new Relationship(member.getClassName(), false, true, member.getCardinality(), null));
                }
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getChildClasses");

        return childClasses;
    }


    private @NotNull List<Field> getClassMembers(final @NotNull Entity entity, Schema modelObject) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers");

        List<Field> classMembers = new ArrayList<>();
        if (modelObject instanceof ObjectSchema) {
            ObjectSchema schema = (ObjectSchema) modelObject;
            classMembers = getClassMembersFromModel(entity, schema);
        } else if (modelObject instanceof ArraySchema) {
            // Nothing to do
        } else if (modelObject instanceof ComposedSchema) {
            // Nothing to do
            classMembers = getClassMembersFromComposedModel(entity, (ComposedSchema) modelObject);
        } else {
            System.out.println("Unsupported schema " + modelObject.getClass().getName());
        }
        LOGGER.exiting(LOGGER.getName(), "getClassMembers");
        return classMembers;
    }


    private @NotNull List<Field> getClassMembersFromComposedModel(@NotNull Entity entity, ComposedSchema composedModel) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ComposedModel");

        List<Field> classMembers = new ArrayList<Field>();

        List<Schema> allOf = composedModel.getAllOf();
        for (Schema currentModel : allOf) {
            final Map properties = currentModel.getProperties();
            if (currentModel.getAdditionalProperties() != null) {
                System.out.println(currentModel.getAdditionalProperties());
            }
            final String referencedModel = currentModel.get$ref();
            if (referencedModel != null) {
                Relationship relation = new Relationship();
                relation.setTargetClass(TypingUtils.resolveRefType(entity, referencedModel, false, true));
                relation.setComposition(false);
                relation.setExtension(true);
                entity.dependencies.add(relation);
                final String componentId = NamingUtils.getComponentId(referencedModel);
                final Schema schema      = findComponent(componentId);
                if (schema == null) {
                    LOGGER.log(Level.SEVERE, "Cannot find the type identified by the reference " + referencedModel);
                }
                classMembers = convertModelPropertiesToClassMembers(schema.getProperties(), entity, schema.getRequired());
            } else {
                classMembers = convertModelPropertiesToClassMembers(properties, entity, currentModel.getRequired());
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ComposedModel");
        return classMembers;
    }

    private Schema findComponent(final String componentId) {
        final Schema schema = this.swagger.getComponents().getSchemas().get(componentId);
        return schema;
    }


    private @NotNull List<Field> getClassMembersFromModel(final @NotNull Entity entity, ObjectSchema model) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ModelImpl");
        List<Field> classMembers = new ArrayList<>();

        if (model.getAdditionalProperties() != null) {
            System.out.println(model.getAdditionalProperties());
        }
        Map<String, Schema> modelMembers = model.getProperties();
        if (modelMembers != null && !modelMembers.isEmpty()) {
            classMembers = modelMembers.entrySet()
                                       .stream()
                                       .map(entry -> {
                                           final Field field1 = new Field();
                                           field1.setClassName(entry.getKey());
                                           field1.setName(entry.getKey());
                                           final boolean isRequired = model.getRequired() != null && model.getRequired()
                                                                                                          .contains(entry.getKey());
                                           field1.setCardinality(isRequired ? "1..1" : "0..1");
                                           field1.setDataType(TypingUtils.resolveType(entity, entry.getValue(), isRequired));
                                           return field1;
                                       })
                                       .collect(Collectors.toList());
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ModelImpl");

        return classMembers;
    }


    private @NotNull Field getRefClassMembers(@NotNull RefProperty refProperty) {
        LOGGER.entering(LOGGER.getName(), "getRefClassMembers");
        Field classMember = new Field();
        classMember.setClassName(refProperty.getSimpleRef());
        classMember.setName(" ");
        /**
         if (includeCardinality) {
         classMember.setCardinality(CARDINALITY_NONE_TO_MANY);
         }
         **/

        LOGGER.exiting(LOGGER.getName(), "getRefClassMembers");
        return classMember;
    }


    private @NotNull List<Field> getEnum(@Nullable List<String> enumValues) {
        LOGGER.entering(LOGGER.getName(), "getEnum");

        List<Field> classMembers = new ArrayList<>();

        if (enumValues != null && !enumValues.isEmpty()) {
            for (String enumValue : enumValues) {
                Field classMember = new Field();
                classMember.setName(enumValue);
                classMembers.add(classMember);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getEnum");
        return classMembers;
    }


    private @NotNull List<Field> convertModelPropertiesToClassMembers(@NotNull Map<String, Schema> modelMembers,
                                                                      Entity entity, final List required) {
        LOGGER.entering(LOGGER.getName(), "convertModelPropertiesToClassMembers");

        List<Field> classMembers = new ArrayList<>();

        for (Entry<String, Schema> modelMapObject : modelMembers.entrySet()) {
            final String  variablName = modelMapObject.getKey();
            final boolean isRequired  = required != null && required.contains(variablName);
            final Schema  schema      = modelMapObject.getValue();

            Field classMemberObject = new Field();
            classMemberObject.setName(variablName);
            classMemberObject.setDataType(TypingUtils.resolveType(entity, schema, isRequired));
            classMemberObject.setCardinality(isRequired ? "1..1" : "0..1");
            classMembers.add(classMemberObject);
        }

        LOGGER.exiting(LOGGER.getName(), "convertModelPropertiesToClassMembers");
        return classMembers;
    }


    private @NotNull Field getClassMember(@NotNull ArrayProperty property,
                                          Model modelObject,
                                          @NotNull Map<String, Model> models,
                                          String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-ArrayProperty");

        Field    classMemberObject = new Field();
        Property propObject        = property.getItems();

        if (propObject instanceof RefProperty) {
            classMemberObject = getClassMember((RefProperty) propObject, models, modelObject, variablName);
        } else if (propObject instanceof StringProperty) {
            classMemberObject = getClassMember((StringProperty) propObject, variablName);
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMember-ArrayProperty");
        return classMemberObject;
    }


    private @NotNull Field getClassMember(@NotNull StringProperty stringProperty, String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-StringProperty");

        Field classMemberObject = new Field();
        //classMemberObject.setDataType(getDataType(stringProperty.getType(), true));
        classMemberObject.setName(variablName);

        LOGGER.exiting(LOGGER.getName(), "getClassMember-StringProperty");
        return classMemberObject;
    }


    private @NotNull Field getClassMember(@NotNull RefProperty refProperty,
                                          @NotNull Map<String, Model> models,
                                          @Nullable Model modelObject,
                                          String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-RefProperty");

        Field classMemberObject = new Field();
/**
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
 */
        return classMemberObject;
    }


    private boolean isRequiredProperty(@Nullable Model modelObject, String propertyName) {
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


    @Override
    public void adapt(final Map<String, Object> additionalProperties) {

        new ApiGraphAdapter(graph, domainClassGeneration, apiGeneration, cardinalityGeneration).adapt(additionalProperties);

    }
}
