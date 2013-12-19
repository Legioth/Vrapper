package org.vaadin.vrapper;

import org.vaadin.vrapper.model.ResouceUrlMethodAction;

import com.vaadin.ui.TextField;

public class ResourceUrlMethodActionConfigForm extends
        AbstractMethodActionConfigForm<ResouceUrlMethodAction> {

    private final TextField resourceKey = new TextField("Resource key");
    private final TextField setterName = new TextField("Setter name");

    public ResourceUrlMethodActionConfigForm(ResouceUrlMethodAction action) {
        super(ResouceUrlMethodAction.class, action);

        addComponents(resourceKey, setterName);
    }

}
