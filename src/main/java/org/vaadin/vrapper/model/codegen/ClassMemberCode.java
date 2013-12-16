package org.vaadin.vrapper.model.codegen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.vaadin.vrapper.model.reflect.ApiType;

public abstract class ClassMemberCode implements SnippetGenerator {

	private boolean isPublic = false;
	private String name;
	private ApiType type;

	private Map<Class<? extends Annotation>, Annotation> annotations = new LinkedHashMap<Class<? extends Annotation>, Annotation>();
	private boolean includeInPreview = true;

	public ClassMemberCode(String name, ApiType type) {
		this.name = name;
		this.type = type;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public void writeBaseDeclaration(SourceWriter w) {
		for (Annotation annotation : annotations.values()) {
			writeAnnotation(w, annotation);
		}

		w.print(buildModifiers());
		if (typeInDeclaration()) {
			w.print("%s %s", type, name);
		} else {
			w.print("%s", name);
		}
	}

	protected boolean typeInDeclaration() {
		return true;
	}

	private void writeAnnotation(SourceWriter w, Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation
				.annotationType();
		w.print("@%s", type.getTypeSource().getType(annotationType));

		LinkedHashMap<String, Object> annotationParams = new LinkedHashMap<String, Object>();
		Method[] declaredMethods = annotationType.getDeclaredMethods();
		for (Method method : declaredMethods) {
			Object defaultValue = method.getDefaultValue();
			try {
				Object value = method.invoke(annotation);
				if (defaultValue == null || !value.equals(defaultValue)) {
					annotationParams.put(method.getName(), value);
				}
			} catch (Exception e) {
				throw new RuntimeException("Can't process " + annotation + "."
						+ method.getName());
			}
		}
		if (!annotationParams.isEmpty()) {
			w.print("(");
			if (annotationParams.size() == 1
					&& annotationParams.containsKey("value")) {
				w.print(encodeAnnotationValue(annotationParams.get("value")));
			} else {
				Set<Entry<String, Object>> entrySet = annotationParams
						.entrySet();
				boolean first = true;
				for (Entry<String, Object> entry : entrySet) {
					if (!first) {
						w.print(", ");
					}
					first = false;

					String annotationValue = encodeAnnotationValue(entry
							.getValue());
					w.print("%s = %s", entry.getKey(), annotationValue);
				}
			}
			w.print(")");
		}

		w.println();
	}

	private String encodeAnnotationValue(Object value) {
		if (value instanceof String) {
			String stringValue = (String) value;
			return '"' + AbstractCodeGenerator.escape(stringValue) + '"';
		} else {
			throw new RuntimeException("Unsupported annotation value: " + value);
		}
	}

	private String buildModifiers() {
		StringBuilder b = new StringBuilder();

		if (isPublic) {
			b.append("public ");
		} else {
			b.append("private ");
		}

		return b.toString();
	}

	public void addAnnotation(Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation
				.annotationType();
		if (annotations.containsKey(annotation)) {
			throw new IllegalStateException("Field " + name
					+ " already has an " + annotationType + " annotation");
		}

		annotations.put(annotationType, annotation);
	}

	public boolean isIncludeInPreview() {
		return includeInPreview;
	}

	public void setIncludeInPreview(boolean includeInPreview) {
		this.includeInPreview = includeInPreview;
	}

	public ApiType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

}
