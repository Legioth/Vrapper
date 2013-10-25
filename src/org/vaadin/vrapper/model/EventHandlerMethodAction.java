package org.vaadin.vrapper.model;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.codegen.OneLiner;
import org.vaadin.vrapper.model.codegen.SnippetGenerator;
import org.vaadin.vrapper.model.codegen.SourceWriter;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.shared.communication.ServerRpc;

public class EventHandlerMethodAction extends MethodAction {

    private String rpcInterfaceName;

    private List<CustomMethodParameter> customParameters = new ArrayList<CustomMethodParameter>();

    private ClassType eventHandlerType;

    private ApiMethod handlerMethod;

    private ClassType eventType;

    public EventHandlerMethodAction(ApiMethod method,
            WidgetConfiguration widgetConfiguration) {
        super(method, "Send event to server");
        rpcInterfaceName = widgetConfiguration.getComponentCodeConfiguration()
                .getClassName() + "ServerRpc";

        List<ApiType> parameterTypes = getMethod().getParameterTypes();
        if (parameterTypes.size() == 1
                && parameterTypes.get(0) instanceof ClassType) {
            eventHandlerType = (ClassType) parameterTypes.get(0);

            List<ApiMethod> handlerMethods = new ArrayList<ApiMethod>();

            for (ApiMethod handlerMethod : eventHandlerType.getMethods()) {
                if (!handlerMethod.getDeclaringType().getClassName()
                        .equals("java.lang.Object")) {
                    handlerMethods.add(handlerMethod);
                }
            }

            if (handlerMethods.size() == 1) {
                handlerMethod = handlerMethods.get(0);
                List<ApiType> handlerParameters = handlerMethod
                        .getParameterTypes();
                if (handlerParameters.size() == 1
                        && handlerParameters.get(0) instanceof ClassType) {
                    eventType = (ClassType) handlerParameters.get(0);
                }
            }
        }
    }

    public String getRpcInterfaceName() {
        return rpcInterfaceName;
    }

    public void setRpcInterfaceName(String rpcInterfaceName) {
        this.rpcInterfaceName = rpcInterfaceName;
    }

    @Override
    public Evaluation evaluate() {
        ApiMethod method = getMethod();

        if (method.getParameterTypes().size() != 1) {
            return new Evaluation(Status.IMPOSSIBLE,
                    "Only one parameter supported");
        } else if (eventHandlerType == null || handlerMethod == null) {
            return new Evaluation(Status.IMPOSSIBLE,
                    "Method parameter type must have one abstract method");
        } else if (!handlerMethod.getReturnType().getClassName().equals("void")) {
            return new Evaluation(Status.IMPOSSIBLE,
                    "Handler method must return void");
        } else if (eventType == null) {
            return new Evaluation(Status.DISCOURAGED, "Event type not detected");
        } else if (!method.getReturnType().getClassName()
                .equals("com.google.gwt.event.shared.HandlerRegistration")) {
            return new Evaluation(Status.DISCOURAGED,
                    "Method does not return a HandlerRegistration");
        } else {
            return new Evaluation(Status.SUPPORTED, "");
        }
    }

    @Override
    public void writeCode(Code code) {
        final ApiType handlerType = getMethod().getParameterTypes().get(0);

        List<ApiType> parameterTypes = new ArrayList<ApiType>();
        List<String> parameterNames = new ArrayList<String>();
        for (CustomMethodParameter parameter : getCustomParameters()) {
            parameterTypes.add(parameter.getType());
            parameterNames.add(parameter.getParameterName());
        }

        ClassType rpcType = code.addServerRpcMethod(getRpcInterfaceName(),
                getTypeSource().getType(ServerRpc.class),
                handlerMethod.getName(), parameterTypes, parameterNames,
                new OneLiner("// TODO handle event"));

        final ClassType rpcTypeFinal = rpcType;

        code.getConnectorCode().addInitSnippet(new SnippetGenerator() {
            @Override
            public void writeSnippet(SourceWriter w) {
                w.println("getWidget().%s(new %s() {", getMethod().getName(),
                        handlerType);
                w.indent();

                if (!w.isPreview()) {
                    w.println("@Override");
                }
                if (eventType != null) {
                    w.println("public %s %s(%s event) {",
                            handlerMethod.getReturnType(),
                            handlerMethod.getName(), eventType);
                } else {
                    w.print("public %s %s(", handlerMethod.getReturnType(),
                            handlerMethod.getName());
                    List<ApiType> handlerParameters = handlerMethod
                            .getParameterTypes();
                    for (int i = 0; i < handlerParameters.size(); i++) {
                        if (i != 0) {
                            w.print(", ");
                        }
                        w.print("%s p%d", handlerParameters.get(i),
                                Integer.valueOf(i));
                    }
                    w.println(") {");
                }
                w.indent();

                w.print("getRpcProxy(%s.class).%s(", rpcTypeFinal,
                        handlerMethod.getName());
                List<CustomMethodParameter> parameterGenerators = getCustomParameters();
                boolean splitLines = parameterGenerators.size() >= 2;
                if (splitLines) {
                    w.println();
                    w.indent();
                }
                for (int i = 0; i < parameterGenerators.size(); i++) {
                    if (i != 0) {
                        w.print(", ");
                        if (splitLines) {
                            w.println();
                        }
                    }
                    parameterGenerators.get(i).getGenerator().writeSnippet(w);
                }
                if (splitLines) {
                    w.println();
                    w.outdent();
                }

                w.println(");");

                w.outdent();
                w.println("}");

                w.outdent();
                w.println("});");
            }
        });
    }

    public ClassType getEventType() {
        return eventType;
    }

    public List<CustomMethodParameter> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(List<CustomMethodParameter> customParameters) {
        this.customParameters = customParameters;
    }

    public ApiMethod getHandlerMethod() {
        return handlerMethod;
    }
}
