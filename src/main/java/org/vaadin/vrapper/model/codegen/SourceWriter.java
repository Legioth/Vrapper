package org.vaadin.vrapper.model.codegen;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.vaadin.vrapper.model.reflect.ApiType;

/**
 * Borrowed from GWT's com.google.gwt.user.rebind.StringSourceWriter
 */
public class SourceWriter {

	private final StringWriter buffer = new StringWriter();
	private int indentLevel = 0;
	private String indentPrefix = "";
	private boolean needsIndent;
	private final PrintWriter out = new PrintWriter(buffer);
	private ImportResolver importResolver;
	private boolean preview;
	private final String indentString;

	public SourceWriter(ImportResolver importResolver, boolean preview,
			String indentString) {
		this.importResolver = importResolver;
		this.preview = preview;
		this.indentString = indentString;
	}

	public boolean isPreview() {
		return preview;
	}

	public String resolveImport(ApiType type) {
		if (isPreview()) {
			return type.getSimpleName();
		}
		if (importResolver != null) {
			return importResolver.resolveImport(type);
		} else {
			return type.getClassName();
		}
	}

	public void beginJavaDocComment() {
		println("/**");
		indent();
		indentPrefix = " * ";
	}

	public void endJavaDocComment() {
		out.println("*/");
		outdent();
		indentPrefix = "";
	}

	public void indent() {
		indentLevel++;
	}

	public void indentln(String s) {
		indent();
		println(s);
		outdent();
	}

	public void indentln(String s, Object... args) {
		resolveImports(args);
		indentln(String.format(s, args));
	}

	public void outdent() {
		indentLevel = Math.max(indentLevel - 1, 0);
	}

	public void print(String s) {
		maybeIndent();
		out.print(s);
	}

	public void print(String s, Object... args) {
		resolveImports(args);
		print(String.format(s, args));
	}

	public void println() {
		maybeIndent();
		// Unix-style line endings for consistent behavior across platforms.
		out.print('\n');
		needsIndent = true;
	}

	public void println(String s) {
		print(s);
		println();
	}

	public void println(String s, Object... args) {
		resolveImports(args);
		println(String.format(s, args));
	}

	private void resolveImports(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof ApiType) {
				args[i] = resolveImport((ApiType) args[i]);
			}
		}
	}

	@Override
	public String toString() {
		out.flush();
		return buffer.getBuffer().toString();
	}

	private void maybeIndent() {
		if (needsIndent) {
			needsIndent = false;
			for (int i = 0; i < indentLevel; i++) {
				out.print(indentString);
				out.print(indentPrefix);
			}
		}
	}
}