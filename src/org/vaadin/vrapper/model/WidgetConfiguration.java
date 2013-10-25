package org.vaadin.vrapper.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vaadin.vrapper.model.codegen.AbstractCodeGenerator;
import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.codegen.CodeConfiguration;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.shared.AbstractComponentState;
import com.vaadin.ui.AbstractComponent;

public class WidgetConfiguration {
    private static final Set<String> ignoreMethodsFrom = new HashSet<String>(
            Arrays.asList("java.lang.Object",
                    "com.google.gwt.user.client.ui.UIObject",
                    "com.google.gwt.user.client.ui.Widget"));

    private ClassType widgetType;

    private Map<ApiMethod, MethodConfiguration> methodConfigurations = new LinkedHashMap<ApiMethod, MethodConfiguration>();

    private CodeConfiguration connectorCodeConfig;

    private CodeConfiguration stateCodeConfig;

    private CodeConfiguration componentCodeConfig;

    public WidgetConfiguration(ClassType widgetType) {
        this.widgetType = widgetType;

        Collection<ApiMethod> methods = widgetType.getMethods();
        for (final ApiMethod method : methods) {
            if (ignoreMethodsFrom.contains(method.getDeclaringType()
                    .getClassName())) {
                continue;
            }

            MethodConfiguration methodConfiguration = new MethodConfiguration(
                    this, method);
            List<MethodAction> actions = methodConfiguration.getActions();

            if (!actions.isEmpty()) {
                methodConfigurations.put(method, methodConfiguration);
            }
        }
    }

    public Collection<MethodConfiguration> getMethodConfigurations() {
        return Collections
                .unmodifiableCollection(methodConfigurations.values());
    }

    public ClassType getWidget() {
        return widgetType;
    }

    public CodeConfiguration getConnectorCodeConfiguration() {
        if (connectorCodeConfig == null) {
            connectorCodeConfig = new CodeConfiguration();
            connectorCodeConfig.setClassName(getComponentCodeConfiguration()
                    .getClassName() + "Connector");
            connectorCodeConfig.setPackageName(getWidget().getPackageName());
            connectorCodeConfig.setSuperClass(getWidget().getTypeSource()
                    .getObjectType(
                            "com.vaadin.client.ui.AbstractComponentConnector"));
        }
        return connectorCodeConfig;
    }

    public CodeConfiguration getStateCodeConfiguration() {
        if (stateCodeConfig == null) {
            stateCodeConfig = new CodeConfiguration();
            stateCodeConfig.setClassName(getComponentCodeConfiguration()
                    .getClassName() + "State");
            stateCodeConfig.setPackageName(getComponentCodeConfiguration()
                    .getPackageName() + ".shared");
            stateCodeConfig.setSuperClass(getWidget().getTypeSource().getType(
                    AbstractComponentState.class));
        }
        return stateCodeConfig;
    }

    public CodeConfiguration getComponentCodeConfiguration() {
        if (componentCodeConfig == null) {
            componentCodeConfig = new CodeConfiguration();

            String className = getWidget().getSimpleName();
            if (className.length() >= 2 && className.charAt(0) == 'V'
                    && Character.isUpperCase(className.charAt(1))) {
                className = className.substring(1);
            }

            if (className.endsWith("Widget")) {
                className = className.substring(0, className.length()
                        - "Widget".length());
            }

            componentCodeConfig.setClassName(className);

            String packageName = getWidget().getPackageName();
            int clientPackageIndex = packageName.indexOf(".client");
            if (clientPackageIndex != -1) {
                packageName = packageName.substring(0, clientPackageIndex);
            }
            componentCodeConfig.setPackageName(packageName);

            componentCodeConfig.setSuperClass(getWidget().getTypeSource()
                    .getType(AbstractComponent.class));
        }
        return componentCodeConfig;
    }

    public String buildFullSource() {
        Code code = new Code(this);

        Collection<MethodConfiguration> configurations = getMethodConfigurations();
        for (MethodConfiguration methodConfiguration : configurations) {
            MethodAction selectedAction = methodConfiguration
                    .getSelectedAction();
            if (selectedAction != null) {
                selectedAction.writeCode(code);
            }
        }

        StringBuilder b = new StringBuilder();
        List<AbstractCodeGenerator> classes = code.getClasses();
        boolean first = true;
        for (AbstractCodeGenerator codeGenerator : classes) {
            if (!first) {
                b.append("\n\n");
            }
            first = false;
            String classCode = codeGenerator.generateCode(false);
            b.append(classCode);
        }

        return b.toString();
    }

}
