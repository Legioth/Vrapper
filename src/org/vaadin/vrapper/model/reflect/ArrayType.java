package org.vaadin.vrapper.model.reflect;

import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.Type;

public class ArrayType extends ApiType {

    public ArrayType(Type type, TypeSource typeSource) {
        super(typeSource, type);
    }

    public int getDimensions() {
        return getType().getDimensions();
    }

    @Override
    public Collection<ApiMethod> getMethods() {
        return Collections.emptyList();
    }

    public ApiType getElementType() {
        return getTypeSource().getTypeByInternalName(
                getType().getElementType().getDescriptor());
    }

    @Override
    public String getClassName() {
        return addBrackets(getElementType().getClassName());
    }

    public String addBrackets(String className) {
        StringBuilder builder = new StringBuilder(className);

        int dimensions = getDimensions();
        for (int i = 0; i < dimensions; i++) {
            builder.append("[]");
        }

        return builder.toString();
    }

    @Override
    public String getSimpleName() {
        return addBrackets(getElementType().getSimpleName());
    }

}
