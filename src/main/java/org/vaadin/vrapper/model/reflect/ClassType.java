package org.vaadin.vrapper.model.reflect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ClassType extends ApiType {

    private ClassType superType;
    private final Map<String, ApiMethod> methods = new LinkedHashMap<String, ApiMethod>();

    private boolean loaded = false;

    private boolean isPublic;
    private boolean isAbstract;

    public ClassType(Type type, TypeSource typeSource) {
        super(typeSource, type);
    }

    @Override
    public Collection<ApiMethod> getMethods() {
        loadIfNeeded();
        return Collections.unmodifiableCollection(methods.values());
    }

    public ClassType getSuperType() {
        loadIfNeeded();
        return superType;
    }

    private void loadIfNeeded() {
        if (loaded) {
            return;
        }

        InputStream inputStream = null;
        try {
            String name = getType().getClassName().replace('.', '/');
            inputStream = getTypeSource().getResolver().findClassStream(name);
            if (inputStream == null) {
                throw new IOException("Could not find " + name);
            }

            ClassReader classReader = new ClassReader(inputStream);
            classReader.accept(new ClassVisitor(Opcodes.ASM4) {
                @Override
                public MethodVisitor visitMethod(int access, String name,
                        String desc, String signature, String[] exceptions) {
                    if (isPublicNotStatic(access)) {
                        ApiMethod apiMethod = new ApiMethod(ClassType.this,
                                new Method(name, desc));
                        if (name.equals("<init>")) {
                            // Ignoring constructors for now
                        } else {
                            methods.put(name + desc, apiMethod);
                        }
                    }
                    return null;
                }

                private boolean isPublicNotStatic(int access) {
                    return (access & Opcodes.ACC_PUBLIC) != 0
                            && (access & Opcodes.ACC_STATIC) == 0;
                }
            }, ClassReader.SKIP_CODE);
            String superName = classReader.getSuperName();
            isPublic = (classReader.getAccess() & Opcodes.ACC_PUBLIC) != 0;
            isAbstract = (classReader.getAccess() & Opcodes.ACC_ABSTRACT) != 0;
            if (superName != null) {
                superType = (ClassType) getTypeSource().getTypeByInternalName(
                        Type.getObjectType(superName).getDescriptor());
                Collection<ApiMethod> superMethods = superType.getMethods();
                for (ApiMethod apiMethod : superMethods) {
                    methods.put(
                            apiMethod.getName() + apiMethod.getDescriptor(),
                            apiMethod);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            loaded = true;
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String getClassName() {
        return getType().getClassName();
    }

    @Override
    public String getSimpleName() {
        String className = getClassName();
        int lastDot = className.lastIndexOf('.');

        String simpleName;
        if (lastDot == -1) {
            simpleName = className;
        } else {
            simpleName = className.substring(lastDot + 1);
        }

        return simpleName.replace('$', '.');
    }

    public String getPackageName() {
        String internalName = getType().getInternalName();
        int lastSlash = internalName.lastIndexOf('/');
        if (lastSlash == -1) {
            return "";
        } else {
            return internalName.substring(0, lastSlash).replace('/', '.');
        }
    }

    public boolean isPublic() {
        loadIfNeeded();
        return isPublic;
    }

    public boolean isAbstract() {
        loadIfNeeded();
        return isAbstract;
    }

    public boolean isOrExtends(ClassType objectType) {
        if (this.equals(objectType)) {
            return true;
        } else if (getSuperType() == null) {
            return false;
        } else {
            return getSuperType().isOrExtends(objectType);
        }
    }
}
