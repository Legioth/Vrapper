package org.vaadin.vrapper.model.codegen;

public class OneLiner extends Snippet {

	public OneLiner(String code, Object... args) {
		super(code, args);
	}

	@Override
	public void writeSnippet(SourceWriter w) {
		super.writeSnippet(w);
		w.println();
	}

}
