package com.borets.extendedactions.actions;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.ActionType;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.data.meta.ContainerDataUnit;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.CloseAction;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.StandardCloseAction;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.security.entity.RoleType;

import javax.inject.Inject;

@ActionType(OpenInEntityInspector.ID)
public class OpenInEntityInspector extends ItemTrackingAction {
    public static final String ID = "openInEntityInspector";
    private boolean isAdmin;


    public OpenInEntityInspector() {
        this(ID);
    }

    public OpenInEntityInspector(String id) {
        super(id);
        final UserSessionSource uss = AppBeans.get(UserSessionSource.NAME);
        this.isAdmin = uss.getUserSession().getUser().getUserRoles().stream().anyMatch(userRole -> userRole.getRole().getType().equals(RoleType.SUPER));
    }

    @Inject
    protected void setMessages(Messages messages) {
        this.caption = messages.getMainMessage("actions.showInEntityInspector");
    }

    @Override
    public void actionPerform(Component component) {
        super.actionPerform(component);
        final Entity selected = getTarget().getSingleSelected();
        if (selected == null)
            return;

        final FrameOwner frameOwner = getTarget().getFrame().getFrameOwner();
        if (frameOwner instanceof StandardLookup) {
            final StandardLookup standardLookup = (StandardLookup) frameOwner;
            final AbstractWindow window = standardLookup.getWindow().openWindow("entityInspector.edit", WindowManager.OpenType.NEW_TAB,
                    ParamsMap.of("item", selected));

            if (getTarget() instanceof com.haulmont.cuba.gui.components.Component.Focusable) {
                window.addAfterCloseListener(event -> {
                    // move focus to owner
                    ((Component.Focusable) getTarget()).focus();
                });
            }
            if (getTarget().getItems() instanceof ContainerDataUnit) {
                window.addAfterCloseListener(afterCloseEvent -> {
                    if (isCommitCloseAction(afterCloseEvent.getCloseAction())) {
                        //refresh item changes. look at EditorBuilderProcessor.java as example
                        //fixme: is`t enough.
                        final CollectionContainer container = ((ContainerDataUnit) getTarget().getItems()).getContainer();
                        container.replaceItem(selected);
                    }
                });
            }

        }
    }

    @Override
    public boolean isVisible() {
        return this.isAdmin;
    }

    protected boolean isCommitCloseAction(CloseAction closeAction) {
        return (closeAction instanceof StandardCloseAction)
                && ((StandardCloseAction) closeAction).getActionId().equals(Window.COMMIT_ACTION_ID);
    }
}