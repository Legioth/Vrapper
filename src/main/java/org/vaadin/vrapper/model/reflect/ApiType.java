package org.vaadin.vrapper.model.reflect;

import java.util.Collection;

import org.objectweb.asm.Type;

public abstract class ApiType {

    private final TypeSource typeSource;
    private final Type type;

    public ApiType(TypeSource typeSource, Type type) {
        this.typeSource = typeSource;
        this.type = type;
    }

    protected Type getType() {
        return type;
    }

    public abstract Collection<ApiMethod> getMethods();

    public abstract String getClassName();

    public abstract String getSimpleName();

    public final TypeSource getTypeSource() {
        return typeSource;
    }

    public final boolean hasSerializationSupport() {
        return getTypeSource().isSerializable(this);
    }

    @Override
    public final String toString() {
        return getType().toString();
    }

}