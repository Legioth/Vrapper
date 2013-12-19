package org.vaadin.vrapper.model;

import java.util.List;

import org.vaadin.vrapper.model.codegen.Code;
import org.vaadin.vrapper.model.codegen.MethodCode;
import org.vaadin.vrapper.model.codegen.SnippetGenerator;
import org.vaadin.vrapper.model.codegen.SourceWriter;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.shared.communication.ClientRpc;

public class ClientRpcMethodAction extends MethodAction {

	private String methodName;
	private String rpcInterfaceName;

	public ClientRpcMethodAction(ApiMethod method,
			WidgetConfiguration widgetConfiguration) {
		super(method, "Call using RPC");
		methodName = method.getName();

		rpcInterfaceName = widgetConfiguration.getComponentCodeConfiguration()
				.getClassName() + "ClientRpc";
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getRpcInterfaceName() {
		return rpcInterfaceName;
	}

	public void setRpcInterfaceName(String rpcInterfaceName) {
		this.rpcInterfaceName = rpcInterfaceName;
	}

	@Override
	public Evaluation evaluate() {
		ApiMethod method = getMethod();

		if (!method.allParametersHasSerializationSupport()) {
			// TODO provide more information
			return new Evaluation(Status.IMPOSSIBLE,
					"Not all parameters are serializable");
		} else if (method.getReturnType().getClassName().equals("void")) {
			return new Evaluation(Status.SUPPORTED, "");
		} else if (isGetter()) {
			return new Evaluation(Status.DISCOURAGED,
					"Seems like a getter method");
		} else {
			return new Evaluation(Status.DISCOURAGED,
					"Returned value will be ignored");
		}
	}

	private boolean isGetter() {
		ApiMethod method = getMethod();

		String name = method.getName();
		return (name.startsWith("get") || name.startsWith("is"))
				&& method.getParameterTypes().size() == 0;
	}

	@Override
	public void writeCode(Code code) {
		ApiType rpcSuperIntrface = getMethod().getReturnType().getTypeSource()
				.getType(ClientRpc.class);
		final String rpcMethodName = getMethod().getName();
		final List<ApiType> rpcMethodParameters = getMethod()
				.getParameterTypes();

		final ClassType rpcType = code.addClientRpcMethod(
				getRpcInterfaceName(), rpcSuperIntrface, rpcMethodName,
				rpcMethodParameters, null, new SnippetGenerator() {
					@Override
					public void writeSnippet(SourceWriter w) {
						w.print("getWidget().%s(", rpcMethodName);
						for (int i = 0; i < rpcMethodParameters.size(); i++) {
							if (i != 0) {
								w.print(", ");
							}
							w.print("p%d", Integer.valueOf(i));
						}
						w.println(");");
					}
				});

		MethodCode componentMethod = code.getComponentCode().addMethod(
				getMethod().getReturnType(), getMethodName(),
				rpcMethodParameters);
		componentMethod.addImplementationSnippet(new SnippetGenerator() {
			@Override
			public void writeSnippet(SourceWriter w) {
				w.print("getRpcProxy(%s.class).%s(", rpcType, rpcMethodName);

				List<ApiType> parameterTypes = rpcMethodParameters;
				for (int i = 0; i < parameterTypes.size(); i++) {
					if (i != 0) {
						w.print(", ");
					}
					w.print("p%d", Integer.valueOf(i));
				}

				w.println(");");
			}
		});
	}
}
