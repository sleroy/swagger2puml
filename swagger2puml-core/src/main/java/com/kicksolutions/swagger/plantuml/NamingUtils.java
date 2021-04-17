package com.kicksolutions.swagger.plantuml;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NamingUtils {
    public static final String DOMAIN = "Domain";

    public static @NotNull String toTitleCase(@NotNull String input) {
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

    public static @NotNull String toID(@NotNull String input) {
        StringBuilder titleCase     = new StringBuilder();
        boolean       nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                c = '_';
            }
            titleCase.append(c);
        }

        return titleCase.toString();
    }

    @NotNull
    public static String generateResourceName(final String tag) {
        return StringUtils.capitalize(tag) + "Resource";
    }

    public static String generateResourceId(final String tag) {
        return toTitleCase(tag);

    }


    @NotNull
    public static String generateInterfaceId(final String interfaceName) {
        return NamingUtils.toID(interfaceName);
    }


    @NotNull
    public static String generateOperationId(final HttpMethod method, final String uri, final String operationId) {
        return NamingUtils.toID(method + "_" + uri + "_" + operationId);
    }


    public static @NotNull String obtainSimpleTypeName(String type, @Nullable String format) {
        return type + (format != null ? (" " + format) : "");
    }


    /**
     * Obtain component ID from ref
     * @param resolveRefName the ref name
     * @return the component ID
     */
    public static String getComponentId(final String resolveRefName) {

        return StringUtils.removeStart(resolveRefName, "#/components/schemas/");

    }

    @NotNull
    public static String newDomainId(final String className) {
        return DOMAIN + NamingUtils.toID(className);
    }

    public static @NotNull String getDataType(@NotNull String className, boolean isArray) {
        if (isArray) {
            return new StringBuilder().append(NamingUtils.toTitleCase(className)).append("[]").toString();
        }

        return NamingUtils.toTitleCase(className);
    }


    public static @NotNull String getOperationName(final PathItem.HttpMethod method,
                                                   io.swagger.v3.oas.models.@NotNull Operation operation) {
        return toTitleCase(method + "_" + operation.getOperationId());
    }

    public static @NotNull String getInterfaceName(@Nullable List<String> tags,
                                                   io.swagger.v3.oas.models.@NotNull Operation operation, @NotNull String uri) {
        String interfaceName;
        if (tags != null && !tags.isEmpty()) {
            interfaceName = NamingUtils.toTitleCase(tags.get(0).replaceAll(" ", ""));
        } else if (StringUtils.isNotEmpty(operation.getOperationId())) {
            interfaceName = NamingUtils.toTitleCase(operation.getOperationId());
        } else {
            interfaceName = NamingUtils.toTitleCase(uri.replaceAll("\\{", "").replaceAll("}", "").replaceAll("\\\\", ""));
        }

        return interfaceName + "Resource";
    }

}
