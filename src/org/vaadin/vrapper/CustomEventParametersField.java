package org.vaadin.vrapper;

import org.vaadin.vrapper.model.CustomMethodParameter;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.VerticalLayout;

public class CustomEventParametersField extends
		CustomField<CustomMethodParameter> {

	final VerticalLayout layout = new VerticalLayout();

	public CustomEventParametersField() {

	}

	@Override
	protected Component initContent() {
		return layout;
	}

	@Override
	protected void setInternalValue(CustomMethodParameter newValue) {
		super.setInternalValue(newValue);
	}

	@Override
	public Class<CustomMethodParameter> getType() {
		return CustomMethodParameter.class;
	}

}
