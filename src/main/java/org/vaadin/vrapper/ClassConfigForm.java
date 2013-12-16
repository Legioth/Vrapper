package org.vaadin.vrapper;

import org.vaadin.vrapper.model.codegen.CodeConfiguration;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

public class ClassConfigForm extends CustomComponent {
	private TextField className = new TextField();

	private TextField packageName = new TextField();

	public ClassConfigForm(String caption, CodeConfiguration codeConfiguration,
			ValueChangeListener anyChangeListener) {
		setCaption(caption);

		BeanFieldGroup<CodeConfiguration> fieldGroup = new BeanFieldGroup<CodeConfiguration>(
				CodeConfiguration.class);
		fieldGroup.setBuffered(false);
		fieldGroup.setItemDataSource(codeConfiguration);
		fieldGroup.bindMemberFields(this);

		for (Field<?> field : fieldGroup.getFields()) {
			field.addValueChangeListener(anyChangeListener);
			if (field instanceof AbstractComponent) {
				((AbstractComponent) field).setImmediate(true);
			}
		}

		packageName.setColumns(20);

		Label dotLabel = new Label(".");
		HorizontalLayout layout = new HorizontalLayout();
		layout.setDefaultComponentAlignment(Alignment.BOTTOM_CENTER);
		layout.addComponents(packageName, dotLabel, className);
		setCompositionRoot(layout);
	}

}
