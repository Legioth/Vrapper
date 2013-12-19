package org.vaadin.vrapper;

import org.vaadin.vrapper.model.MethodAction;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;

public class AbstractMethodActionConfigForm<T extends MethodAction> extends
        FormLayout {
    private BeanFieldGroup<T> fieldGroup;

    public AbstractMethodActionConfigForm(Class<T> type, T bean) {
        setMargin(true);

        fieldGroup = new BeanFieldGroup<T>(type);
        fieldGroup.setItemDataSource(bean);
        fieldGroup.setBuffered(false);
    }

    protected BeanFieldGroup<T> getFieldGroup() {
        return fieldGroup;
    }

    public void bind(ValueChangeListener changeListener) {
        fieldGroup.bindMemberFields(this);

        for (Field<?> field : fieldGroup.getFields()) {
            if (field instanceof AbstractComponent) {
                ((AbstractComponent) field).setImmediate(true);
            }
            field.addValueChangeListener(changeListener);
        }
    }

}