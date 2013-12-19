package org.vaadin.vrapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.vrapper.model.CustomMethodParameter;
import org.vaadin.vrapper.model.QualifiedMethodParameter;
import org.vaadin.vrapper.model.codegen.Snippet;
import org.vaadin.vrapper.model.codegen.SourceWriter;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.data.Property;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

public class MethodParamListField extends
        CustomField<List<CustomMethodParameter>> {

    private abstract class TargetTypeMethodEditor extends
            CustomField<CustomMethodParameter> {
        private final ClassType widgetType;

        private TargetTypeMethodEditor(ClassType targetType) {
            this.widgetType = targetType;
        }

        @Override
        protected Component initContent() {
            final NativeSelect select = new NativeSelect("Method") {
                @Override
                public String getItemCaption(Object itemId) {
                    if (itemId instanceof ApiMethod) {
                        ApiMethod method = (ApiMethod) itemId;
                        return method.getReturnType().getSimpleName() + " "
                                + method.getName() + "()";
                    } else {
                        return super.getItemCaption(itemId);
                    }
                }
            };

            Collection<ApiMethod> methods = widgetType.getMethods();
            for (ApiMethod method : methods) {
                if (!method.getParameterTypes().isEmpty()) {
                    continue;
                }

                ApiType returnType = method.getReturnType();
                if (!returnType.getClassName().equals("void")
                        && returnType.hasSerializationSupport()) {
                    select.addItem(method);
                }
            }

            select.addValueChangeListener(new ValueChangeListener() {
                @Override
                public void valueChange(Property.ValueChangeEvent event) {
                    ApiMethod method = (ApiMethod) select.getValue();
                    if (method == null) {
                        setInternalValue(null);
                    } else {
                        setInternalValue(createParameter(method));
                    }
                }

            });

            return select;
        }

        protected abstract CustomMethodParameter createParameter(
                ApiMethod method);

        @Override
        public Class<? extends CustomMethodParameter> getType() {
            return CustomMethodParameter.class;
        }
    }

    private interface EditorFactory {
        public Field<? extends CustomMethodParameter> createEditor();
    }

    private Map<String, EditorFactory> editors = new LinkedHashMap<String, EditorFactory>();

    final Button addItemButton = new Button("Add", new Button.ClickListener() {
        @Override
        public void buttonClick(ClickEvent event) {
            showAddDialog();
        }
    });

    final VerticalLayout layout = new VerticalLayout();

    private ClassType widgetType;

    public MethodParamListField(final ClassType widgetType) {
        this.widgetType = widgetType;
        addItemButton.setStyleName(Reindeer.BUTTON_SMALL);

        addEventTypeEditor(widgetType, "Widget method", "getWidget()");
    }

    public void addEventTypeEditor(final ClassType targetType, String name,
            final String qualifier) {
        editors.put(name, new EditorFactory() {
            @Override
            public Field<? extends CustomMethodParameter> createEditor() {
                return new TargetTypeMethodEditor(targetType) {
                    @Override
                    protected CustomMethodParameter createParameter(
                            ApiMethod method) {
                        return new QualifiedMethodParameter(qualifier, method);
                    }
                };
            }
        });
    }

    public void addSendableParametersOption(
            final Map<String, ApiType> sendableParameters) {
        editors.put("Handler parameter", new EditorFactory() {
            @Override
            public Field<? extends CustomMethodParameter> createEditor() {
                return new CustomField<CustomMethodParameter>() {

                    final NativeSelect parameterSelector = new NativeSelect(
                            "Parameter", sendableParameters.keySet()) {
                        @Override
                        public String getItemCaption(Object itemId) {
                            ApiType type = sendableParameters.get(itemId);
                            if (type != null) {
                                return type.getSimpleName() + " " + itemId;
                            } else {
                                return super.getItemCaption(itemId);
                            }
                        }
                    };

                    final TextField nameField = new TextField(
                            "RPC parameter name");

                    @Override
                    protected Component initContent() {

                        parameterSelector
                                .addValueChangeListener(new ValueChangeListener() {
                                    @Override
                                    public void valueChange(
                                            Property.ValueChangeEvent event) {
                                        String parameter = (String) parameterSelector
                                                .getValue();
                                        ApiType type = sendableParameters
                                                .get(parameter);
                                        if (type == null) {
                                            return;
                                        }

                                        String currentName = nameField
                                                .getValue();
                                        if (currentName == null
                                                || currentName.isEmpty()
                                                && type instanceof ClassType) {
                                            nameField.setValue(type
                                                    .getSimpleName()
                                                    .toLowerCase());
                                        }
                                        updateValue();
                                    }
                                });
                        parameterSelector.setImmediate(true);

                        nameField
                                .addValueChangeListener(new ValueChangeListener() {
                                    @Override
                                    public void valueChange(
                                            Property.ValueChangeEvent event) {
                                        updateValue();
                                    }
                                });

                        return new FormLayout(parameterSelector, nameField);
                    }

                    public void updateValue() {
                        String parameter = (String) parameterSelector
                                .getValue();
                        ApiType type = sendableParameters.get(parameter);
                        if (type == null) {
                            setInternalValue(null);
                        } else {
                            String parameterName = nameField.getValue();
                            if (parameterName == null
                                    || parameterName.isEmpty()) {
                                parameterName = parameter;
                            }

                            setInternalValue(new CustomMethodParameter(type,
                                    parameterName, new Snippet(parameter),
                                    "Parameter " + parameterName));
                        }
                    }

                    @Override
                    public Class<? extends CustomMethodParameter> getType() {
                        return CustomMethodParameter.class;
                    }
                };
            }
        });
    }

    public void addMouseEventDetailsOption(final String nativeEventAccessor) {
        editors.put("MouseEventDetails", new EditorFactory() {
            @Override
            public Field<? extends CustomMethodParameter> createEditor() {
                return new CustomField<CustomMethodParameter>() {
                    @Override
                    protected Component initContent() {
                        final CheckBox checkBox = new CheckBox(
                                "Use relative element", true);
                        checkBox.addValueChangeListener(new ValueChangeListener() {
                            @Override
                            public void valueChange(
                                    Property.ValueChangeEvent event) {
                                updateValue(checkBox.getValue().booleanValue());
                            }
                        });
                        updateValue(true);

                        return checkBox;
                    }

                    private void updateValue(boolean useRelative) {
                        ClassType mouseDetailsType = widgetType.getTypeSource()
                                .getObjectType(
                                        "com.vaadin.shared.MouseEventDetails");
                        ClassType mouseDetailsBuilderType = widgetType
                                .getTypeSource()
                                .getObjectType(
                                        "com.vaadin.client.MouseEventDetailsBuilder");
                        String template;
                        if (useRelative) {
                            template = "%s.buildMouseEventDetails(%s, getWidget().getElement())";
                        } else {
                            template = "%s.buildMouseEventDetails(%s)";
                        }

                        Snippet snippet;
                        snippet = new Snippet(template,
                                mouseDetailsBuilderType, nativeEventAccessor) {
                            @Override
                            public void writeSnippet(SourceWriter w) {
                                super.writeSnippet(w);
                            }
                        };

                        setInternalValue(new CustomMethodParameter(
                                mouseDetailsType, "details", snippet,
                                "MouseEventDetails"));
                    }

                    @Override
                    public Class<? extends CustomMethodParameter> getType() {
                        return CustomMethodParameter.class;
                    }
                };
            }
        });
    }

    private void showAddDialog() {
        final VerticalLayout addLayout = new VerticalLayout();
        addLayout.setMargin(true);
        addLayout.setSpacing(true);
        final Window addDialog = new Window("Add parameter", addLayout);

        if (editors.size() > 1) {
            final OptionGroup typeSelector = new OptionGroup(null,
                    editors.keySet());
            typeSelector.setImmediate(true);
            typeSelector.addValueChangeListener(new ValueChangeListener() {

                @Override
                public void valueChange(Property.ValueChangeEvent event) {
                    Object selectedType = typeSelector.getValue();
                    EditorFactory editorFactory = editors.get(selectedType);

                    selectEditFactory(addLayout, addDialog, editorFactory);
                }
            });

            addLayout.addComponent(typeSelector);
        } else {
            selectEditFactory(addLayout, addDialog, editors.values().iterator()
                    .next());
        }

        addDialog.setModal(true);
        UI.getCurrent().addWindow(addDialog);
    }

    private void selectEditFactory(final VerticalLayout addLayout,
            final Window addDialog, EditorFactory editorFactory) {
        while (addLayout.getComponentCount() > 1) {
            addLayout.removeComponent(addLayout.getComponent(addLayout
                    .getComponentCount() - 1));
        }

        if (editorFactory != null) {
            final Field<? extends CustomMethodParameter> editor = editorFactory
                    .createEditor();
            addLayout.addComponent(editor);

            addLayout.addComponent(new Button("Add",
                    new Button.ClickListener() {
                        @Override
                        public void buttonClick(ClickEvent event) {
                            CustomMethodParameter value = editor.getValue();
                            if (value != null) {
                                getInternalValue().add(value);
                                fireValueChange(true);
                            }
                            updateUi();
                            addDialog.getUI().removeWindow(addDialog);
                        }
                    }));
        }
    }

    @Override
    protected Component initContent() {
        return layout;
    }

    @Override
    protected void setInternalValue(final List<CustomMethodParameter> newValue) {
        super.setInternalValue(newValue);
        updateUi();
    }

    private void updateUi() {
        final List<CustomMethodParameter> value = getInternalValue();
        layout.removeAllComponents();

        if (value != null) {
            for (final CustomMethodParameter customMethodParameter : value) {
                final HorizontalLayout row = new HorizontalLayout();
                row.setSpacing(true);
                row.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
                Button removeButton = new Button("Remove",
                        new Button.ClickListener() {
                            @Override
                            public void buttonClick(ClickEvent event) {
                                value.remove(customMethodParameter);
                                fireValueChange(true);
                                layout.removeComponent(row);
                            }
                        });
                removeButton.setStyleName(Reindeer.BUTTON_LINK);
                row.addComponents(
                        new Label(customMethodParameter.getDescription()),
                        removeButton);

                layout.addComponent(row);
            }
            layout.addComponent(addItemButton);
        }
    }

    @Override
    public Class<? extends List<CustomMethodParameter>> getType() {
        return (Class) List.class;
    }

}
