package org.vaadin.vrapper.model.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.vrapper.model.WidgetConfiguration;
import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;
import org.vaadin.vrapper.model.reflect.TypeSource.Primitive;

public class Code {
	private WidgetConfiguration configuration;

	private Map<String, AbstractCodeGenerator> classes = new LinkedHashMap<String, AbstractCodeGenerator>();

	public Code(WidgetConfiguration configuration) {
		this.configuration = configuration;
	}

	public ConnectorCodeGenerator getConnectorCode() {
		CodeConfiguration codeConfiguration = configuration
				.getConnectorCodeConfiguration();
		String className = codeConfiguration.getPackageName() + "."
				+ codeConfiguration.getClassName();

		ConnectorCodeGenerator generator = (ConnectorCodeGenerator) classes
				.get(className);
		if (generator == null) {
			generator = new ConnectorCodeGenerator(codeConfiguration,
					getComponentCode().getType());
			classes.put(className, generator);

			final MethodCode getWidget = generator.addMethod(
					configuration.getWidget(), "getWidget");
			getWidget
					.addImplementationSnippet(new OneLiner(
							"return (%s) super.getWidget();", configuration
									.getWidget()));
			getWidget.setOverride(true);
			getWidget.setIncludeInPreview(false);
		}

		return generator;
	}

	public SharedStateCodeGenerator getSharedStateCode() {
		CodeConfiguration codeConfiguration = configuration
				.getStateCodeConfiguration();
		String className = codeConfiguration.getPackageName() + "."
				+ codeConfiguration.getClassName();

		SharedStateCodeGenerator stateGenerator = (SharedStateCodeGenerator) classes
				.get(className);
		if (stateGenerator == null) {
			stateGenerator = new SharedStateCodeGenerator(codeConfiguration);
			classes.put(className, stateGenerator);

			final ClassType stateType = stateGenerator.getType();

			for (AbstractCodeGenerator g : Arrays.asList(getConnectorCode(),
					getComponentCode())) {
				MethodCode getStateMethod = g.addMethod(
						stateGenerator.getType(), "getState");
				getStateMethod.addImplementationSnippet(new OneLiner(
						"return (%s) super.getState();", stateType));
				getStateMethod.setOverride(true);
				getStateMethod.setIncludeInPreview(false);
			}

			MethodCode getStateMethod = getComponentCode().addMethod(
					stateGenerator.getType(),
					"getState",
					stateGenerator.getTypeSource().getPrimitiveType(
							Primitive.BOOLEAN));
			getStateMethod.setParameterNames("markAsDirty");
			getStateMethod.addImplementationSnippet(new OneLiner(
					"return (%s) super.getState(markAsDirty);", stateType));
			getStateMethod.setIncludeInPreview(false);
			getStateMethod.setOverride(true);

		}

		return stateGenerator;
	}

	public ComponentCodeGenerator getComponentCode() {
		CodeConfiguration codeConfiguration = configuration
				.getComponentCodeConfiguration();
		String className = codeConfiguration.getPackageName() + "."
				+ codeConfiguration.getClassName();

		ComponentCodeGenerator generator = (ComponentCodeGenerator) classes
				.get(className);
		if (generator == null) {
			generator = new ComponentCodeGenerator(codeConfiguration);
			classes.put(className, generator);

		}
		return generator;
	}

	public AbstractCodeGenerator addClass(String packageName, String className,
			ApiType superClass) {
		String key = packageName + "." + className;
		if (classes.containsKey(key)) {
			throw new IllegalStateException(key + " has already been defined");
		}

		CodeConfiguration configuration = new CodeConfiguration();
		configuration.setPackageName(packageName);
		configuration.setClassName(className);
		configuration.setSuperClass(superClass);
		AbstractCodeGenerator codeGenerator = new AbstractCodeGenerator(
				configuration);

		classes.put(key, codeGenerator);

		return codeGenerator;
	}

	public List<AbstractCodeGenerator> getClasses() {
		List<AbstractCodeGenerator> classes = new ArrayList<AbstractCodeGenerator>();
		classes.addAll(this.classes.values());
		return classes;
	}

	public String getSharedPackage() {
		return configuration.getStateCodeConfiguration().getPackageName();
	}

	public ClassType addClientRpcMethod(String interfaceName,
			ApiType superIntrface, String methodName,
			List<ApiType> methodParameters, List<String> parameterNames,
			SnippetGenerator handlerSnippet) {
		return addRpcMethod(interfaceName, superIntrface, methodName,
				methodParameters, parameterNames, handlerSnippet,
				getConnectorCode());
	}

	public ClassType addServerRpcMethod(String interfaceName,
			ApiType superIntrface, String methodName,
			List<ApiType> methodParameters, List<String> parameterNames,
			SnippetGenerator handlerSnippet) {
		return addRpcMethod(interfaceName, superIntrface, methodName,
				methodParameters, parameterNames, handlerSnippet,
				getComponentCode());
	}

	private ClassType addRpcMethod(String interfaceName, ApiType superIntrface,
			String methodName, List<ApiType> methodParameters,
			List<String> parameterNames, SnippetGenerator handlerSnippet,
			RpcHandlerCodeGenerator rpcHandlerCodeGenerator) {
		String classKey = getSharedPackage() + "." + interfaceName;
		AbstractCodeGenerator interfaceGenerator = classes.get(classKey);
		if (interfaceGenerator != null) {
			// super class
			if (!interfaceGenerator.getSuperClass().equals(superIntrface)) {
				throw new IllegalStateException(
						"There is already a class named "
								+ interfaceName
								+ " with conflicting super type: "
								+ superIntrface.getClassName()
								+ " vs. "
								+ interfaceGenerator.getSuperClass()
										.getClassName());
			}
		} else {
			CodeConfiguration codeConf = new CodeConfiguration();
			codeConf.setClassName(interfaceName);
			codeConf.setPackageName(getSharedPackage());
			codeConf.setSuperClass(superIntrface);
			interfaceGenerator = new AbstractCodeGenerator(codeConf);
			interfaceGenerator.setInterface(true);
			classes.put(classKey, interfaceGenerator);
		}

		MethodCode methodDeclaration = interfaceGenerator.addMethod(
				superIntrface.getTypeSource().getVoid(), methodName,
				methodParameters);
		if (parameterNames != null) {
			methodDeclaration.setParameterNames(parameterNames);
		}

		rpcHandlerCodeGenerator.addRpcInit(interfaceGenerator.getType(),
				methodDeclaration, handlerSnippet);

		return interfaceGenerator.getType();
	}
}
