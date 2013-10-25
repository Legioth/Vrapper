package org.vaadin.vrapper.model;

import org.vaadin.vrapper.model.codegen.Snippet;
import org.vaadin.vrapper.model.reflect.ApiMethod;

public class QualifiedMethodParameter extends CustomMethodParameter {
	public QualifiedMethodParameter(String qualifier, ApiMethod method) {
		super(method.getReturnType(), StateFieldMethodAction
				.getPropertyName(method), new Snippet("%s.%s()", qualifier,
				method.getName()), qualifier + "." + method.getName() + "()");
	}
}
