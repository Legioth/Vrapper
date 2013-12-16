package org.vaadin.vrapper.model.codegen;

import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.shared.ui.Connect;

public class ConnectorCodeGenerator extends RpcHandlerCodeGenerator {

	private MethodCode initMethod;
	private ClassType componentType;
	private MethodCode stateChangeMethod;

	public ConnectorCodeGenerator(CodeConfiguration configuration,
			ClassType componentType) {
		super(configuration);
		this.componentType = componentType;
	}

	public void addInitSnippet(SnippetGenerator snippetGenerator) {
		if (initMethod == null) {
			initMethod = addMethod(getTypeSource().getVoid(), "init");
			initMethod.addImplementationSnippet(new OneLiner("super.init();"));
			initMethod.setOverride(true);
		}

		initMethod.addImplementationSnippet(snippetGenerator);
	}

	@Override
	protected void writeClassAnnotations(SourceWriter w) {
		if (!w.isPreview()) {
			// Always use fully qualified name
			w.println("@%s(%s.class)", getTypeSource().getType(Connect.class),
					componentType.getClassName());
		}
		super.writeClassAnnotations(w);
	}

	@Override
	protected void ensureRpcInit() {
		addInitSnippet(new SnippetGenerator() {
			@Override
			public void writeSnippet(SourceWriter w) {
				writeRpcInit(w);
			}
		});
	}

	public void addStateChangeSnippet(SnippetGenerator snippetGenerator) {
		if (stateChangeMethod == null) {
			stateChangeMethod = addMethod(
					getTypeSource().getVoid(),
					"onStateChange",
					getTypeSource().getObjectType(
							"com.vaadin.client.communication.StateChangeEvent"));
			stateChangeMethod.setOverride(true);
			stateChangeMethod.setParameterNames("event");
		}

		stateChangeMethod.addImplementationSnippet(snippetGenerator);
	}

}
