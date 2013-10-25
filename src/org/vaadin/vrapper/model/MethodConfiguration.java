package org.vaadin.vrapper.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.vaadin.vrapper.model.MethodAction.Status;
import org.vaadin.vrapper.model.reflect.ApiMethod;

public class MethodConfiguration {
    private static final Comparator<MethodAction> actionComparator = new Comparator<MethodAction>() {
        @Override
        public int compare(MethodAction o1, MethodAction o2) {
            return o2.getStatus().compareTo(o1.getStatus());
        }
    };

    private final WidgetConfiguration widgetConf;
    private final ApiMethod method;
    private final List<MethodAction> actions = new ArrayList<MethodAction>();
    private final List<MethodAction> impossibleActions = new ArrayList<MethodAction>();

    private MethodAction selectedAction;

    public MethodConfiguration(WidgetConfiguration widgetConf, ApiMethod method) {
        this.widgetConf = widgetConf;
        this.method = method;

        for (MethodAction methodAction : Arrays
                .asList(new StateFieldMethodAction(method),
                        new ClientRpcMethodAction(method, widgetConf),
                        new EventHandlerMethodAction(method, widgetConf),
                        new ResouceUrlMethodAction(method))) {
            methodAction.init();
            if (methodAction.getStatus() != Status.IMPOSSIBLE) {
                actions.add(methodAction);
            } else {
                impossibleActions.add(methodAction);
            }
        }
        Collections.sort(actions, actionComparator);

        if (!actions.isEmpty()
                && actions.get(0).getStatus() == Status.RECOMMENDED) {
            selectedAction = actions.get(0);
        }
    }

    public List<MethodAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<MethodAction> getImpossibleActions() {
        return Collections.unmodifiableList(impossibleActions);
    }

    public MethodAction getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(MethodAction action) {
        if (action != null && !action.getMethod().equals(method)) {
            throw new IllegalArgumentException("Can't assign action for "
                    + action.getMethod() + " assign to the method " + method);
        }
        this.selectedAction = action;
    }

    public ApiMethod getMethod() {
        return method;
    }
}
