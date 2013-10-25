package org.vaadin.vrapper;

import org.vaadin.vrapper.model.StateFieldMethodAction;

import com.vaadin.ui.TextField;

public class StateFieldMethodActionConfigForm extends
        AbstractMethodActionConfigForm<StateFieldMethodAction> {

    private final TextField fieldName = new TextField("Field name");
    private final TextField setterName = new TextField("Setter name");

    public StateFieldMethodActionConfigForm(StateFieldMethodAction action) {
        super(StateFieldMethodAction.class, action);

        addComponents(fieldName, setterName);
    }

}
