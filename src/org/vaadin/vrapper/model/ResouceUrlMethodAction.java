package org.vaadin.vrapper.model;

import java.util.List;

import org.vaadin.vrapper.model.codegen.AbstractCodeGenerator;
import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.codegen.MethodCode;
import org.vaadin.vrapper.model.codegen.OneLiner;
import org.vaadin.vrapper.model.codegen.SnippetGenerator;
import org.vaadin.vrapper.model.codegen.SourceWriter;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;

import com.vaadin.server.Resource;

public class ResouceUrlMethodAction extends MethodAction {

	private String resourceKey;

	private String setterName;

	public ResouceUrlMethodAction(ApiMethod method) {
		super(method, "Set using Resource");
		resourceKey = StateFieldMethodAction.getPropertyName(method);
		setterName = method.getName();
	}

	public String getResourceKey() {
		return resourceKey;
	}

	public void setResourceKey(String resourceKey) {
		this.resourceKey = resourceKey;
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

		if (parameterTypes.size() != 1) {
			return new Evaluation(Status.IMPOSSIBLE,
					"There's more than 1 parameter");
		}

		ApiType param = parameterTypes.get(0);
		if (!param.getClassName().equals(String.class.getName())) {
			return new Evaluation(Status.IMPOSSIBLE,
					"Only String parameter supported");
		} else if (!method.getReturnType().getClassName().equals("void")) {
			return new Evaluation(Status.DISCOURAGED,
					"Returned value will be ignored");
		} else {
			return new Evaluation(Status.SUPPORTED, "");
		}
	}

	@Override
	public void writeCode(Code code) {
		final ApiMethod method = getMethod();
		ApiType voidType = method.getReturnType();
		ApiType resourceType = getTypeSource().getType(Resource.class);

		final String resourceKey = AbstractCodeGenerator
				.escape(getResourceKey());

		MethodCode setter = code.getComponentCode().addMethod(voidType,
				getSetterName(), resourceType);
		setter.setParameterNames("resource");
		setter.addImplementationSnippet(new OneLiner(
				"setResource(\"%s\", resource);", resourceKey));

		MethodCode getter = code.getComponentCode().addMethod(
				resourceType,
				StateFieldMethodAction.getGetterName(resourceType,
						getSetterName()));
		getter.addImplementationSnippet(new OneLiner(
				"return getResource(\"%s\");", resourceKey));

		code.getConnectorCode().addStateChangeSnippet(new SnippetGenerator() {
			@Override
			public void writeSnippet(SourceWriter w) {
				w.println("if (event.hasPropertyChanged(\"resources\") {");
				w.indent();

				w.println("getWidget().%s(getResourceUrl(\"%s\"));",
						method.getName(), resourceKey);

				w.outdent();
				w.println("}");
			}
		});
	}

}
