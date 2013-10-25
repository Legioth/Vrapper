package org.vaadin.vrapper.model.reflect;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ApiMethod {

	private final Method method;
	private final ApiType declaringType;

	public ApiMethod(ApiType declaringType, Method method) {
		this.declaringType = declaringType;
		this.method = method;
	}

	public List<ApiType> getParameterTypes() {
		Type[] argumentTypes = method.getArgumentTypes();
		List<ApiType> parameterTypes = new ArrayList<ApiType>(
				argumentTypes.length);
		for (Type type : argumentTypes) {
			parameterTypes.add(declaringType.getTypeSource()
					.getTypeByInternalName(type.getDescriptor()));
		}

		return parameterTypes;
	}

	public ApiType getDeclaringType() {
		return declaringType;
	}

	@Override
	public String toString() {
		return method.toString();
	}

	public String getName() {
		return method.getName();
	}

	public ApiType getReturnType() {
		Type returnType = method.getReturnType();
		return declaringType.getTypeSource().getTypeByInternalName(
				returnType.getDescriptor());
	}

	public String getDescriptor() {
		return method.getDescriptor();
	}

	public String getSourceString() {
		StringBuilder b = new StringBuilder();

		if (method.getName().equals("<init>")) {
			b.append("new ").append(getDeclaringType().getSimpleName());
		} else {
			b.append(getReturnType().getSimpleName()).append(' ')
					.append(getName());
		}

		b.append("(");
		List<ApiType> parameterTypes = getParameterTypes();
		for (int i = 0; i < parameterTypes.size(); i++) {
			if (i != 0) {
				b.append(", ");
			}
			b.append(parameterTypes.get(i).getSimpleName());
		}
		b.append(")");
		return b.toString();
	}

	public boolean allParametersHasSerializationSupport() {
		List<ApiType> parameterTypes = getParameterTypes();
		for (ApiType apiType : parameterTypes) {
			if (!apiType.hasSerializationSupport()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((declaringType == null) ? 0 : declaringType.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ApiMethod other = (ApiMethod) obj;
		if (declaringType == null) {
			if (other.declaringType != null) {
				return false;
			}
		} else if (!declaringType.equals(other.declaringType)) {
			return false;
		}
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		return true;
	}

}
