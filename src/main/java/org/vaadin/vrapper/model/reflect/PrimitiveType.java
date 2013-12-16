package org.vaadin.vrapper.model.reflect;

import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.Type;

public class PrimitiveType extends ApiType {

    public PrimitiveType(Type type, TypeSource typeSource) {
        super(typeSource, type);
    }

    @Override
    public Collection<ApiMethod> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public String getClassName() {
        return getType().getClassName();
    }

    @Override
    public String getSimpleName() {
        return getClassName();
    }

}
