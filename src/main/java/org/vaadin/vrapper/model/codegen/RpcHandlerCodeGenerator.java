package org.vaadin.vrapper.model.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;

public abstract class RpcHandlerCodeGenerator extends AbstractCodeGenerator {

	private static class RpcInit {
		MethodCode methodCode;
		SnippetGenerator snippetGenerator;

		public RpcInit(MethodCode methodCode, SnippetGenerator snippetGenerator) {
			this.methodCode = methodCode;
			this.snippetGenerator = snippetGenerator;
		}
	}

	private Map<ClassType, List<RpcInit>> rpcInits = new HashMap<ClassType, List<RpcInit>>();

	public RpcHandlerCodeGenerator(CodeConfiguration configuration) {
		super(configuration);
	}

	protected void addRpcInit(ClassType rpcInterface,
			MethodCode methodDeclaration, SnippetGenerator handlerSnippet) {
		if (rpcInits.isEmpty()) {
			ensureRpcInit();
		}
		List<RpcInit> interfaceInits = rpcInits.get(rpcInterface);
		if (interfaceInits == null) {
			interfaceInits = new ArrayList<RpcInit>();
			rpcInits.put(rpcInterface, interfaceInits);
		}

		interfaceInits.add(new RpcInit(methodDeclaration, handlerSnippet));
	}

	protected abstract void ensureRpcInit();

	protected void writeRpcInit(SourceWriter w) {
		for (Entry<ClassType, List<RpcInit>> entry : rpcInits.entrySet()) {
			ClassType rpcInterface = entry.getKey();
			List<RpcInit> methods = entry.getValue();

			w.println("registerRpc(%s.class, new %s() {", rpcInterface,
					rpcInterface);
			w.indent();

			for (RpcInit initMethod : methods) {
				if (!w.isPreview()) {
					w.println("@Override");
				}
				w.print("public %s %s(", initMethod.methodCode.getReturnType(),
						initMethod.methodCode.getName());
				List<ApiType> parameterTypes = initMethod.methodCode
						.getParameterTypes();
				List<String> parameterNames = initMethod.methodCode
						.getParameterNames();
				for (int i = 0; i < parameterTypes.size(); i++) {
					if (i != 0) {
						w.print(", ");
					}
					w.print("%s %s", parameterTypes.get(i),
							parameterNames.get(i));
				}
				w.println(") {");
				w.indent();

				initMethod.snippetGenerator.writeSnippet(w);

				w.outdent();
				w.println("}");
			}

			w.outdent();
			w.println("});");
		}
	}

}