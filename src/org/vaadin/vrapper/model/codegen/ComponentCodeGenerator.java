package org.vaadin.vrapper.model.codegen;


public class ComponentCodeGenerator extends RpcHandlerCodeGenerator {

	private MethodCode constructor;

	public ComponentCodeGenerator(CodeConfiguration configuration) {
		super(configuration);
	}

	public void addToConstructor(SnippetGenerator snippetGenerator) {
		if (constructor == null) {
			constructor = super.addConstructor();
		}

		constructor.addImplementationSnippet(snippetGenerator);
	}

	@Override
	protected void ensureRpcInit() {
		addToConstructor(new SnippetGenerator() {
			@Override
			public void writeSnippet(SourceWriter w) {
				writeRpcInit(w);
			}
		});
	}

}
