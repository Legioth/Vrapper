package org.vaadin.vrapper.model.codegen;

import org.vaadin.vrapper.model.reflect.ApiType;

public interface ImportResolver {

	public String resolveImport(ApiType type);

}
