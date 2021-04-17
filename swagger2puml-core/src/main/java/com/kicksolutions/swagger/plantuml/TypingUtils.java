package com.kicksolutions.swagger.plantuml;

import com.kicksolutions.swagger.graph.Entity;
import com.kicksolutions.swagger.graph.Relationship;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

public class TypingUtils {
    public static String resolveType(@NotNull Entity entity, @NotNull final Schema schema) {
        Validate.notNull(entity);
        if (schema instanceof ArraySchema) {
            return resolveArrayType(entity, (ArraySchema) schema);
        } else {
            return resolveNonArrayType(entity, schema);
        }
    }

    private static String resolveArrayType(final Entity entity, final ArraySchema schema) {
        if (schema.get$ref() != null) {
            return resolveRefType(entity, schema.getItems().get$ref()) + "[]";
        }
        if (schema.getFormat() != null) {
            return schema.getItems().getType() + " " + schema.getItems().getFormat() + "[]";
        } else {
            return schema.getItems().getType() + "[]";
        }

    }

    public static String resolveNonArrayType(final @NotNull Entity entity, final @NotNull Schema schema) {
        if (schema.get$ref() != null) {
            return resolveRefType(entity, schema.get$ref());
        }
        if (schema.getFormat() != null) {
            return schema.getType() + " " + schema.getFormat();
        } else {
            return schema.getType();
        }
    }



    public static String resolveRefType(final @NotNull Entity entity, final String $ref) {
        if ($ref == null) return "unknown";
        final String componentId = NamingUtils.getComponentId($ref);
        final Relationship relationship = entity.newDependency(NamingUtils.newDomainId(componentId));
        relationship.setExtension(true);
        return componentId;

    }


}
