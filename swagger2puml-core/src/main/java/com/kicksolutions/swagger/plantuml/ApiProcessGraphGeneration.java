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
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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

    public ApiProcessGraphGeneration(final boolean domainClassGeneration, final boolean apiGeneration) {
        this.domainClassGeneration = domainClassGeneration;
        this.apiGeneration = apiGeneration;
        graph = new Graph();
        graph.addGraphPackage(API_DEFINITION, "#lightgray-white");
        graph.addGraphPackage(NamingUtils.DOMAIN, "#lightgray-white");
        this.entityFactory = new EntityFactory(graph);

    }

    public Graph process(final OpenAPI swagger) {

        // Browse tags to create resources
        createResources(swagger);
        processSwaggerPaths(swagger);
        processSwaggerModels(swagger);
        return graph;

    }

    private void createResources(final OpenAPI swagger) {
        for (Entry<String, PathItem> paths : swagger.getPaths().entrySet()) {
            final PathItem pathItem = paths.getValue();
            if (pathItem == null) continue;
            final Map<HttpMethod, Operation> methodOperationMap = pathItem.readOperationsMap();
            if (methodOperationMap == null) continue;

            methodOperationMap.forEach((httpMethod, operation) -> {
                final List<String> tags = operation.getTags();
                if (tags == null) return;
                tags.forEach(tag -> {
                    createResourceEntity(httpMethod, operation, tag);
                });
            });

        }
    }

    private void createResourceEntity(final HttpMethod httpMethod, final Operation operation, final String tag) {
        ResourceEntity resourceEntity = entityFactory.newResourceEntity(operation, tag, httpMethod);
        String         errorClassName = getErrorClassName(operation);
        resourceEntity.dependencies = (getInterfaceRelations(operation, errorClassName));
        resourceEntity.setErrorClass(errorClassName);
        resourceEntity.setMethods(getInterfaceMethods(httpMethod, operation));
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
                newOperationEntity(operationEntry, uri);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerPaths");
    }

    private @NotNull OperationEntity newOperationEntity(final @NotNull Entry<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operationEntry,
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
            final ApiResponse responseEntryValue = responseEntry.getValue();
            final Content     content            = responseEntryValue.getContent();

            for (Entry<String, MediaType> responseSwgDef : content.entrySet()) {
                convertResponse(operationEntity, responseEntry, responseSwgDef);
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getOperationInterface");
        return operationEntity;

    }

    private void convertResponse(final OperationEntity operationEntity, final Entry<String, ApiResponse> responseEntry, final Entry<String,
            MediaType> responseSwgDef) {
        final ResponseDefinition responseDefinition = new ResponseDefinition();
        responseDefinition.setFieldName(responseEntry.getKey());
        final String mediaType = responseSwgDef.getKey();
        responseDefinition.setMediaType(mediaType);

        final MediaType value = responseSwgDef.getValue();
        responseDefinition.setReturnType(TypingUtils.resolveType(operationEntity, value.getSchema()));
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

    private void convertRequestBody(final OperationEntity operationEntity, @NotNull final RequestBody bodyParameter,
                                    final Entry<String, MediaType> stringMediaTypeEntry) {
        final FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.setFieldName("payload");
        fieldDefinition.required = bodyParameter.getRequired() != null && bodyParameter.getRequired();
        fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, stringMediaTypeEntry.getValue().getSchema()));
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
                    fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema()));
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
                    fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema()));
                } else {
                    fieldDefinition.setReturnType("unknown");
                }
                operationEntity.queryParams.add(fieldDefinition);
                break;
            case "header":
                fieldDefinition.setFieldName(parameter.getName());
                fieldDefinition.required = parameter.getRequired() != null && parameter.getRequired();
                fieldDefinition.setReturnType(TypingUtils.resolveType(operationEntity, parameter.getSchema()));
                operationEntity.headers.add(fieldDefinition);
                break;
            default:
                LOGGER.log(Level.SEVERE, "Unsupported parameter type " + parameter.getIn());
                break;
        }
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

    private @NotNull List<Relationship> getInterfaceRelatedResponses(io.swagger.v3.oas.models.@NotNull Operation operation) {
        List<Relationship> relatedResponses = new ArrayList<>();
        ApiResponses       responses        = operation.getResponses();

        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                //FIXME::Property responseProperty = responsesEntry.getValue().getContent();
                Property responseProperty = null;

                if (responseProperty instanceof RefProperty) {
                    Relationship relation = new Relationship();
                    relation.setTargetClass(((RefProperty) responseProperty).getSimpleRef());
                    relation.setComposition(false);
                    relation.setExtension(true);

                    relatedResponses.add(relation);
                } else if (responseProperty instanceof ArrayProperty) {
                    ArrayProperty arrayObject           = (ArrayProperty) responseProperty;
                    Property      arrayResponseProperty = arrayObject.getItems();

                    if (arrayResponseProperty instanceof RefProperty) {
                        Relationship relation = new Relationship();
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


    private @NotNull List<MethodDefinitions> getInterfaceMethods(@NotNull io.swagger.v3.oas.models.PathItem.HttpMethod method,
                                                                 Operation operation) {
        List<MethodDefinitions> interfaceMethods  = new ArrayList<>();
        MethodDefinitions       methodDefinitions = new MethodDefinitions();
        final String methodDefinition =
                method.name() + " " + operation.getOperationId() + "(" + getMethodParameters(operation) + ")";
        methodDefinitions.setMethodDefinition(methodDefinition);
        methodDefinitions.setReturnType(getInterfaceReturnType(operation));

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


    private @NotNull Set<Relationship> getInterfaceRelations(io.swagger.v3.oas.models.@NotNull Operation operation,
                                                             String errorClassName) {
        Set<Relationship> relations = new HashSet<>();
        relations.addAll(getInterfaceRelatedResponses(operation));
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


    private String getInterfaceReturnType(@NotNull Operation operation) {
        String returnType = "void";

        ApiResponses responses = operation.getResponses();
        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                /**Property responseProperty = responsesEntry.getValue().getSchema();

                 if (responseProperty instanceof RefProperty) {
                 returnType = ((RefProperty) responseProperty).getSimpleRef();
                 } else if (responseProperty instanceof ArrayProperty) {
                 Property arrayResponseProperty = ((ArrayProperty) responseProperty).getItems();
                 if (arrayResponseProperty instanceof RefProperty) {
                 returnType = ((RefProperty) arrayResponseProperty).getSimpleRef() + "[]";
                 }
                 } else if (responseProperty instanceof ObjectProperty) {
                 returnType = toTitleCase(operation.getOperationId()) + "Generated";
                 }
                 */
            }
        }

        return returnType;
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

        String             superClass   = getSuperClass(modelObject);
        List<ClassMembers> classMembers = getClassMembers(modelObject, modelsMap);
        final String       dotId        = NamingUtils.newDomainId(className);
        final DomainEntity domainEntity = new DomainEntity(dotId, className);
        domainEntity.description = modelObject.getDescription();
        domainEntity.classMembers = classMembers;
        domainEntity.childClasses = getChildClasses(classMembers, superClass);
        domainEntity.superClass = superClass;
        domainEntity.dependencies.addAll(domainEntity.childClasses);
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

    private @NotNull List<Relationship> getChildClasses(@NotNull List<ClassMembers> classMembers, String superClass) {
        LOGGER.entering(LOGGER.getName(), "getChildClasses");

        List<Relationship> childClasses = new ArrayList<>();

        for (ClassMembers member : classMembers) {

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


    private @NotNull List<ClassMembers> getClassMembers(Model modelObject,
                                                        @NotNull Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers");

        List<ClassMembers> classMembers = new ArrayList<>();

        if (modelObject instanceof ModelImpl) {
            classMembers = getClassMembers((ModelImpl) modelObject, modelsMap);
        } else if (modelObject instanceof ComposedModel) {
            classMembers = getClassMembers((ComposedModel) modelObject, modelsMap);
        } else if (modelObject instanceof ArrayModel) {
            classMembers = getClassMembers(modelObject, modelsMap);
        }

        LOGGER.exiting(LOGGER.getName(), "getClassMembers");
        return classMembers;
    }

    private @NotNull List<ClassMembers> getClassMembers(Schema arrayModel, Map<String, Schema> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ArrayModel");
        List<ClassMembers> classMembers = new ArrayList<ClassMembers>();
        /**


         Property propertyObject = arrayModel.getItems();

         if (propertyObject instanceof RefProperty) {
         classMembers.add(getRefClassMembers((RefProperty) propertyObject));
         }
         */
        LOGGER.exiting(LOGGER.getName(), "getClassMembers-ArrayModel");
        return classMembers;
    }


    private @NotNull List<ClassMembers> getClassMembers(@NotNull ComposedModel composedModel, @NotNull Map<String, Model> modelsMap) {
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


    private @NotNull List<ClassMembers> getClassMembers(@NotNull ModelImpl model,
                                                        @NotNull Map<String, Model> modelsMap) {
        LOGGER.entering(LOGGER.getName(), "getClassMembers-ModelImpl");

        List<ClassMembers> classMembers = new ArrayList<>();

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


    private @NotNull ClassMembers getRefClassMembers(@NotNull RefProperty refProperty) {
        LOGGER.entering(LOGGER.getName(), "getRefClassMembers");
        ClassMembers classMember = new ClassMembers();
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


    private @NotNull List<ClassMembers> getEnum(@Nullable List<String> enumValues) {
        LOGGER.entering(LOGGER.getName(), "getEnum");

        List<ClassMembers> classMembers = new ArrayList<>();

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


    private @NotNull List<ClassMembers> convertModelPropertiesToClassMembers(@NotNull Map<String, Property> modelMembers,
                                                                             Model modelObject,
                                                                             @NotNull Map<String, Model> models) {
        LOGGER.entering(LOGGER.getName(), "convertModelPropertiesToClassMembers");

        List<ClassMembers> classMembers = new ArrayList<>();

        for (Map.Entry<String, Property> modelMapObject : modelMembers.entrySet()) {
            String variablName = modelMapObject.getKey();

            ClassMembers classMemberObject = new ClassMembers();
            Property     property          = modelMembers.get(variablName);

            if (property instanceof ArrayProperty) {
                classMemberObject = getClassMember((ArrayProperty) property, modelObject, models, variablName);
            } else if (property instanceof RefProperty) {
                classMemberObject = getClassMember((RefProperty) property, models, modelObject, variablName);
            } else {
                //classMemberObject.setDataType(TypeUtils);
                classMemberObject.setName(variablName);
            }

            classMembers.add(classMemberObject);
        }

        LOGGER.exiting(LOGGER.getName(), "convertModelPropertiesToClassMembers");
        return classMembers;
    }


    private @NotNull ClassMembers getClassMember(@NotNull ArrayProperty property,
                                                 Model modelObject,
                                                 @NotNull Map<String, Model> models,
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


    private @NotNull ClassMembers getClassMember(@NotNull StringProperty stringProperty, String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-StringProperty");

        ClassMembers classMemberObject = new ClassMembers();
        //classMemberObject.setDataType(getDataType(stringProperty.getType(), true));
        classMemberObject.setName(variablName);

        LOGGER.exiting(LOGGER.getName(), "getClassMember-StringProperty");
        return classMemberObject;
    }


    private @NotNull ClassMembers getClassMember(@NotNull RefProperty refProperty,
                                                 @NotNull Map<String, Model> models,
                                                 @Nullable Model modelObject,
                                                 String variablName) {
        LOGGER.entering(LOGGER.getName(), "getClassMember-RefProperty");

        ClassMembers classMemberObject = new ClassMembers();
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


    public List<ClassDiagram> generateClassDiagrams(final Graph graph) {

        final List<ClassDiagram> resourceClasses = graph.filter(ResourceEntity.class).stream().map(r -> {
            final ClassDiagram classDiagram = new ClassDiagram();
            classDiagram.setClassName(r.dotId);
            classDiagram.setDescription(r.name);
            classDiagram.setStereotype("R");
            classDiagram.setColor("lightblue");
            classDiagram.setBackgroundColor("#white-lightblue");
            classDiagram.setDomain(API_DEFINITION);
            return classDiagram;
        }).collect(Collectors.toList());


        final List<ClassDiagram> domainClasses = graph.filter(DomainEntity.class).stream().map(r -> {
            final ClassDiagram classDiagram = new ClassDiagram();
            classDiagram.setClassName(r.dotId);
            classDiagram.setDescription(r.name);
            classDiagram.setChildClass(classDiagram.getChildClass());
            classDiagram.setSuperClass(classDiagram.getSuperClass());
            classDiagram.setFields(classDiagram.getFields());
            classDiagram.setStereotype("C");
            classDiagram.setColor("lightyellow");
            classDiagram.setBackgroundColor("#white-lightyellow");
            classDiagram.setDomain(NamingUtils.DOMAIN);
            return classDiagram;
        }).collect(Collectors.toList());

        final List<ClassDiagram> collect = new ArrayList<>();
        collect.addAll(resourceClasses);
        collect.addAll(domainClasses);
        return collect;
    }

    public GraphAdapter getGraphAdapter() {

        return this;

    }

    @Override
    public void adapt(final Map<String, Object> additionalProperties, final Graph graph) {
        additionalProperties.put("classDiagrams", generateClassDiagrams(graph));
        additionalProperties.put("graphPackages", graph.graphPackages);
        //additionalProperties.put("interfaceDiagrams", interfaceDiagrams);


        final List<OperationDiagram> operationDiagrams = graph.filter(OperationEntity.class).stream().map(op -> {
            final OperationDiagram operationDiagram = new OperationDiagram();
            operationDiagram.setClassName(op.dotId);
            operationDiagram.setDescription(op.name);
            operationDiagram.setStereotype("O");
            operationDiagram.setColor("lightgreen");
            operationDiagram.setBackgroundColor("#white-lightgreen");
            operationDiagram.setDomain(API_DEFINITION);
            operationDiagram.setFormParams(op.formParams);
            operationDiagram.setPathParams(op.pathParams);
            operationDiagram.setQueryParams(op.queryParams);
            operationDiagram.setHeaders(op.headers);
            operationDiagram.setResponses(op.responses);
            operationDiagram.setBody(op.body);
            return operationDiagram;
        }).collect(Collectors.toList());

        additionalProperties.put("operationDiagrams", operationDiagrams);

        // Compute all relationships
        additionalProperties.put("entityRelations", computeAllRelations(graph, apiGeneration, domainClassGeneration));

    }

    /**
     * Compute all relations
     *
     * @return
     */

    private @NotNull Set<Relationship> computeAllRelations(final Graph graph, boolean apiGeneration, boolean domainClassGeneration) {
        Set<String> entitiesId = this.graph.entityList.stream().map(e -> e.dotId).collect(Collectors.toSet());
        return graph.entityList.stream()
                               .flatMap(entity -> entity.dependencies.stream().map(r -> {
                                   if (r.getSourceClass() == null) {
                                       r.from = entity.dotId;
                                   }
                                   if (r.getTargetClass() == null) {
                                       r.to = entity.dotId;
                                   }
                                   return r;
                               }))
                               .filter(r -> entitiesId.contains(r.getSourceClass()) && entitiesId.contains(r.getTargetClass()))
                               .collect(Collectors.toSet());
    }

}
