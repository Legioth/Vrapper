package org.vaadin.vrapper.model.codegen;

public class Snippet implements SnippetGenerator {

	private String code;
	private Object[] args;

	public Snippet(String code, Object... args) {
		this.code = code;
		this.args = args;
	}

	@Override
	public void writeSnippet(SourceWriter w) {
		w.print(code, args);
	}

}