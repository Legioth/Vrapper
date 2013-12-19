package org.vaadin.vrapper.model.reflect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;
import org.vaadin.vrapper.model.StateFieldMethodAction;

public class TypeSource {

    public static enum Primitive {
        VOID(Type.VOID_TYPE), BOOLEAN(Type.BOOLEAN_TYPE), BYTE(Type.BYTE_TYPE), CHAR(
                Type.CHAR_TYPE), SHORT(Type.SHORT_TYPE), INT(Type.INT_TYPE), LONG(
                Type.LONG_TYPE), FLOAT(Type.FLOAT_TYPE), DOUBLE(
                Type.DOUBLE_TYPE);

        private Type type;

        Primitive(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }

    private static final Collection<Class<?>> knownSerializable = Collections
            .unmodifiableCollection(Arrays.asList(String.class, List.class,
                    Set.class, Map.class, Boolean.class, Byte.class,
                    Character.class, Short.class, Integer.class, Long.class,
                    Float.class, Double.class, Date.class));

    private static final Collection<String> knownUnserializable = Collections
            .unmodifiableCollection(Arrays
                    .asList("com.vaadin.client.ApplicationConnection"));

    private final Resolver resolver;
    private final Map<Type, ApiType> cache = new HashMap<Type, ApiType>();

    private Map<ApiType, Boolean> serialiableCache = new HashMap<ApiType, Boolean>();

    public TypeSource(Resolver resolver) {
        this.resolver = resolver;

        for (Class<?> c : knownSerializable) {
            serialiableCache.put(getType(c), Boolean.TRUE);
        }

        for (String name : knownUnserializable) {
            serialiableCache.put(getObjectType(name), Boolean.FALSE);
        }
    }

    public Resolver getResolver() {
        return resolver;
    }

    public ClassType getType(Class<?> type) {
        return (ClassType) getType(Type.getType(type));
    }

    public ClassType getObjectType(String fullyQualifiedName) {
        return (ClassType) getType(Type.getObjectType(fullyQualifiedName
                .replace('.', '/')));
    }

    public ApiType getTypeByInternalName(String name) {
        return getType(Type.getType(name));
    }

    public PrimitiveType getPrimitiveType(Primitive primitive) {
        return (PrimitiveType) getType(primitive.getType());
    }

    public PrimitiveType getVoid() {
        return getPrimitiveType(Primitive.VOID);
    }

    private ApiType getType(Type type) {
        ApiType apiType = cache.get(type);
        if (apiType == null) {
            apiType = createType(type);
            cache.put(type, apiType);
        }

        return apiType;
    }

    private ApiType createType(Type type) {
        if (type.getSort() == Type.OBJECT) {
            return new ClassType(type, this);
        } else if (type.getSort() == Type.ARRAY) {
            return new ArrayType(type, this);
        } else {
            return new PrimitiveType(type, this);
        }
    }

    public boolean isSerializable(ApiType type) {
        Boolean trivialSerializability = checkTrivialSerializability(type);
        if (trivialSerializability != null) {
            return trivialSerializability.booleanValue();
        } else {
            boolean serializability = resolveSerializability(type);
            serialiableCache.put(type, Boolean.valueOf(serializability));
            return serializability;
        }
    }

    private Boolean checkTrivialSerializability(ApiType type) {
        if (type instanceof PrimitiveType) {
            return Boolean.TRUE;
        } else {
            return serialiableCache.get(type);
        }
    }

    private boolean resolveSerializability(final ApiType type) {
        HashSet<ApiType> checkedTypes = new HashSet<ApiType>();
        HashSet<ApiType> queue = new HashSet<ApiType>();

        queue.add(type);
        while (!queue.isEmpty()) {
            Iterator<ApiType> iterator = queue.iterator();
            ApiType currentType = iterator.next();
            iterator.remove();

            if (currentType instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) currentType;
                currentType = arrayType.getElementType();
            }

            Boolean trivialSerializability = checkTrivialSerializability(currentType);
            if (trivialSerializability == Boolean.FALSE) {
                // TODO could mark some types as not serializable if there would
                // be a dependency graph to follow
                serialiableCache.put(currentType, Boolean.FALSE);
                return false;
            } else if (trivialSerializability == null) {
                if (!checkedTypes.add(currentType)) {
                    continue;
                }

                // resolve
                if (currentType instanceof ClassType) {
                    ClassType classType = (ClassType) currentType;
                    if (classType
                            .isOrExtends(getObjectType("com.google.gwt.core.client.JavaScriptObject"))) {
                        serialiableCache.put(currentType, Boolean.FALSE);
                        return false;
                    } else if (classType
                            .isOrExtends(getObjectType("java.lang.Enum"))) {
                        // Enum is basically always serializable (unless it has
                        // JSO fields)
                        continue;
                    }
                    Collection<ApiMethod> methods = classType.getMethods();
                    HashSet<String> setters = new HashSet<String>();

                    for (ApiMethod method : methods) {
                        if (method.getName().startsWith("set")
                                && method.getParameterTypes().size() == 1) {
                            setters.add(StateFieldMethodAction
                                    .getPropertyName(method));
                        }
                    }

                    if (setters.isEmpty()) {
                        // Clearly not a bean
                        serialiableCache.put(currentType, Boolean.FALSE);
                        return false;
                    }

                    boolean hasProperties = false;

                    // Check the type of getters that have setters
                    for (ApiMethod method : methods) {
                        String name = method.getName();
                        List<ApiType> parameterTypes = method
                                .getParameterTypes();
                        if ((name.startsWith("get") || name.startsWith("is"))
                                && parameterTypes.isEmpty()
                                && setters.remove(StateFieldMethodAction
                                        .getPropertyName(method))) {
                            ApiType returnType = method.getReturnType();
                            if (!returnType.equals(getVoid())) {
                                queue.add(returnType);
                                hasProperties = true;
                            }
                        }
                    }

                    if (!hasProperties) {
                        serialiableCache.put(currentType, Boolean.FALSE);
                        return false;
                    }
                } else {
                    throw new RuntimeException(
                            "Only ClassType expected here, but got : "
                                    + currentType.getClass());
                }
            }
        }

        // Queue empty without finding any problems -> all checked types OK
        for (ApiType apiType : checkedTypes) {
            serialiableCache.put(apiType, Boolean.TRUE);
        }

        return true;
    }

}
