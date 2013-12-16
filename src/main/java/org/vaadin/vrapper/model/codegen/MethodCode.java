package org.vaadin.vrapper.model.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.vaadin.vrapper.model.reflect.ApiType;

public class MethodCode extends ClassMemberCode {

	private List<ApiType> parameterTypes;
	private final List<String> parameterNames;
	private List<SnippetGenerator> snippets = new ArrayList<SnippetGenerator>();
	private boolean constructor;
	private boolean override;

	public MethodCode(ApiType returnType, String name,
			List<ApiType> parameterTypes) {
		super(name, returnType);
		setPublic(true);
		this.parameterTypes = parameterTypes;

		parameterNames = new ArrayList<String>(parameterTypes.size());
		for (int i = 0; i < parameterTypes.size(); i++) {
			parameterNames.add("p" + i);
		}
	}

	public List<String> getParameterNames() {
		return Collections.unmodifiableList(parameterNames);
	}

	public void setParameterNames(List<String> parameterNames) {
		if (parameterTypes.size() != parameterNames.size()) {
			throw new IllegalArgumentException(
					"Invalid number of parameter names: "
							+ parameterNames.size() + ", expected "
							+ parameterTypes.size());
		}

		this.parameterNames.clear();
		this.parameterNames.addAll(parameterNames);
	}

	@Override
	public void writeSnippet(SourceWriter w) {
		if (isOverride() && !w.isPreview()) {
			w.println("@Override");
		}
		super.writeBaseDeclaration(w);

		w.print("(");
		for (int i = 0; i < parameterTypes.size(); i++) {
			if (i != 0) {
				w.print(", ");
			}
			ApiType type = parameterTypes.get(i);
			w.print("%s %s", type, parameterNames.get(i));
		}
		w.print(")");

		if (!snippets.isEmpty()) {
			w.println(" { ");
			w.indent();

			for (SnippetGenerator snippet : snippets) {
				snippet.writeSnippet(w);
			}

			w.outdent();
			w.println("}");
		} else {
			w.println(";");
		}
	}

	public void addImplementationSnippet(SnippetGenerator snippet) {
		snippets.add(snippet);
	}

	public void setConstructor(boolean constructor) {
		this.constructor = constructor;
	}

	public boolean isConstructor() {
		return constructor;
	}

	@Override
	protected boolean typeInDeclaration() {
		return !isConstructor();
	}

	public ApiType getReturnType() {
		return super.getType();
	}

	public List<ApiType> getParameterTypes() {
		return Collections.unmodifiableList(parameterTypes);
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public boolean isOverride() {
		return override;
	}

	public void setParameterNames(String... names) {
		setParameterNames(Arrays.asList(names));
	}

}
