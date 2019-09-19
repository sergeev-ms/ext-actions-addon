package com.borets.extendedactions.actions;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.UrlRouting;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.inputdialog.InputDialogAction;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.ScreenContext;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.web.gui.components.WebClipboardTrigger;
import com.haulmont.cuba.web.gui.components.WebTextField;

import javax.inject.Inject;

@ActionType(CopyUrlAction.ID)
public class CopyUrlAction extends ItemTrackingAction {
    public static final String ID = "copyUrlAction";

    @Inject
    private Messages messages;

    public CopyUrlAction() {
        this(ID);
    }

    public CopyUrlAction(String id) {
        super(id);
    }

    @Inject
    protected void setMessages(Messages messages) {
        this.caption = messages.getMainMessage("copyUrlAction.copyUrlAction");
    }

    @Override
    public void actionPerform(Component component) {
        super.actionPerform(component);
        final Entity selected = getTarget().getSingleSelected();
        if (selected == null)
            return;

        final FrameOwner frameOwner = getTarget().getFrame().getFrameOwner();
        if (frameOwner instanceof StandardLookup) {

            final ScreenContext screenContext = ComponentsHelper.getScreenContext(getTarget());
            final Dialogs dialogs = screenContext.getDialogs();

            final UrlRouting urlRouting = ComponentsHelper.getScreenContext(getTarget()).getUrlRouting();
            final String url = urlRouting.getRouteGenerator().getEditorRoute(selected);

            final InputDialog dialog = dialogs.createInputDialog(frameOwner)
                    .withCaption(messages.getMainMessage("copyUrlAction.dialogCaption"))
                    .withParameter(InputParameter.stringParameter("urlParameter")
                            .withField(() -> {
                                final TextField<String> textField = ((UiComponents) AppBeans.get(UiComponents.NAME)).create(WebTextField.TYPE_STRING);
                                textField.setCaption(messages.getMainMessage("copyUrlAction.dialogFieldCaption"));
                                textField.setValue(url);
                                textField.setEditable(false);
                                textField.setWidthFull();
                                return textField;
                            }))
                    .withActions(InputDialogAction
                            .action("copyAction")
                            .withCaption(messages.getMainMessage("copyUrlAction.dialogActionCaption"))
                            .withHandler(inputDialogActionPerformed -> inputDialogActionPerformed.getInputDialog().close(FrameOwner.WINDOW_CLOSE_ACTION)))
                    .show();

            final WebClipboardTrigger clipboardTrigger = new WebClipboardTrigger();
            clipboardTrigger.setButton(((Button) dialog.getDialogWindow().getComponents().stream()
                    .filter(comp -> comp instanceof Button)
                    .findFirst().orElse(null)));

            final TextInputField textInputField = (TextInputField) dialog.getDialogWindow().getComponent("urlParameter");
            clipboardTrigger.setInput(textInputField);

            dialog.getDialogWindow().addFacet(clipboardTrigger);


            if (getTarget() instanceof Component.Focusable) {
                dialog.addAfterCloseListener(event -> {
                    // move focus to owner
                    ((Component.Focusable) getTarget()).focus();
                });
            }
        }
    }
}