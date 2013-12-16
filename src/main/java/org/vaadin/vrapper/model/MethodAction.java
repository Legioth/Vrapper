package org.vaadin.vrapper.model;

import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.TypeSource;

public abstract class MethodAction {

	public enum Status {
		IMPOSSIBLE, DISCOURAGED, SUPPORTED, RECOMMENDED;
	}

	protected static class Evaluation {
		private String comment;
		private Status status;

		public Evaluation(Status status, String comment) {
			this.status = status;
			this.comment = comment;
		}
	}

	private ApiMethod method;
	private String name;
	private Status status;
	private String comment;

	public MethodAction(ApiMethod method, String name) {
		this.method = method;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void init() {
		Evaluation evaluation = evaluate();
		status = evaluation.status;
		comment = evaluation.comment;
	}

	public abstract Evaluation evaluate();

	public Status getStatus() {
		return status;
	}

	public String getComment() {
		return comment;
	}

	public ApiMethod getMethod() {
		return method;
	}

	public abstract void writeCode(Code code);

	protected TypeSource getTypeSource() {
		return getMethod().getReturnType().getTypeSource();
	}
}
