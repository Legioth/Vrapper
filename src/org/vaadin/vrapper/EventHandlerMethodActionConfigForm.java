package org.vaadin.vrapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.vrapper.model.EventHandlerMethodAction;
import org.vaadin.vrapper.model.reflect.ApiMethod;
import org.vaadin.vrapper.model.reflect.ApiType;
import org.vaadin.vrapper.model.reflect.ClassType;

import com.vaadin.ui.TextField;

public class EventHandlerMethodActionConfigForm extends
        AbstractMethodActionConfigForm<EventHandlerMethodAction> {

    private final MethodParamListField customParameters;

    private final TextField rpcInterfaceName = new TextField(
            "Rpc interface name");

    public EventHandlerMethodActionConfigForm(EventHandlerMethodAction action,
            ClassType widgetType) {
        super(EventHandlerMethodAction.class, action);

        customParameters = new MethodParamListField(widgetType);
        customParameters.setCaption("Custom parameters");

        ClassType eventType = action.getEventType();
        if (eventType != null) {
            Collection<ApiMethod> methods = eventType.getMethods();
            ClassType nativeEventType = eventType.getTypeSource()
                    .getObjectType("com.google.gwt.dom.client.NativeEvent");

            for (ApiMethod apiMethod : methods) {
                if (apiMethod.getReturnType().equals(nativeEventType)
                        && apiMethod.getParameterTypes().isEmpty()) {
                    customParameters.addMouseEventDetailsOption("event."
                            + apiMethod.getName() + "()");
                    break;
                }
            }
            if (eventType != null) {
                customParameters.addEventTypeEditor(eventType, "Event method",
                        "event");
            }
        } else {
            ApiMethod handlerMethod = action.getHandlerMethod();
            List<ApiType> parameterTypes = handlerMethod.getParameterTypes();
            Map<String, ApiType> sendableParameters = new LinkedHashMap<String, ApiType>();
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (parameterTypes.get(i).hasSerializationSupport()) {
                    sendableParameters.put("p" + i, parameterTypes.get(i));
                }
            }
            if (!sendableParameters.isEmpty()) {
                customParameters
                        .addSendableParametersOption(sendableParameters);
            }
        }

        addComponents(customParameters, rpcInterfaceName);
    }

}
