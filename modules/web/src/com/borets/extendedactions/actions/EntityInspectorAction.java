package com.borets.extendedactions.actions;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.core.entityinspector.EntityInspectorEditor;
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
import com.haulmont.cuba.security.entity.Access;
import com.haulmont.cuba.security.entity.RoleType;
import com.haulmont.cuba.security.entity.UserRole;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.List;

@ActionType(EntityInspectorAction.ID)
public class EntityInspectorAction extends ItemTrackingAction {
    public static final String ID = "openInEntityInspector";
    private final boolean isAdmin;


    public EntityInspectorAction() {
        this(ID);
    }

    public EntityInspectorAction(String id) {
        super(id);
        final UserSessionSource uss = AppBeans.get(UserSessionSource.NAME);
        final UserSession userSession = uss.getUserSession();
        isAdmin = isSuperUser(userSession);
    }

    @Inject
    protected void setMessages(Messages messages) {
        this.caption = messages.getMainMessage("entityInspectorActions.showInEntityInspector");
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
                        @SuppressWarnings("unchecked")
                        final CollectionContainer<Entity> container = ((ContainerDataUnit) getTarget().getItems()).getContainer();
                        container.replaceItem(((EntityInspectorEditor) window).getItem());
                    }
                });
            }
        }
    }

    private boolean isSuperUser(UserSession userSession) {
        final Access policy = userSession.getPermissionUndefinedAccessPolicy();
        final List<UserRole> userRoles = userSession.getUser().getUserRoles();
        if (Access.ALLOW.equals(policy)) {
            return userRoles.stream()
                    .map(userRole -> userRole.getRole().getType())
                    .anyMatch(RoleType.SUPER::equals);
        } else {
            return userRoles.stream()
                    .map(UserRole::getRoleName)
                    .anyMatch("system-full-access"::equals);
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