package org.vaadin.vrapper.model.codegen;

import org.vaadin.vrapper.model.reflect.ApiType;

public class FieldCode extends ClassMemberCode {
	private String initializationCode;

	public FieldCode(String name, ApiType type) {
		super(name, type);
	}

	public void setInitializationCode(String initializationCode) {
		this.initializationCode = initializationCode;
	}

	@Override
	public void writeSnippet(SourceWriter w) {
		super.writeBaseDeclaration(w);
		if (initializationCode != null && !initializationCode.isEmpty()) {
			w.print(" = %s", initializationCode);
		}
		w.println(";");
	}
}