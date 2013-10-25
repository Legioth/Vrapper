package org.vaadin.vrapper.model.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ArrayType;
import org.vaadin.vrapper.model.reflect.ClassType;
import org.vaadin.vrapper.model.reflect.TypeSource;

public class AbstractCodeGenerator implements ImportResolver {
	private final String className;
	private final String packageName;
	private final ApiType superClass;

	private Map<String, FieldCode> fields = new LinkedHashMap<String, FieldCode>();
	private Map<String, MethodCode> methods = new LinkedHashMap<String, MethodCode>();
	private Map<String, MethodCode> constructors = new LinkedHashMap<String, MethodCode>();

	private boolean isInterface;

	private Map<String, ClassType> imports = new HashMap<String, ClassType>();

	public AbstractCodeGenerator(CodeConfiguration configuration) {
		this.className = configuration.getClassName();
		this.packageName = configuration.getPackageName();
		this.superClass = configuration.getSuperClass();
	}

	public ClassMemberCode addField(String fieldName, ApiType type) {
		if (fields.containsKey(fieldName)) {
			throw new IllegalStateException("There is already a field named "
					+ fieldName);
		}

		FieldCode field = new FieldCode(fieldName, type);
		fields.put(fieldName, field);
		return field;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public ApiType getSuperClass() {
		return superClass;
	}

	public String generateCode(boolean preview) {
		Collection<ClassMemberCode> classMembers = getClassMembers(preview);
		if (preview && classMembers.isEmpty()) {
			return null;
		}

		String indentString = preview ? "  " : "    ";
		SourceWriter w = new SourceWriter(this, preview, indentString);

		writeClassAnnotations(w);

		String type = isInterface() ? "interface" : "class";
		w.println("public %s %s extends %s {", type, getClassName(),
				resolveImport(getSuperClass()));
		w.indent();
		w.println();

		for (ClassMemberCode memberCode : classMembers) {
			memberCode.writeSnippet(w);
			w.println();
		}

		w.outdent();
		w.println("}");

		StringBuilder b = new StringBuilder();
		if (!preview) {
			b.append("package ").append(getPackageName()).append(";\n\n");

			Collection<ClassType> values = imports.values();
			List<String> importStrings = new ArrayList<String>(values.size());
			for (ClassType importType : values) {
				String importPackage = importType.getPackageName();
				if (!importPackage.equals("java.lang")) {
					importStrings.add(importType.getClassName());
				}
			}

			Collections.sort(importStrings);
			for (String importString : importStrings) {
				b.append("import ").append(importString).append(";\n");
			}

			b.append('\n');
		}

		b.append(w.toString());
		return b.toString();
	}

	protected void writeClassAnnotations(SourceWriter w) {

	}

	private Collection<ClassMemberCode> getClassMembers(boolean preview) {
		ArrayList<ClassMemberCode> members = new ArrayList<ClassMemberCode>();

		populateClassMembers(members, fields.values(), preview);
		populateClassMembers(members, constructors.values(), preview);
		populateClassMembers(members, methods.values(), preview);

		return members;
	}

	private void populateClassMembers(Collection<ClassMemberCode> members,
			Collection<? extends ClassMemberCode> values, boolean preview) {
		for (ClassMemberCode classMemberCode : values) {
			if (!preview || classMemberCode.isIncludeInPreview()) {
				members.add(classMemberCode);
			}
		}
	}

	private boolean isInterface() {
		return isInterface;
	}

	public MethodCode addMethod(ApiType returnType, String name,
			List<ApiType> parameterTypes) {
		String key = name + '(' + parameterTypes;
		if (methods.containsKey(key)) {
			throw new IllegalStateException(
					"There's already a method with the signature " + key
							+ ") in " + getClassName());
		}

		MethodCode method = new MethodCode(returnType, name, parameterTypes);
		methods.put(key, method);

		return method;
	}

	public MethodCode addMethod(ApiType returnType, String name,
			ApiType... types) {
		return addMethod(returnType, name, Arrays.asList(types));
	}

	public static String escape(String unescaped) {
		// Implementation borrowed from
		// com.google.gwt.core.ext.Generator.escape(String)
		int extra = 0;
		for (int in = 0, n = unescaped.length(); in < n; ++in) {
			switch (unescaped.charAt(in)) {
			case '\0':
			case '\n':
			case '\r':
			case '\"':
			case '\\':
				++extra;
				break;
			}
		}

		if (extra == 0) {
			return unescaped;
		}

		char[] oldChars = unescaped.toCharArray();
		char[] newChars = new char[oldChars.length + extra];
		for (int in = 0, out = 0, n = oldChars.length; in < n; ++in, ++out) {
			char c = oldChars[in];
			switch (c) {
			case '\0':
				newChars[out++] = '\\';
				c = '0';
				break;
			case '\n':
				newChars[out++] = '\\';
				c = 'n';
				break;
			case '\r':
				newChars[out++] = '\\';
				c = 'r';
				break;
			case '\"':
				newChars[out++] = '\\';
				c = '"';
				break;
			case '\\':
				newChars[out++] = '\\';
				c = '\\';
				break;
			}
			newChars[out] = c;
		}

		return String.valueOf(newChars);
	}

	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	protected TypeSource getTypeSource() {
		return getSuperClass().getTypeSource();
	}

	@Override
	public String resolveImport(ApiType type) {
		if (type instanceof ClassType) {
			ClassType classType = (ClassType) type;

			String simpleName = classType.getSimpleName();
			ClassType importType = imports.get(simpleName);
			if (importType == null) {
				imports.put(simpleName, classType);
				return simpleName;
			}

			if (importType.equals(classType)) {
				return simpleName;
			}
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			ApiType elementType = arrayType.getElementType();
			String resolvedElement = resolveImport(elementType);
			return arrayType.addBrackets(resolvedElement);
		}

		return type.getClassName();
	}

	public ClassType getType() {
		return getTypeSource().getObjectType(
				getPackageName() + "." + getClassName());
	}

	public MethodCode addConstructor(ApiType... parameters) {
		String key = Arrays.toString(parameters);
		if (constructors.containsKey(key)) {
			throw new IllegalStateException(
					"There's already a constructor with the parameters " + key
							+ " in " + getClassName());
		}

		MethodCode constructor = new MethodCode(null, getClassName(),
				Arrays.asList(parameters));
		constructor.setConstructor(true);
		constructors.put(key, constructor);

		return constructor;
	}

}
