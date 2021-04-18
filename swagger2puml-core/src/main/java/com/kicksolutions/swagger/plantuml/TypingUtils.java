package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.graph.Relationship;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;

public class TypingUtils {
    public static String resolveType(@NotNull Entity entity, @Nullable final Schema schema, final boolean required) {
        if (schema == null) return "unknown";
        Validate.notNull(entity);
        if (schema instanceof ArraySchema) {
            return resolveArrayType(entity, (ArraySchema) schema, required);
        } else {
            return resolveNonArrayType(entity, schema, required);
        }
    }

    private static String resolveArrayType(final Entity entity, final ArraySchema schema, final boolean required) {
        if (schema.get$ref() != null) {
            return resolveRefType(entity, schema.get$ref(), true, required) + "[]";
        }
        if (schema.getItems().get$ref() != null) {
            return resolveRefType(entity, schema.getItems().get$ref(), true, required) + "[]";
        }
        if (schema.getFormat() != null) {
            return schema.getItems().getType() + " " + schema.getItems().getFormat() + "[]";
        } else {
            return schema.getItems().getType() + "[]";
        }

    }

    public static String resolveNonArrayType(final @NotNull Entity entity, final @NotNull Schema schema, final boolean required) {

        if (schema instanceof MapSchema) {
            String valueType = resolveType(entity, (Schema) schema.getAdditionalProperties(), required);
            return "Map<string, " + valueType + ">";
        } else {
            return resolveNonArrayNonMapType(entity, schema, required);
        }
    }

    private static String resolveNonArrayNonMapType(final @NotNull Entity entity, final @NotNull Schema schema, final boolean required) {
        if (schema.get$ref() != null) {
            return resolveRefType(entity, schema.get$ref(), false, required);
        }
        if (schema.getFormat() != null) {
            return schema.getType() + " " + schema.getFormat();
        } else {
            return schema.getType();
        }
    }


    public static String resolveRefType(final @NotNull Entity entity, final String $ref, boolean array, boolean required) {
        if ($ref == null) return "unknown";
        final String       componentId  = NamingUtils.getComponentId($ref);
        final Relationship relationship = entity.newDependency(NamingUtils.newDomainId(componentId));
        relationship.setExtension(true);
        if (array) {
            relationship.setCardinality("0..*");
        } else if (required) {
            relationship.setCardinality("1..1");
        } else {
            relationship.setCardinality("0..1");
        }
        return componentId;

    }


}
