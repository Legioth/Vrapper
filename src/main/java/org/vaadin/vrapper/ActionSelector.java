package org.vaadin.vrapper;

import java.util.EnumMap;
import java.util.List;

import org.vaadin.vrapper.model.MethodAction;
import org.vaadin.vrapper.model.MethodAction.Status;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.ComboBox;

public class ActionSelector extends ComboBox {

	private final BeanItemContainer<MethodAction> container = new BeanItemContainer<MethodAction>(
			MethodAction.class);

	private static final EnumMap<Status, String> statusIcons = new EnumMap<Status, String>(
			Status.class);
	static {
		// TODO find better icons, e.g. red, yellow and green
		statusIcons.put(Status.DISCOURAGED, "help.png");
		statusIcons.put(Status.SUPPORTED, "ok.png");
		statusIcons.put(Status.RECOMMENDED, "attention.png");
	}

	public ActionSelector() {
		setContainerDataSource(container);
	}

	@Override
	public String getItemCaption(Object itemId) {
		if (itemId instanceof MethodAction) {
			MethodAction action = (MethodAction) itemId;
			return action.getName();
		} else {
			return super.getItemCaption(itemId);
		}
	}

	@Override
	public Resource getItemIcon(Object itemId) {
		if (itemId instanceof MethodAction) {
			MethodAction action = (MethodAction) itemId;
			String iconName = statusIcons.get(action.getStatus());

			if (iconName != null) {
				return new ThemeResource("../runo/icons/16/" + iconName);
			} else {
				return null;
			}

		} else {
			return super.getItemIcon(itemId);
		}
	}

	public void setActions(List<MethodAction> actions) {
		container.removeAllItems();
		container.addAll(actions);
	}
}
