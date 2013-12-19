package org.vaadin.vrapper;

import org.vaadin.vrapper.model.ClientRpcMethodAction;

import com.vaadin.ui.TextField;

public class ClientRpcMethodActionConfigForm extends
        AbstractMethodActionConfigForm<ClientRpcMethodAction> {

    private final TextField methodName = new TextField("Method name");
    private final TextField rpcInterfaceName = new TextField("Rpc interface");

    public ClientRpcMethodActionConfigForm(ClientRpcMethodAction action) {
        super(ClientRpcMethodAction.class, action);

        addComponents(methodName, rpcInterfaceName);
    }

}
