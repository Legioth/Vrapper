package org.vaadin.vrapper.model;

import org.vaadin.vrapper.model.codegen.SnippetGenerator;
import org.vaadin.vrapper.model.reflect.ApiType;

public class CustomMethodParameter {

	private ApiType type;
	private SnippetGenerator generator;
	private String description;
	private String parameterName;

	public CustomMethodParameter(ApiType type, String parameterName,
			SnippetGenerator generator, String description) {
		this.type = type;
		this.parameterName = parameterName;
		this.generator = generator;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getParameterName() {
		return parameterName;
	}

	public ApiType getType() {
		return type;
	}

	public SnippetGenerator getGenerator() {
		return generator;
	}

}
