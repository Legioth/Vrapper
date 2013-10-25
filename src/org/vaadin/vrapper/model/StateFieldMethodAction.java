package org.vaadin.vrapper.model;

import java.lang.annotation.Annotation;
import java.util.List;

import org.vaadin.vrapper.model.codegen.ClassMemberCode;
import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.codegen.MethodCode;
import org.vaadin.vrapper.model.codegen.OneLiner;
import org.vaadin.vrapper.model.codegen.SnippetGenerator;
import org.vaadin.vrapper.model.codegen.SourceWriter;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;

import com.vaadin.shared.annotations.DelegateToWidget;

public class StateFieldMethodAction extends MethodAction {

    private String fieldName;
    private String setterName;

    public StateFieldMethodAction(ApiMethod method) {
        super(method, "Define in shared state");
        fieldName = getPropertyName(method);
        setterName = method.getName();
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getSetterName() {
        return setterName;
    }

    public void setSetterName(String setterName) {
        this.setterName = setterName;
    }

    @Override
    public Evaluation evaluate() {
        ApiMethod method = getMethod();

        List<ApiType> parameterTypes = method.getParameterTypes();

        if (parameterTypes.isEmpty()) {
            return new Evaluation(Status.IMPOSSIBLE,
                    "Must have at least one parameter");
        }

        for (ApiType apiType : parameterTypes) {
            if (!apiType.hasSerializationSupport()) {
                return new Evaluation(Status.IMPOSSIBLE, apiType
                        + " can't be serialized");
            }
        }

        if (!method.getReturnType().getClassName().equals("void")) {
            return new Evaluation(Status.DISCOURAGED,
                    "Doesn't seem like a setter since there is a return type");
        } else if (method.getName().startsWith("set")) {
            if (parameterTypes.size() == 1) {
                return new Evaluation(Status.RECOMMENDED, "");
            } else {
                return new Evaluation(Status.SUPPORTED, "");
            }
        } else {
            return new Evaluation(Status.SUPPORTED, "Not named like a setter");
        }
    }

    public static String getPropertyName(ApiMethod method) {
        String methodName = method.getName();
        if (methodName.startsWith("set") || methodName.startsWith("get")) {
            return Character.toLowerCase(methodName.charAt(3))
                    + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            return Character.toLowerCase(methodName.charAt(2))
                    + methodName.substring(3);
        } else {
            return methodName;
        }
    }

    @Override
    public void writeCode(Code code) {
        final ApiMethod method = getMethod();
        List<ApiType> parameterTypes = method.getParameterTypes();
        if (parameterTypes.size() == 1) {
            writeSingleParameterCode(code, parameterTypes.get(0));
        } else {
            writeMultiParameterCode(code, parameterTypes);
        }
    }

    private void writeMultiParameterCode(Code code,
            final List<ApiType> parameterTypes) {
        ApiMethod method = getMethod();
        final String fieldName = getFieldName();

        MethodCode setter = code.getComponentCode().addMethod(
                method.getReturnType(), getSetterName(), parameterTypes);
        setter.addImplementationSnippet(new OneLiner("%s state = getState();",
                code.getSharedStateCode().getType()));

        for (int i = 0; i < parameterTypes.size(); i++) {
            ApiType apiType = parameterTypes.get(i);
            String paramField = fieldName + i;
            ClassMemberCode field = code.getSharedStateCode().addField(
                    paramField, apiType);
            field.setPublic(true);

            setter.addImplementationSnippet(new OneLiner("state.%s = %s;",
                    paramField, setter.getParameterNames().get(i)));

            MethodCode getter = code.getComponentCode().addMethod(apiType,
                    "get" + fieldName + i);
            getter.addImplementationSnippet(new OneLiner("return %s;",
                    paramField));
        }

        code.getConnectorCode().addStateChangeSnippet(new SnippetGenerator() {
            @Override
            public void writeSnippet(SourceWriter w) {
                w.print("if (");
                for (int i = 0; i < parameterTypes.size(); i++) {
                    if (i != 0) {
                        w.print(" || ");
                    }
                    w.print("event.hasPropertyChanged(\"%s\")", fieldName + i);
                }
                w.println(") {");
                w.indent();

                w.print("getWidget().%s(", getMethod().getName());
                for (int i = 0; i < parameterTypes.size(); i++) {
                    if (i != 0) {
                        w.print(", ");
                    }
                    w.print(fieldName + i);
                }
                w.println(");");

                w.outdent();
                w.print("}");
            }
        });

    }

    private void writeSingleParameterCode(Code code, ApiType type) {
        final String fieldName = getFieldName();

        ClassMemberCode field = code.getSharedStateCode().addField(fieldName,
                type);
        field.setPublic(true);
        field.addAnnotation(new DelegateToWidget() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return DelegateToWidget.class;
            }

            @Override
            public String value() {
                ApiMethod method = getMethod();
                if (fieldName.equals(getPropertyName(method))) {
                    return "";
                } else {
                    return method.getName();
                }
            }
        });

        MethodCode componentSetter = code.getComponentCode().addMethod(
                getTypeSource().getVoid(), getSetterName(), type);
        componentSetter.addImplementationSnippet(new OneLiner("getState()."
                + fieldName + " = " + fieldName + ";"));
        componentSetter.setParameterNames(fieldName);

        MethodCode componentGetter = code.getComponentCode().addMethod(type,
                getGetterName());
        componentGetter.addImplementationSnippet(new OneLiner(
                "return getState(false)." + fieldName + ";"));
    }

    private String getGetterName() {
        ApiType propertyType = getMethod().getParameterTypes().get(0);
        String setterName = getSetterName();
        return getGetterName(propertyType, setterName);
    }

    public static String getGetterName(ApiMethod method) {
        ApiType propertyType = method.getParameterTypes().get(0);
        String setterName = method.getName();
        return getGetterName(propertyType, setterName);
    }

    public static String getGetterName(ApiType propertyType, String setterName) {
        if (setterName.startsWith("set")) {
            String base = setterName.substring(3);
            if (propertyType.getClassName().equals("boolean")) {
                return "is" + base;
            } else {
                return "get" + base;
            }
        } else {
            return setterName;
        }
    }
}
