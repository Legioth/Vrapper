package org.vaadin.vrapper;

import java.util.Collection;
import java.util.List;

import org.vaadin.vrapper.model.MethodAction;
import org.vaadin.vrapper.model.MethodConfiguration;
import org.vaadin.vrapper.model.WidgetConfiguration;
import org.vaadin.vrapper.model.reflect.ApiType;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

public class WidgetConfigurator extends CustomComponent {
    private static final String SELECTED_ACTION_PROPERTY = "selectedAction";
    private static final String DEFINING_CLASS_PROPERTY = "definingClass";
    private static final String METHOD_PROPERTY = "method";

    private final VerticalLayout layout = new VerticalLayout();
    private final MethodConfigurator methodConfigurator = new MethodConfigurator(
            this);

    private WidgetConfiguration configuration;

    public WidgetConfigurator(WidgetConfiguration configuration) {
        this.configuration = configuration;
        setCompositionRoot(layout);

        Collection<MethodConfiguration> methods = configuration
                .getMethodConfigurations();

        BeanItemContainer<MethodConfiguration> items = new BeanItemContainer<MethodConfiguration>(
                MethodConfiguration.class, methods);

        final Table table = new Table("Methods in "
                + configuration.getWidget().getClassName(), items) {
            @Override
            protected String formatPropertyValue(Object rowId, Object colId,
                    Property<?> property) {
                MethodConfiguration method = (MethodConfiguration) rowId;
                if (METHOD_PROPERTY.equals(colId)) {
                    return method.getMethod().getSourceString();
                } else if (SELECTED_ACTION_PROPERTY.equals(colId)) {
                    MethodAction action = method.getSelectedAction();
                    if (action == null) {
                        return "";
                    } else {
                        return action.getName();
                    }
                }
                return super.formatPropertyValue(rowId, colId, property);
            }
        };

        table.addGeneratedColumn(DEFINING_CLASS_PROPERTY,
                new ColumnGenerator() {
                    @Override
                    public Object generateCell(Table source, Object itemId,
                            Object columnId) {
                        MethodConfiguration configuration = (MethodConfiguration) itemId;
                        ApiType declaringType = configuration.getMethod()
                                .getDeclaringType();
                        return declaringType.getSimpleName();
                    }
                });
        table.setColumnWidth(DEFINING_CLASS_PROPERTY, 100);

        table.addGeneratedColumn(SELECTED_ACTION_PROPERTY,
                new ColumnGenerator() {
                    @Override
                    public Object generateCell(final Table source,
                            final Object itemId, Object columnId) {
                        MethodConfiguration configuration = (MethodConfiguration) itemId;
                        List<MethodAction> actions = configuration.getActions();
                        ActionSelector selector = new ActionSelector();
                        selector.setImmediate(true);
                        selector.setActions(actions);
                        selector.setPropertyDataSource(source.getItem(
                                configuration).getItemProperty(
                                SELECTED_ACTION_PROPERTY));
                        selector.addValueChangeListener(new ValueChangeListener() {
                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                Object newValue = event.getProperty()
                                        .getValue();
                                if (newValue != null) {
                                    table.setValue(itemId);
                                }
                            }
                        });
                        selector.setWidth("100%");
                        return selector;
                    }
                });
        table.setColumnWidth(SELECTED_ACTION_PROPERTY, 200);

        table.setVisibleColumns(METHOD_PROPERTY, DEFINING_CLASS_PROPERTY,
                SELECTED_ACTION_PROPERTY);
        table.setColumnHeaders("Method", "Defined in", "Action");

        table.setImmediate(true);
        table.setSelectable(true);
        table.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                BeanItem<MethodConfiguration> item = (BeanItem<MethodConfiguration>) table
                        .getItem(table.getValue());
                methodConfigurator.setCurrentConfiguration(item);
            }
        });

        Button showCodeButton = new Button("Show all the code",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        try {
                            String source = WidgetConfigurator.this.configuration
                                    .buildFullSource();
                            TextArea textArea = new TextArea();
                            textArea.setValue(source);
                            textArea.setSizeFull();
                            textArea.setWordwrap(false);
                            Window window = new Window("All the code", textArea);
                            window.setHeight("300px");
                            window.setWidth("300px");
                            window.setWindowMode(WindowMode.MAXIMIZED);
                            window.setModal(true);

                            getUI().addWindow(window);
                        } catch (Exception e) {
                            Notification.show("Could not generate code",
                                    e.getLocalizedMessage(),
                                    Notification.Type.ERROR_MESSAGE);
                        }
                    }
                });

        Button restartButton = new Button("Restart",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        getUI().getPage().reload();
                    }
                });
        restartButton.addStyleName(Reindeer.BUTTON_LINK);

        ValueChangeListener classChangeListener = new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                methodConfigurator.updateCode();
            }
        };

        VerticalLayout leftLayout = new VerticalLayout(table,
                new ClassConfigForm("Connector class",
                        configuration.getConnectorCodeConfiguration(),
                        classChangeListener), new ClassConfigForm(
                        "Shared state class",
                        configuration.getStateCodeConfiguration(),
                        classChangeListener), new ClassConfigForm(
                        "Component class",
                        configuration.getComponentCodeConfiguration(),
                        classChangeListener), showCodeButton, restartButton);
        leftLayout.setSpacing(true);

        HorizontalLayout layout = new HorizontalLayout(leftLayout,
                methodConfigurator);
        layout.setSpacing(true);
        layout.setMargin(true);
        setCompositionRoot(layout);
    }

    public WidgetConfiguration getWidgetConfiguration() {
        return configuration;
    }

}
