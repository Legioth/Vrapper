package org.vaadin.vrapper.model.codegen;

import org.vaadin.vrapper.model.reflect.ApiType;

public class CodeConfiguration {
	private String className;
	private String packageName;
	private ApiType superClass;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public ApiType getSuperClass() {
		return superClass;
	}

	public void setSuperClass(ApiType superClass) {
		this.superClass = superClass;
	}

}
