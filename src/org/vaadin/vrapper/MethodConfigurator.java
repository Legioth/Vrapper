package org.vaadin.vrapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.vaadin.vrapper.model.ClientRpcMethodAction;
import org.vaadin.vrapper.model.EventHandlerMethodAction;
import org.vaadin.vrapper.model.MethodAction;
import org.vaadin.vrapper.model.MethodAction.Status;
import org.vaadin.vrapper.model.MethodConfiguration;
import org.vaadin.vrapper.model.ResouceUrlMethodAction;
import org.vaadin.vrapper.model.StateFieldMethodAction;
import org.vaadin.vrapper.model.codegen.AbstractCodeGenerator;
import org.vaadin.vrapper.model.codegen.Code;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class MethodConfigurator extends CustomComponent {

    private final VerticalLayout layout = new VerticalLayout();
    private final Label selectItemLabel = new Label(
            "Please select a method to configure");
    private final CheckBox previewBox = new CheckBox("Only show relevant code",
            true);
    private final TextArea codeViewer = new TextArea("Code for this method");

    // TODO desperately needs some styling
    private final Label commentLabel = new Label();

    // TODO consider using PopupView instead
    private final Button impossibleActionsButton = new Button(
            "Show impossible actions", new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    if (methodConfiguration == null) {
                        Notification.show("No method selected");
                        return;
                    }

                    List<MethodAction> impossibleActions = methodConfiguration
                            .getImpossibleActions();

                    FormLayout formLayout = new FormLayout();
                    formLayout.setMargin(true);
                    for (MethodAction methodAction : impossibleActions) {
                        Label label = new Label(methodAction.getComment());
                        label.setCaption(methodAction.getName());
                        formLayout.addComponent(label);
                    }

                    Window window = new Window("Impossible actions", formLayout);
                    window.setModal(true);
                    UI.getCurrent().addWindow(window);
                }
            });

    private ActionSelector actionSelector = new ActionSelector();

    private Panel customizationPanel = new Panel("Customize");

    private WidgetConfigurator widgetConfigurator;

    private final ValueChangeListener codeUpdater = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
            updateCode();
        }
    };

    private MethodConfiguration methodConfiguration;

    public MethodConfigurator(WidgetConfigurator widgetConfigurator) {
        this.widgetConfigurator = widgetConfigurator;

        setCurrentConfiguration(null);

        setCompositionRoot(layout);

        ValueChangeListener configFormChangeListener = new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                updateCommentLabel();
                updateConfigForm();
                updateCode();
            }
        };

        layout.setSpacing(true);

        actionSelector.setImmediate(true);
        actionSelector.addValueChangeListener(configFormChangeListener);

        previewBox.setImmediate(true);
        previewBox.addValueChangeListener(codeUpdater);

        codeViewer.setWidth("500px");
        codeViewer.setHeight("500px");
        codeViewer.setWordwrap(false);

        customizationPanel.setVisible(false);
    }

    private void updateCommentLabel() {
        String value = null;
        MethodAction action = getCurrentAction();

        if (action != null) {
            value = action.getComment();
        }

        boolean visible = value != null && !"".equals(value);
        if (visible && action != null) {
            Status status = action.getStatus();
            if (status == Status.DISCOURAGED) {
                value = "Discouraged: " + value;
            }
        }

        commentLabel.setVisible(visible);
        commentLabel.setValue(value);
    }

    protected void updateConfigForm() {
        MethodAction action = getCurrentAction();
        Component configForm;
        if (action != null) {
            configForm = createConfigForm(action);
        } else {
            configForm = null;
        }
        customizationPanel.setVisible(configForm != null);
        customizationPanel.setContent(configForm);
    }

    private Component createConfigForm(MethodAction action) {
        AbstractMethodActionConfigForm<?> form;
        if (action instanceof ClientRpcMethodAction) {
            form = new ClientRpcMethodActionConfigForm(
                    (ClientRpcMethodAction) action);
        } else if (action instanceof StateFieldMethodAction) {
            form = new StateFieldMethodActionConfigForm(
                    (StateFieldMethodAction) action);
        } else if (action instanceof ResouceUrlMethodAction) {
            form = new ResourceUrlMethodActionConfigForm(
                    (ResouceUrlMethodAction) action);
        } else if (action instanceof EventHandlerMethodAction) {
            form = new EventHandlerMethodActionConfigForm(
                    (EventHandlerMethodAction) action, widgetConfigurator
                            .getWidgetConfiguration().getWidget());
        } else {
            form = null;
        }

        if (form != null) {
            form.bind(codeUpdater);
        }
        return form;
    }

    public void setCurrentConfiguration(BeanItem<MethodConfiguration> item) {
        if (item == null) {
            methodConfiguration = null;
            actionSelector.setActions(Collections.<MethodAction> emptyList());
            layout.removeAllComponents();
            layout.addComponent(selectItemLabel);
        } else {
            if (!actionSelector.isAttached()) {
                layout.removeAllComponents();
                layout.addComponent(actionSelector);
                layout.addComponent(impossibleActionsButton);
                layout.addComponent(commentLabel);
                layout.addComponent(customizationPanel);
                layout.addComponent(previewBox);
                layout.addComponent(codeViewer);
            }

            methodConfiguration = item.getBean();

            impossibleActionsButton.setEnabled(!methodConfiguration
                    .getImpossibleActions().isEmpty());

            actionSelector.setCaption("Action for "
                    + methodConfiguration.getMethod().getSourceString());
            actionSelector.setPropertyDataSource(null);

            List<MethodAction> actions = methodConfiguration.getActions();
            actionSelector.setActions(actions);

            actionSelector.setPropertyDataSource(item
                    .getItemProperty("selectedAction"));

            updateCode();
        }
    }

    public void updateCode() {
        Code code = new Code(widgetConfigurator.getWidgetConfiguration());
        MethodAction action = getCurrentAction();
        if (action != null) {
            action.writeCode(code);
        } else {
            code.getConnectorCode();
        }

        StringBuilder b = new StringBuilder();

        List<AbstractCodeGenerator> classes = code.getClasses();
        Collections.sort(classes, new Comparator<AbstractCodeGenerator>() {
            @Override
            public int compare(AbstractCodeGenerator o1,
                    AbstractCodeGenerator o2) {
                return o2.getType().getClassName()
                        .compareTo(o1.getType().getClassName());
            }
        });

        boolean first = true;
        for (AbstractCodeGenerator generator : classes) {
            String generated = generator.generateCode(previewBox.getValue()
                    .booleanValue());
            if (generated != null) {
                if (!first) {
                    b.append("\n\n");
                }

                b.append(generated);
                first = false;
            }
        }

        codeViewer.setValue(b.toString());
    }

    private MethodAction getCurrentAction() {
        MethodAction action = (MethodAction) actionSelector.getValue();
        return action;
    }

}
